package com.example.orionstargazer.ar

import androidx.compose.ui.graphics.Color

object SpectralColorUtil {
    fun colorForSpectralType(type: String?): Color {
        return when {
            type == null -> Color.White
            type.startsWith("O") -> Color(0xFF9BB0FF)
            type.startsWith("B") -> Color(0xFFADBFFF)
            type.startsWith("A") -> Color(0xFFD1DBFF)
            type.startsWith("F") -> Color(0xFFF8F7FF)
            type.startsWith("G") -> Color(0xFFFFF4E8)
            type.startsWith("K") -> Color(0xFFFFD2A1)
            type.startsWith("M") -> Color(0xFFFFCC6F)
            else -> Color.White
        }
    }
}
