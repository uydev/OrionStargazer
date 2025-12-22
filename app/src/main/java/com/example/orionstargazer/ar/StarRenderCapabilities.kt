package com.example.orionstargazer.ar

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class StarRenderCapabilities(
    val glEsVersion: Int,
    val glEsMajor: Int,
    val glEsMinor: Int,
    val supportsCustomShaderGlow: Boolean,
    val reasonIfUnsupported: String? = null
) {
    companion object {
        fun detect(context: Context): StarRenderCapabilities {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val req = am.deviceConfigurationInfo.reqGlEsVersion
            val major = (req shr 16) and 0xFFFF
            val minor = req and 0xFFFF

            // We implement “custom shader glow” via RuntimeShader (AGSL) which is API 33+.
            // We also require GLES 3.0+ as a conservative baseline.
            val sdkOk = Build.VERSION.SDK_INT >= 33
            val glOk = req >= 0x00030000

            val ok = sdkOk && glOk
            val reason = when {
                ok -> null
                !sdkOk -> "Requires Android 13+ (RuntimeShader)"
                !glOk -> "Requires OpenGL ES 3.0+"
                else -> "Not supported"
            }

            return StarRenderCapabilities(
                glEsVersion = req,
                glEsMajor = major,
                glEsMinor = minor,
                supportsCustomShaderGlow = ok,
                reasonIfUnsupported = reason
            )
        }
    }
}

