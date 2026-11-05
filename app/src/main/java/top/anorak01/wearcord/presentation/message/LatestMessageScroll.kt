package top.anorak01.wearcord.presentation.message

import androidx.wear.compose.foundation.lazy.ScalingLazyListState

internal const val LATEST_ANCHOR_ITEM_INDEX = 0

internal fun latestAnchorScrollOffset(viewportHeight: Int, bottomBarHeight: Int): Int =
    (viewportHeight / 2) - bottomBarHeight

internal suspend fun jumpToLatestMessage(
    listState: ScalingLazyListState,
    bottomBarHeight: Int,
) {
    // Item zero is a zero-height marker at the end of the newest message.
    listState.animateScrollToItem(
        index = LATEST_ANCHOR_ITEM_INDEX,
        scrollOffset = latestAnchorScrollOffset(
            viewportHeight = listState.layoutInfo.viewportSize.height,
            bottomBarHeight = bottomBarHeight,
        ),
    )
}
