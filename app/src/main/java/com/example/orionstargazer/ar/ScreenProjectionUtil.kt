package com.example.orionstargazer.ar

import android.graphics.Point
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3

object ScreenProjectionUtil {
    /**
     * Projects a world 3D position to a 2D screen pixel, given the ArSceneView.
     * Returns null if projection is not possible.
     */
    fun projectWorldToScreen(
        sceneView: ArSceneView?,
        world: Vector3?
    ) : Point? {
        if (sceneView == null || world == null) return null
        val projection = sceneView.scene.camera.worldToScreenPoint(world)
        if (projection == null) return null
        return Point(projection.x.toInt(), projection.y.toInt())
    }
}
