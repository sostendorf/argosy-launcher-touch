package com.nendo.argosy.ui.screens.gamedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import com.nendo.argosy.ui.screens.gamedetail.GameDownloadStatus
import com.nendo.argosy.ui.theme.LocalLauncherTheme
import com.nendo.argosy.ui.util.clickableNoFocus
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nendo.argosy.ui.common.rememberCoverAspectRatio
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.components.Box3dCover
import com.nendo.argosy.ui.components.GameTitle
import com.nendo.argosy.ui.screens.gamedetail.GameDetailUi
import com.nendo.argosy.ui.theme.ALauncherColors
import com.nendo.argosy.ui.theme.AspectRatioClass
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalBoxArtStyle
import com.nendo.argosy.ui.theme.LocalUiScale
import com.nendo.argosy.util.formatPlayTime

private val EXPANDED_COVER_WIDTH = 200.dp
private val COLLAPSED_THUMBNAIL_SIZE = 48.dp
private val COLLAPSED_BAR_HEIGHT = 64.dp

data class CollapsingHeaderState(
    val isCollapsed: Boolean = false
)

@Composable
fun CollapsingHeader(
    game: GameDetailUi,
    state: CollapsingHeaderState,
    modifier: Modifier = Modifier
) {
    val collapseProgress by animateFloatAsState(
        targetValue = if (state.isCollapsed) 1f else 0f,
        label = "collapse_progress"
    )

    Box(modifier = modifier) {
        ExpandedHeader(
            game = game,
            modifier = Modifier
                .alpha(1f - collapseProgress)
                .graphicsLayer {
                    scaleX = 1f - (collapseProgress * 0.1f)
                    scaleY = 1f - (collapseProgress * 0.1f)
                }
        )

        if (collapseProgress > 0f) {
            CollapsedHeader(
                game = game,
                modifier = Modifier
                    .alpha(collapseProgress)
            )
        }
    }
}

@Composable
fun StickyCollapsedHeader(
    game: GameDetailUi,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        CollapsedHeader(game = game)
    }
}

/**
 * The primary action as the header draws it: what the button should say and do right now.
 *
 * Passed in rather than derived here because the same decision already drives the left menu's play
 * item, and two places computing "is this downloaded" from the same state is how they end up
 * disagreeing. Null means no button - the surfaces that keep the action in the menu pass nothing.
 */
data class PrimaryActionUi(
    val downloadStatus: GameDownloadStatus,
    val downloadProgress: Float,
    val onClick: () -> Unit
)

@Composable
fun ExpandedHeader(
    game: GameDetailUi,
    modifier: Modifier = Modifier,
    primaryAction: PrimaryActionUi? = null
) {
    val aspectRatioClass = LocalUiScale.current.aspectRatioClass
    val isWideDisplay = aspectRatioClass == AspectRatioClass.WIDE ||
                        aspectRatioClass == AspectRatioClass.ULTRA_WIDE

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (isWideDisplay) {
            LandscapeExpandedHeader(game = game)
        } else {
            PortraitExpandedHeader(
                game = game,
                maxWidth = maxWidth,
                primaryAction = primaryAction
            )
        }
    }
}

/**
 * The download/play button, sized to the cover it sits under.
 *
 * Colour carries the state so it reads before the word does: green once the game is on the device,
 * orange while it is not, the error colour for a failure. Those come from the theme's semantic
 * colours, which is where green and orange are already defined for exactly this kind of signal.
 */
