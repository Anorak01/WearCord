package top.anorak01.wearcord.presentation.message

import org.junit.Assert.assertEquals
import org.junit.Test

class LatestMessageScrollTest {
    @Test
    fun latestMessageBottomIsAlignedAboveComposerForDifferentMessageHeights() {
        val viewport = 480
        val composer = 100

        for (messageHeight in listOf(80, 220, 400)) {
            val offset = latestMessageScrollOffset(viewport, composer, messageHeight)
            val itemCenter = viewport / 2 + offset
            val itemBottom = itemCenter + messageHeight / 2

            assertEquals(viewport - composer, itemBottom)
        }
    }
}
