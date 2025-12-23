package com.example.orionstargazer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InstructionPanel(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val instructions = listOf(
        "Drag or pinch the bottom sheet to adjust your magnitude filter.",
        "Tap any star in view to pin its info card, see brightness + distance + constellation.",
        "Toggle constellations off/on and switch rendering modes from Settings.",
        "Use Highlights for quick awareness of top visible objects; use Compass + Status bar to stay oriented.",
        "When collapsed, drag the subtle handle below the sheet to bring it back up."
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE0C1324))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Orion Stargazer Instructions",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFEAF2FF)
            )
            instructions.forEachIndexed { index, line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8CD2FF)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCFE0FF)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Got it!")
            }
        }
    }
}
