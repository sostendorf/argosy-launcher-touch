package com.nendo.argosy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nendo.argosy.ui.navigation.Screen
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.util.clickableNoFocus

/**
 * A destination the touch chrome can show. [route] is the same string the drawer navigates with, so
 * the bottom bar and the drawer stay one navigation model rather than two that can disagree.
 */
data class TouchDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * The order the bottom bar prefers, mirroring RomM's own mobile bar (browse first, tools after).
 * Destinations the drawer is currently hiding - Social when signed out, Media when no media server
 * is linked - simply never appear here, because the list is built from the drawer's live items.
 */
private val BOTTOM_BAR_PRIORITY = listOf(
    Screen.Home.route,
    Screen.Library.route,
    Screen.Collections.route,
    Screen.Downloads.route,
    Screen.Settings.route,
    Screen.MediaLibrary.route,
    Screen.SaveSync.route,
    Screen.Apps.route,
    Screen.Social.route,
    Screen.QuayPass.route
)

private const val MAX_BOTTOM_BAR_ITEMS = 5

private fun iconForRoute(route: String): ImageVector = when (route.substringBefore("?")) {
    Screen.ROUTE_HOME -> Icons.Rounded.Home
    Screen.ROUTE_LIBRARY -> Icons.Rounded.VideogameAsset
    Screen.ROUTE_COLLECTIONS -> Icons.Rounded.Bookmarks
    Screen.ROUTE_DOWNLOADS -> Icons.Rounded.Download
    Screen.ROUTE_SETTINGS -> Icons.Rounded.Settings
    Screen.ROUTE_MEDIA_LIBRARY -> Icons.Rounded.Movie
    Screen.ROUTE_SAVE_SYNC -> Icons.Rounded.CloudSync
    Screen.ROUTE_APPS -> Icons.Rounded.Apps
    Screen.ROUTE_SOCIAL -> Icons.Rounded.People
    Screen.ROUTE_QUAYPASS -> Icons.Rounded.QrCodeScanner
    else -> Icons.Rounded.Apps
}

/**
 * Picks the bottom-bar destinations out of whatever the drawer is offering, in [BOTTOM_BAR_PRIORITY]
 * order, capped at [MAX_BOTTOM_BAR_ITEMS]. Everything that does not fit is still reachable: the
 * drawer is one tap away on the top bar, and it lists every destination.
 */
fun touchDestinations(items: List<Pair<String, String>>): List<TouchDestination> {
    val byRoute = items.associateBy { it.first.substringBefore("?") }
    return BOTTOM_BAR_PRIORITY
        .mapNotNull { preferred -> byRoute[preferred.substringBefore("?")] }
        .take(MAX_BOTTOM_BAR_ITEMS)
        .map { (route, label) -> TouchDestination(route, label, iconForRoute(route)) }
}

/** Two routes are the same destination when their paths match; the query args carry filters, not identity. */
private fun sameDestination(currentRoute: String?, itemRoute: String): Boolean =
    currentRoute?.substringBefore("?") == itemRoute.substringBefore("?")

/**
 * The touch top bar: drawer handle, current section, search. Deliberately thin - the screens below
 * draw their own headers, so this bar exists to give touch users the two things a controller gets
 * from a stick click (the drawer and search) and nothing more.
 *
 * It paints no background of its own. Filling it read as a separate strip laid over the page rather
 * than part of it, so whatever surface the app is drawing - flat colour, backdrop pattern, art -
 * runs unbroken through the bar.
 */
@Composable
fun TouchTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.headerHeight)
            .padding(horizontal = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TouchIconButton(
            icon = Icons.Rounded.Menu,
            contentDescription = "Open navigation menu",
            onClick = onMenuClick
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.spacingSm)
        )
        TouchIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = "Search",
            onClick = onSearchClick
        )
    }
}

@Composable
private fun TouchIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Dimens.iconXl + Dimens.spacingMd)
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .clickableNoFocus(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(Dimens.iconMd)
        )
    }
}

/**
 * The touch bottom bar. Selection is derived from the route rather than held as its own state, so a
 * deep link, a back press and a tap all light up the same tab without a third source of truth.
 */
@Composable
fun TouchBottomNav(
    destinations: List<TouchDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (destinations.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.borderThin)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = Dimens.spacingXs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                TouchNavItem(
                    destination = destination,
                    selected = sameDestination(currentRoute, destination.route),
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Buttons whose hint has no touch equivalent, and therefore has to become a visible control.
 *
 * A and B are left out on purpose: A is "confirm", which a tap on the thing itself already does,
 * and B is "back", which the system back gesture already does. Showing them as buttons would be
 * the controller footer with different paint. The D-pad, bumper and trigger hints are navigation,
 * which touch does by scrolling, so they are dropped entirely.
 */
private val TOUCH_ACTION_BUTTONS = setOf(
    InputButton.X,
    InputButton.Y,
    InputButton.START,
    InputButton.SELECT
)

/**
 * Actions the game details screen already offers, matched by the label the screen registered.
 *
 * Once a tap opens details, a bar button that opens details is a second door to the room you just
 * walked into, and favouriting is a control that lives on the game's own page next to everything
 * else about that game. Matching on the label rather than the button is deliberate: the same button
 * carries a different action on each screen, so the button is not what makes these redundant.
 */
private val REDUNDANT_TOUCH_ACTIONS = setOf(
    "details",
    "favorite",
    "unfavorite",
    "favourite",
    "unfavourite"
)

/**
 * The touch replacement for the gamepad guide bar: the same registered hints, rendered as labelled
 * pills that say what they do instead of which button does it.
 *
 * Hints reach this bar through the same FooterHints registration every screen already uses, so a
 * screen gets its touch actions for free the moment it declares its gamepad hints - there is no
 * second list to keep in step.
 */
@Composable
fun TouchActionBar(
    hints: List<FooterHintItem>,
    onHintClick: ((InputButton) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (onHintClick == null) return
    val actions = hints.filter {
        it.button in TOUCH_ACTION_BUTTONS &&
            it.enabled &&
            it.action.trim().lowercase() !in REDUNDANT_TOUCH_ACTIONS
    }
    if (actions.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { hint ->
            Text(
                text = hint.action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.radiusPill))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickableNoFocus { onHintClick(hint.button) }
                    .padding(
                        horizontal = Dimens.buttonPaddingH,
                        vertical = Dimens.buttonPaddingV
                    )
            )
        }
    }
}

@Composable
private fun TouchNavItem(
    destination: TouchDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .heightIn(min = Dimens.menuRowHeightLg)
            .clip(RoundedCornerShape(Dimens.radiusMd))
            .clickableNoFocus(onClick)
            .padding(vertical = Dimens.spacingXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = tint,
            modifier = Modifier.size(Dimens.iconMd)
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spacingXs)
        )
    }
}
