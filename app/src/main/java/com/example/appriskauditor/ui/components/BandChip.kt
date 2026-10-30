package com.example.appriskauditor.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult

@Composable
fun BandChip(
    riskResult: RiskResult,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (riskResult.band) {
        RiskBand.LOW -> Color(0xFF2E7D32) to Color.White
        RiskBand.MEDIUM -> Color(0xFFF57F17) to Color.Black
        RiskBand.HIGH -> Color(0xFFE65100) to Color.White
        RiskBand.CRITICAL -> Color(0xFFC62828) to Color.White
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = textColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${riskResult.score}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = riskResult.band.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}
