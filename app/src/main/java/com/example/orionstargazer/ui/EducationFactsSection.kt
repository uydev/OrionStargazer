package com.example.orionstargazer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.domain.astronomy.AstronomyFacts

@Composable
fun EducationFactsSection(
    facts: List<AstronomyFacts.Fact>,
    modifier: Modifier = Modifier,
    collapsedCount: Int = 2
) {
    if (facts.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Learn",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEAF2FF)
        )
        Spacer(Modifier.height(8.dp))

        val shown = if (expanded) facts else facts.take(collapsedCount)
        shown.forEachIndexed { idx, fact ->
            FactItem(fact)
            if (idx != shown.lastIndex) Spacer(Modifier.height(10.dp))
        }

        if (facts.size > collapsedCount) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFA9C6FF),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun FactItem(fact: AstronomyFacts.Fact) {
    Column {
        Text(
            text = fact.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFF6E3)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = fact.body,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCFE0FF)
        )
    }
}

