package com.example.orionstargazer.ar

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.Texture

class GalaxySkyboxNode(context: Context, anchor: Anchor, onReady: (GalaxySkyboxNode) -> Unit) : AnchorNode(anchor) {
    init {
        Texture.builder()
            .setSource(context, com.example.orionstargazer.R.drawable.bg_galaxy)
            .build()
            .thenAccept { texture ->
                MaterialFactory.makeTransparentWithTexture(context, texture).thenAccept { material ->
                    val cylinder = ShapeFactory.makeCylinder(12f, 0.01f, Vector3.zero(), material)
                    cylinder.isShadowCaster = false
                    cylinder.isShadowReceiver = false
                    this.renderable = cylinder
                    onReady(this)
                }
            }
    }
}
