package top.anorak01.wearcord.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme

private data class DrawerAction(val label: String, val callback: () -> Unit)

@Composable
fun ContextActionDrawer(
    title: String = "Options",
    vararg actions: Pair<String, () -> Unit>,
    content: @Composable () -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    val drawerActions = actions.map { DrawerAction(it.first, it.second) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val edgeWidth = with(density) { 28.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    when {
                        isOpen && down.position.x <= edgeWidth -> {
                            // Opposite edge/back gesture dismisses only the drawer.
                            isOpen = false
                            down.consume()
                        }
                        !isOpen && down.position.x >= size.width - edgeWidth -> {
                            // Open on edge contact; don't wait for touch slop or a completed drag.
                            isOpen = true
                            down.consume()
                        }
                    }
                }
            },
    ) {
        content()
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { isOpen = false },
                )
                val drawerListState = rememberScalingLazyListState()
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 280.dp)
                        .background(MaterialTheme.colors.background),
                ) {
                    WearListScaffold(listState = drawerListState) {
                        item { ScreenHeader(title) }
                        item { ActionChip(label = "Close", onClick = { isOpen = false }) }
                        drawerActions.forEach { action ->
                            item {
                                ActionChip(
                                    label = action.label,
                                    onClick = {
                                        isOpen = false
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
}
