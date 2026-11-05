package top.anorak01.wearcord.presentation.message

import androidx.wear.compose.foundation.lazy.ScalingLazyListState

internal fun latestMessageScrollOffset(
    viewportHeight: Int,
    bottomBarHeight: Int,
    messageHeight: Int,
): Int = viewportHeight - bottomBarHeight - (messageHeight / 2) - (viewportHeight / 2)

internal suspend fun jumpToLatestMessage(
    listState: ScalingLazyListState,
    bottomBarHeight: Int,
) {
    // First measure the latest item, then align its bottom above the composer.
    listState.scrollToItem(0)
    val layout = listState.layoutInfo
    val latestItem = layout.visibleItemsInfo.firstOrNull { it.index == 0 } ?: return
    listState.animateScrollToItem(
        index = 0,
        scrollOffset = latestMessageScrollOffset(
            viewportHeight = layout.viewportSize.height,
            bottomBarHeight = bottomBarHeight,
            messageHeight = latestItem.size,
        ),
    )
}
