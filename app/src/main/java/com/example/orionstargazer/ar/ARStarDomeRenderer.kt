package com.example.orionstargazer.ar

import com.example.orionstargazer.R
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.Texture
import java.util.ArrayDeque
import kotlin.math.abs

class ARStarDomeRenderer(
    private val sceneView: ArSceneView,
    private val onStarTapped: (com.example.orionstargazer.data.entities.StarEntity) -> Unit
) {
    private data class StarNodeState(
        val node: Node,
        var starId: Int = 0,
        var targetScale: Float = 0f,
        var currentScale: Float = 0f,
        var isDying: Boolean = false
    )

    // Active nodes keyed by star id. Pool to avoid allocations when stars enter/leave view.
    private val active = mutableMapOf<Int, StarNodeState>()
    private val pool = ArrayDeque<StarNodeState>(MAX_POOL)

    // Latest star data for tap callbacks (id -> entity).
    private val visibleById = mutableMapOf<Int, com.example.orionstargazer.data.entities.StarEntity>()

    // Shared billboard sprite renderables keyed by spectral class.
    private val renderablesBySpectralClass = mutableMapOf<Char, ModelRenderable>()
    private val solidRenderablesBySpectralClass = mutableMapOf<Char, ModelRenderable>()
    private var spriteTexture: Texture? = null
    private var renderMode: StarRenderMode = StarRenderMode.GLOW_TEXTURE

    // One update listener for billboarding + smooth fade.
    private val updateListener = Scene.OnUpdateListener { frameTime -> onFrame(frameTime) }

    init {
        // Pre-fill pool so we can handle large numbers smoothly.
        repeat(MAX_POOL) { pool.addLast(newState()) }
        buildStarRenderables()
        buildSolidRenderables()
        sceneView.scene.addOnUpdateListener(updateListener)
    }

    /**
     * Build a small palette of billboard "star sprite" renderables keyed by spectral class
     * (OBAFGKM + planets + default).
     *
     * Uses a radial-gradient drawable as a texture so we get a glowy look without custom shaders.
     */
    private fun buildStarRenderables() {
        val palette: Map<Char, Color> = mapOf(
            'O' to Color(0.60f, 0.72f, 1.00f),
            'B' to Color(0.68f, 0.78f, 1.00f),
            'A' to Color(0.90f, 0.94f, 1.00f),
            'F' to Color(1.00f, 0.98f, 0.90f),
            'G' to Color(1.00f, 0.92f, 0.70f),
            'K' to Color(1.00f, 0.75f, 0.45f),
            'M' to Color(1.00f, 0.55f, 0.50f),
            'P' to Color(0.55f, 1.00f, 0.75f), // planets
            '?' to Color(0.92f, 0.95f, 1.00f)
        )

        Texture.builder()
            .setSource(sceneView.context, R.drawable.star_glow)
            .build()
            .thenAccept { tex ->
                spriteTexture = tex
                palette.forEach { (cls, color) ->
                    MaterialFactory.makeTransparentWithTexture(sceneView.context, tex).thenAccept { material ->
                        // Tint the sprite (best effort; works with Sceneform default materials).
                        runCatching {
                            material.setFloat4("baseColorTint", color)
                        }
                        val r = ShapeFactory.makeCube(
                            Vector3(SPRITE_SIZE, SPRITE_SIZE, SPRITE_THICKNESS),
                            Vector3.zero(),
                            material
                        )
                        r.isShadowCaster = false
                        r.isShadowReceiver = false
                        renderablesBySpectralClass[cls] = r
                    }
                }
            }
    }

    fun updateStars(stars: List<StarPositionCalculator.VisibleStar>, mode: StarRenderMode = StarRenderMode.GLOW_TEXTURE) {
        renderMode = mode
        if (renderMode == StarRenderMode.SOLID && solidRenderablesBySpectralClass.isEmpty()) return
        if (renderMode != StarRenderMode.SOLID && renderablesBySpectralClass.isEmpty()) return
        renderStars(stars)
    }

    private fun renderStars(stars: List<StarPositionCalculator.VisibleStar>) {
        // Update tap data.
        visibleById.clear()
        stars.forEach { visibleById[it.star.id] = it.star }

        // Mark all active as dying; we'll revive the ones present in this update.
        active.values.forEach { it.isDying = true }

        // Add/update visible stars.
        stars.forEach { visible ->
            val id = visible.star.id

            val state = active[id] ?: acquire(id)
            state.isDying = false
            state.node.worldPosition = positionFromAltAz(visible.altitude, visible.azimuth)
            state.node.renderable = renderableFor(visible.star.spectralType)

            // Smooth fade: animate scale towards target scale (scale acts as "fade").
            state.targetScale = sizeFromMagnitude(visible.star.magnitude).toFloat()
            if (state.currentScale <= 0.001f) {
                state.currentScale = 0.001f // start tiny then ease up
            }
        }

        // Stars not present in this update will be faded out by the frame loop.
    }

    private fun positionFromAltAz(alt: Double, az: Double): Vector3 {
        val radius = 10f
        val altRad = Math.toRadians(alt)
        val azRad = Math.toRadians(az)
        val x = radius * Math.cos(altRad) * Math.sin(azRad)
        val y = radius * Math.sin(altRad)
        val z = -radius * Math.cos(altRad) * Math.cos(azRad)
        return Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    }

    private fun renderableFor(spectralType: String?): ModelRenderable {
        val cls = spectralType?.trim()?.firstOrNull()?.uppercaseChar() ?: '?'
        return when (renderMode) {
            StarRenderMode.AUTO -> renderablesBySpectralClass[cls] ?: renderablesBySpectralClass['?']!!
            StarRenderMode.SOLID -> solidRenderablesBySpectralClass[cls] ?: solidRenderablesBySpectralClass['?']!!
            StarRenderMode.CUSTOM_SHADER_GLOW -> renderablesBySpectralClass[cls] ?: renderablesBySpectralClass['?']!!
            StarRenderMode.GLOW_TEXTURE -> renderablesBySpectralClass[cls] ?: renderablesBySpectralClass['?']!!
        }
    }

    private fun sizeFromMagnitude(mag: Double): Double {
        // Clamp magnitude into a reasonable range and map to sprite scale.
        val m = mag.coerceIn(-1.5, 6.0)
        val t = (6.0 - m) / 7.5 // 0..1-ish (bright -> larger)
        return when (renderMode) {
            StarRenderMode.AUTO -> 0.35 + 1.25 * t
            StarRenderMode.SOLID -> 0.15 + 0.65 * t
            StarRenderMode.CUSTOM_SHADER_GLOW -> 0.35 + 1.25 * t
            StarRenderMode.GLOW_TEXTURE -> 0.35 + 1.25 * t
        }
    }

    private fun buildSolidRenderables() {
        val palette: Map<Char, Color> = mapOf(
            'O' to Color(0.60f, 0.72f, 1.00f),
            'B' to Color(0.68f, 0.78f, 1.00f),
            'A' to Color(0.90f, 0.94f, 1.00f),
            'F' to Color(1.00f, 0.98f, 0.90f),
            'G' to Color(1.00f, 0.92f, 0.70f),
            'K' to Color(1.00f, 0.75f, 0.45f),
            'M' to Color(1.00f, 0.55f, 0.50f),
            'P' to Color(0.55f, 1.00f, 0.75f),
            '?' to Color(0.92f, 0.95f, 1.00f)
        )

        palette.forEach { (cls, color) ->
            MaterialFactory.makeOpaqueWithColor(sceneView.context, color).thenAccept { material ->
                val r = ShapeFactory.makeSphere(
                    SOLID_RADIUS,
                    Vector3.zero(),
                    material
                )
                r.isShadowCaster = false
                r.isShadowReceiver = false
                solidRenderablesBySpectralClass[cls] = r
            }
        }
    }

    fun clearNodes() {
        sceneView.scene.removeOnUpdateListener(updateListener)
        active.values.forEach { release(it) }
        active.clear()
    }

    private fun newState(): StarNodeState {
        val node = Node().apply {
            setParent(sceneView.scene)
            // Handle taps via id stored in node.name (fast, no closure churn).
            setOnTapListener { hitTestResult, motionEvent ->
                val id = name.toIntOrNull() ?: return@setOnTapListener
                visibleById[id]?.let(onStarTapped)
            }
        }
        return StarNodeState(node = node)
    }

    private fun acquire(id: Int): StarNodeState {
        val state = pool.removeFirstOrNull() ?: newState()
        state.starId = id
        state.node.name = id.toString()
        state.isDying = false
        state.currentScale = 0.001f
        state.targetScale = 0.001f
        state.node.isEnabled = true
        active[id] = state
        return state
    }

    private fun release(state: StarNodeState) {
        state.node.isEnabled = false
        state.node.renderable = null
        state.node.name = ""
        state.starId = 0
        state.currentScale = 0f
        state.targetScale = 0f
        state.isDying = false
        if (pool.size < MAX_POOL) pool.addLast(state)
    }

    private fun onFrame(frameTime: FrameTime) {
        // Billboard + smooth scale fade for all active nodes.
        val cameraRot = sceneView.scene.camera.worldRotation
        val dt = frameTime.deltaSeconds
        val k = (dt * FADE_SPEED).coerceIn(0f, 1f)

        val iterator = active.values.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next()

            // Billboard: face the camera.
            state.node.worldRotation = cameraRot

            // Fade: scale towards target, or towards zero if dying.
            val target = if (state.isDying) 0f else state.targetScale
            state.currentScale += (target - state.currentScale) * k
            val s = state.currentScale
            state.node.localScale = Vector3(s, s, s)

            // Retire fully faded nodes.
            if (state.isDying && s <= 0.01f) {
                iterator.remove()
                release(state)
            }
        }
    }

    private fun <T> ArrayDeque<T>.removeFirstOrNull(): T? =
        if (isEmpty()) null else removeFirst()

    private companion object {
        private const val SPRITE_SIZE = 0.10f
        private const val SPRITE_THICKNESS = 0.0015f
        private const val FADE_SPEED = 10f
        private const val MAX_POOL = 2600
        private const val SOLID_RADIUS = 0.03f
    }
}