@Composable
private fun PrimaryActionButton(
    action: PrimaryActionUi,
    modifier: Modifier = Modifier
) {
    val semantic = LocalLauncherTheme.current.semanticColors
    val label = when (action.downloadStatus) {
        GameDownloadStatus.EXTRACTING -> "Extracting…"
        GameDownloadStatus.DOWNLOADING -> "${(action.downloadProgress * 100).toInt()}%"
        GameDownloadStatus.QUEUED -> "Queued"
        GameDownloadStatus.WAITING_FOR_STORAGE -> "No Space"
        GameDownloadStatus.PAUSED -> "Resume"
        GameDownloadStatus.FAILED -> "Retry"
        GameDownloadStatus.DOWNLOADED -> "Play"
        GameDownloadStatus.NEEDS_INSTALL -> "Install"
        GameDownloadStatus.NOT_DOWNLOADED -> "Download"
    }
    val containerColor = when (action.downloadStatus) {
        GameDownloadStatus.DOWNLOADED -> semantic.success
        GameDownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        GameDownloadStatus.DOWNLOADING,
        GameDownloadStatus.EXTRACTING,
        GameDownloadStatus.QUEUED -> semantic.progress
        else -> semantic.warning
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight)
            .clip(RoundedCornerShape(Dimens.radiusControl))
            .background(containerColor)
            .clickableNoFocus(action.onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun LandscapeExpandedHeader(
    game: GameDetailUi,
    modifier: Modifier = Modifier
) {
    val boxArtStyle = LocalBoxArtStyle.current
    val coverAspectRatio = if (boxArtStyle.nativeAspectRatio) {
        rememberCoverAspectRatio(game.coverPath, boxArtStyle.aspectRatio)
    } else {
        boxArtStyle.aspectRatio
    }
    val coverHeight = EXPANDED_COVER_WIDTH / coverAspectRatio

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXl)
    ) {
        if (game.boxSpinePath != null && game.coverPath?.startsWith("/") == true) {
            Box3dCover(
                frontPath = game.coverPath,
                spinePath = game.boxSpinePath,
                backPath = game.boxBackPath,
                modifier = Modifier.height(coverHeight)
            )
        } else {
            CoverArtImage(
                coverPath = game.coverPath,
                contentDescription = game.title,
                modifier = Modifier
                    .width(EXPANDED_COVER_WIDTH)
                    .height(coverHeight)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            TitleSection(game = game)
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            GameStatsSection(game = game)
        }
    }
}

@Composable
private fun PortraitExpandedHeader(
    game: GameDetailUi,
    maxWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    primaryAction: PrimaryActionUi? = null
) {
    val boxArtStyle = LocalBoxArtStyle.current
    val coverAspectRatio = if (boxArtStyle.nativeAspectRatio) {
        rememberCoverAspectRatio(game.coverPath, boxArtStyle.aspectRatio)
    } else {
        boxArtStyle.aspectRatio
    }
    /**
     * The cover column takes half the row, so the art and the button under it are never narrower
     * than the stat tiles opposite. At the old 40% the art was the smaller half of a two-column
     * layout while being the thing the page is about.
     */
    val coverWidth = (maxWidth - Dimens.spacingMd) / 2
    val coverHeight = coverWidth / coverAspectRatio

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        TitleSection(game = game)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            Column(
                modifier = Modifier.width(coverWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                if (game.boxSpinePath != null && game.coverPath?.startsWith("/") == true) {
                    Box3dCover(
                        frontPath = game.coverPath,
                        spinePath = game.boxSpinePath,
                        backPath = game.boxBackPath,
                        modifier = Modifier.height(coverHeight)
                    )
                } else {
                    CoverArtImage(
                        coverPath = game.coverPath,
                        contentDescription = game.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(coverHeight)
                    )
                }

                primaryAction?.let { action ->
                    PrimaryActionButton(action = action)
                }
            }

            GameStatsSection(
                game = game,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoverArtImage(
    coverPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = rememberFileImageModel(coverPath),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleSection(
    game: GameDetailUi,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        GameTitle(
            title = game.title,
            titleStyle = MaterialTheme.typography.displaySmall,
            titleColor = MaterialTheme.colorScheme.onSurface,
            adaptiveSize = true
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
            verticalAlignment = Alignment.Bottom
        ) {
            EndWeightedText(
                text = game.platformName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false)
            )
            game.releaseYear?.let { year ->
                Text(text = "|", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingXs))

        game.developer?.let { dev ->
            Text(
                text = dev,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        game.genre?.let { genreString ->
            val genres = genreString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
                    maxLines = 2
                ) {
                    genres.forEach { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(Dimens.radiusSm)
                                )
                                .padding(horizontal = Dimens.spacingSm, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Every stat the detail header shows, in one list so one grid can lay them all out at a single
 * width. They used to be two flow rows of five differently-styled chips, each sized to its own text.
 */
@Composable
private fun gameStats(game: GameDetailUi): List<GameStat> = buildList {
    game.players?.let { add(textStat("Players", it)) }
    game.rating?.let { add(communityRatingStat(it)) }
    add(
        userRatingStat(
            label = "My Rating",
            value = game.userRating,
            icon = Icons.Default.Star,
            iconColor = ALauncherColors.StarGold
        )
    )
    add(
        userRatingStat(
            label = "Difficulty",
            value = game.userDifficulty,
            icon = Icons.Default.Whatshot,
            iconColor = ALauncherColors.DifficultyRed
        )
    )
    if (game.playTimeMinutes > 0) add(playTimeStat(game.playTimeMinutes))
    completionStat(game.status)?.let { add(it) }
    game.timeToBeatMain?.let { add(textStat("Main Story", it)) }
    game.timeToBeatCompletionist?.let { add(textStat("Completionist", it)) }
}

@Composable
private fun GameStatsSection(
    game: GameDetailUi,
    modifier: Modifier = Modifier
) {
    GameStatGrid(stats = gameStats(game), modifier = modifier)
}

@Composable
internal fun CollapsedHeader(
    game: GameDetailUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(COLLAPSED_BAR_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        AsyncImage(
            model = rememberFileImageModel(game.coverPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(COLLAPSED_THUMBNAIL_SIZE)
                .clip(RoundedCornerShape(Dimens.radiusSm))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.platformName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
        ) {
            if (game.rating != null || game.userRating > 0 || game.userDifficulty > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    game.rating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${rating.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (game.userRating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ALauncherColors.StarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${game.userRating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (game.userDifficulty > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = ALauncherColors.DifficultyRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${game.userDifficulty}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (game.playTimeMinutes > 0 || game.playCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (game.playTimeMinutes > 0) {
                        Text(
                            text = formatPlayTime(game.playTimeMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (game.playCount > 0) {
                        Text(
                            text = if (game.playCount == 1) "1 play" else "${game.playCount} plays",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

