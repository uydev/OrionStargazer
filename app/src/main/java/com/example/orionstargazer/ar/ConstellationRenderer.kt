package com.example.orionstargazer.ar

import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import android.os.SystemClock
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory

class ConstellationRenderer(
    private val sceneView: ArSceneView,
    private val lineRadius: Float = 0.01f,
    private val lineColor: Color = Color(0.35f, 0.90f, 1.00f)
) {
    data class Segment(
        val key: String,
        val start: Vector3,
        val end: Vector3
    )

    private enum class AnimState { DRAWING_IN, STABLE, FADING_OUT }

    private data class SegmentAnim(
        var start: Vector3,
        var end: Vector3,
        val node: Node,
        var state: AnimState,
        var stateStartMs: Long,
        var durationMs: Long
    )

    private val segmentsByKey = mutableMapOf<String, SegmentAnim>()
    private var lineRenderable: ModelRenderable? = null

    init {
        buildLineRenderable()
        // Drive animations with Sceneform's per-frame updates.
        sceneView.scene.addOnUpdateListener {
            onFrame()
        }
    }

    private fun buildLineRenderable() {
        // Slightly transparent feel (alpha is limited in this Sceneform build).
        MaterialFactory.makeOpaqueWithColor(sceneView.context, lineColor)
            .thenAccept { material ->
                val cyl = ShapeFactory.makeCylinder(lineRadius, 1f, Vector3.zero(), material)
                cyl.isShadowCaster = false
                cyl.isShadowReceiver = false
                lineRenderable = cyl
            }
    }

    fun updateSegments(segments: List<Segment>) {
        val r = lineRenderable ?: return
        val now = SystemClock.uptimeMillis()
        val nextByKey = segments.associateBy { it.key }

        // Fade out disappeared segments.
        (segmentsByKey.keys - nextByKey.keys).forEach { key ->
            val anim = segmentsByKey[key] ?: return@forEach
            if (anim.state != AnimState.FADING_OUT) {
                anim.state = AnimState.FADING_OUT
                anim.stateStartMs = now
                anim.durationMs = 220L
            }
        }

        // Add/update + animate draw-in for new segments.
        segments.forEachIndexed { index, seg ->
            val existing = segmentsByKey[seg.key]
            if (existing == null) {
                val node = Node().apply { setParent(sceneView.scene) }
                node.renderable = r.makeCopy()
                val delay = (index * 55L).coerceAtMost(350L)
                segmentsByKey[seg.key] = SegmentAnim(
                    start = seg.start,
                    end = seg.end,
                    node = node,
                    state = AnimState.DRAWING_IN,
                    stateStartMs = now + delay,
                    durationMs = 650L
                )
            } else {
                existing.start = seg.start
                existing.end = seg.end
                // If it was fading but came back, re-draw it in.
                if (existing.state == AnimState.FADING_OUT) {
                    existing.state = AnimState.DRAWING_IN
                    existing.stateStartMs = now
                    existing.durationMs = 450L
                }
                if (existing.node.renderable == null) {
                    existing.node.renderable = r.makeCopy()
                }
            }
        }
    }

    private fun onFrame() {
        val now = SystemClock.uptimeMillis()

        val iterator = segmentsByKey.entries.iterator()
        while (iterator.hasNext()) {
            val (_, anim) = iterator.next()

            val direction = Vector3.subtract(anim.end, anim.start)
            val length = direction.length()
            if (length <= 1e-3f) continue

            val dirNorm = direction.normalized()
            val rotation = Quaternion.rotationBetweenVectors(Vector3.up(), dirNorm)

            val rawT = ((now - anim.stateStartMs).toFloat() / anim.durationMs.toFloat())
            val t = when (anim.state) {
                AnimState.DRAWING_IN -> rawT.coerceIn(0f, 1f)
                AnimState.STABLE -> 1f
                AnimState.FADING_OUT -> (1f - rawT.coerceIn(0f, 1f))
            }

            if (anim.state == AnimState.DRAWING_IN && rawT >= 1f) {
                anim.state = AnimState.STABLE
            }

            if (anim.state == AnimState.FADING_OUT && t <= 0f) {
                sceneView.scene.removeChild(anim.node)
                anim.node.setParent(null)
                iterator.remove()
                continue
            }

            val shownLen = kotlin.math.max(0.001f, length * t)
            val center = Vector3.add(anim.start, dirNorm.scaled(shownLen / 2f))

            anim.node.worldPosition = center
            anim.node.worldRotation = rotation
            anim.node.localScale = Vector3(1f, shownLen, 1f)
        }
    }

    fun clear() {
        segmentsByKey.values.forEach { anim ->
            val node = anim.node
            sceneView.scene.removeChild(node)
            node.setParent(null)
        }
        segmentsByKey.clear()
    }
}


