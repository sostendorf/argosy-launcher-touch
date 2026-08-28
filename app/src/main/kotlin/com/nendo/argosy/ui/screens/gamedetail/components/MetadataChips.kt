package com.nendo.argosy.ui.screens.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nendo.argosy.domain.model.CompletionStatus
import com.nendo.argosy.ui.common.color
import com.nendo.argosy.ui.common.icon
import com.nendo.argosy.ui.theme.Dimens

/**
 * One fact about a game, as the detail screen shows it.
 *
 * Every stat is the same shape so every tile can be, which is the point: these were five separate
 * composables with three corner radii, two padding schemes and two type scales between them, and
 * they sat in flow rows that gave each tile whatever width its own text happened to need.
 */
data class GameStat(
    val label: String,
    val value: String,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val isUnset: Boolean = false
)

/**
 * The narrowest a tile may be drawn. Column count is derived from the width the grid is actually
 * given rather than the screen's, because these tiles appear both full-width and in a narrow column
 * beside the cover art, and the screen's width is the wrong answer in the second case.
 */
private val MIN_TILE_WIDTH = 96.dp

@Composable
fun GameStatGrid(
    stats: List<GameStat>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = ((maxWidth / MIN_TILE_WIDTH).toInt()).coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            stats.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    /**
                     * A short final row is padded on both sides rather than only the right, so a
                     * lone tile sits under the middle of the ones above it instead of hanging off
                     * the left edge of an otherwise full grid.
                     */
                    val leftover = columns - row.size
                    if (leftover > 0) {
                        Spacer(modifier = Modifier.weight(leftover / 2f))
                    }
                    row.forEach { stat ->
                        GameStatTile(stat = stat, modifier = Modifier.weight(1f))
                    }
                    if (leftover > 0) {
                        Spacer(modifier = Modifier.weight(leftover / 2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStatTile(
    stat: GameStat,
    modifier: Modifier = Modifier
) {
    val labelColor = if (stat.isUnset) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val valueColor = if (stat.isUnset) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(Dimens.radiusMd)
            )
            .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingSm)
    ) {
        Text(
            text = stat.label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            stat.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = (stat.iconTint ?: valueColor).let {
                        if (stat.isUnset) it.copy(alpha = 0.3f) else it
                    },
                    modifier = Modifier.size(Dimens.iconXs)
                )
            }
            Text(
                text = stat.value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun textStat(label: String, value: String): GameStat = GameStat(label = label, value = value)

fun communityRatingStat(rating: Float): GameStat = GameStat(
    label = "Rating",
    value = "${rating.toInt()}%",
    icon = Icons.Default.People
)

fun userRatingStat(label: String, value: Int, icon: ImageVector, iconColor: Color): GameStat {
    val isSet = value > 0
    return GameStat(
        label = label,
        value = if (isSet) "$value/10" else "--",
        icon = icon,
        iconTint = iconColor,
        isUnset = !isSet
    )
}

fun playTimeStat(minutes: Int): GameStat = GameStat(
    label = "Play Time",
    value = when {
        minutes < 60 -> "${minutes}m"
        minutes < 600 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        }
        else -> "${(minutes / 60).formatWithCommas()}h"
    },
    icon = Icons.Default.Schedule
)

fun completionStat(statusValue: String?): GameStat? {
    val status = CompletionStatus.fromApiValue(statusValue) ?: return null
    return GameStat(
        label = "Status",
        value = status.label,
        icon = status.icon,
        iconTint = status.color
    )
}

private fun Int.formatWithCommas(): String {
    return String.format("%,d", this)
}
