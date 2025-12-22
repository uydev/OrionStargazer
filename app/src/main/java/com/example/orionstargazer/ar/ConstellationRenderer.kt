package com.example.orionstargazer.ar

import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory

class ConstellationRenderer(private val sceneView: ArSceneView) {
    data class Segment(
        val key: String,
        val start: Vector3,
        val end: Vector3
    )

    private val nodes = mutableMapOf<String, Node>()
    private var lineRenderable: ModelRenderable? = null

    init {
        buildLineRenderable()
    }

    private fun buildLineRenderable() {
        // Bright cyan-ish line, slightly transparent feel (alpha is limited in this Sceneform build).
        MaterialFactory.makeOpaqueWithColor(sceneView.context, Color(0.35f, 0.90f, 1.00f))
            .thenAccept { material ->
                val cyl = ShapeFactory.makeCylinder(0.01f, 1f, Vector3.zero(), material)
                cyl.isShadowCaster = false
                cyl.isShadowReceiver = false
                lineRenderable = cyl
            }
    }

    fun updateSegments(segments: List<Segment>) {
        val r = lineRenderable ?: return
        val nextKeys = segments.map { it.key }.toSet()

        // Remove disappeared segments.
        (nodes.keys - nextKeys).forEach { key ->
            nodes.remove(key)?.let { node ->
                sceneView.scene.removeChild(node)
                node.setParent(null)
            }
        }

        // Add/update.
        segments.forEach { seg ->
            val node = nodes.getOrPut(seg.key) {
                Node().apply { setParent(sceneView.scene) }
            }

            val direction = Vector3.subtract(seg.end, seg.start)
            val length = direction.length()
            if (length <= 1e-3f) return@forEach

            val center = Vector3.add(seg.start, seg.end).scaled(0.5f)
            val dirNorm = direction.normalized()

            // Cylinder is along +Y by default; rotate it to align with direction.
            val rotation = Quaternion.rotationBetweenVectors(Vector3.up(), dirNorm)

            node.worldPosition = center
            node.worldRotation = rotation
            node.localScale = Vector3(1f, length, 1f)
            node.renderable = r.makeCopy()
        }
    }

    fun clear() {
        nodes.values.forEach { node ->
            sceneView.scene.removeChild(node)
            node.setParent(null)
        }
        nodes.clear()
    }
}


