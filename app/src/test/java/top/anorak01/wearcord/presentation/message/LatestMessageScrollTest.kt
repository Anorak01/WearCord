package top.anorak01.wearcord.presentation.message

import org.junit.Assert.assertEquals
import org.junit.Test

class LatestMessageScrollTest {
    @Test
    fun latestAnchorIsPositionedAtTheBottomOfVisibleContent() {
        val viewport = 480
        val composer = 100

        val offset = latestAnchorScrollOffset(viewport, composer)
        val anchorPosition = viewport / 2 + offset

        assertEquals(viewport - composer, anchorPosition)
    }

    @Test
    fun latestAnchorDoesNotRequireMessageMeasurement() {
        assertEquals(240, latestAnchorScrollOffset(viewportHeight = 480, bottomBarHeight = 0))
    }
}
