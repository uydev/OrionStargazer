package com.example.orionstargazer.ar

import android.content.Context
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object StarMaterialFactory {
    suspend fun makeGlowyStar(context: Context, color: Color, scale: Float): ModelRenderable = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            MaterialFactory.makeOpaqueWithColor(context, color)
                .thenAcceptAsync({ material ->
                    val sphere = ShapeFactory.makeSphere(scale, Vector3.zero(), material)
                    cont.resume(sphere)
                }, { runnable -> runnable.run() })
        }
    }
}
