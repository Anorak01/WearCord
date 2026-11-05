package top.anorak01.wearcord.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class DrawerAction(val label: String, val callback: () -> Unit)

@Composable
fun ContextActionDrawer(
    title: String = "Options",
    vararg actions: Pair<String, () -> Unit>,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val drawerActions = actions.map { DrawerAction(it.first, it.second) }
    val density = LocalDensity.current

    fun settleDrawer() {
        scope.launch {
            progress.animateTo(
                targetValue = if (progress.value >= 0.35f) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                val edgeWidth = with(density) { 28.dp.toPx() }
                val drawerWidthPx = size.width * 0.86f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x < size.width - edgeWidth || progress.value >= 1f) {
                        return@awaitEachGesture
                    }
                    var previousX = down.position.x
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                        if (!change.pressed) {
                            settleDrawer()
                            break
                        }
                        val amount = change.position.x - previousX
                        if (amount != 0f) {
                            val nextProgress = (progress.value - amount / drawerWidthPx).coerceIn(0f, 1f)
                            scope.launch { progress.snapTo(nextProgress) }
                            previousX = change.position.x
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val drawerWidth = maxWidth * 0.86f
        val drawerWidthPx = with(density) { drawerWidth.toPx() }
        val contentOffset = -drawerWidthPx * progress.value
        val drawerOffset = with(density) { maxWidth.toPx() } - drawerWidthPx * progress.value

        // Both surfaces remain in the layout; the drawer never covers or intercepts the left edge.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth)
                .offset { IntOffset(contentOffset.roundToInt(), 0) },
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .offset { IntOffset(drawerOffset.roundToInt(), 0) }
                .background(MaterialTheme.colors.background),
        ) {
            if (progress.value > 0f) {
                val drawerListState = rememberScalingLazyListState()
                WearListScaffold(listState = drawerListState) {
                    item { ScreenHeader(title) }
                    item {
                        ActionChip(
                            label = "Close",
                            onClick = { scope.launch { progress.animateTo(0f) } },
                        )
                    }
                    drawerActions.forEach { action ->
                        item {
                            ActionChip(
                                label = action.label,
                                onClick = {
                                    scope.launch { progress.animateTo(0f) }
                                    action.callback()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
