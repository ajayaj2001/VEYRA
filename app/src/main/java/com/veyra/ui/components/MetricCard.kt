package com.veyra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String = "",
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(VeyraDimens.IconMd),
                )
                Spacer(Modifier.width(VeyraDimens.SpaceSm))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = VeyraColors.TextTertiary,
                )
            }
            Spacer(Modifier.height(VeyraDimens.SpaceMd))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = color,
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(VeyraDimens.SpaceXs))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeyraColors.TextTertiary,
                    )
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(VeyraDimens.SpaceXs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyraColors.TextTertiary,
                )
            }
        }
    }
}
