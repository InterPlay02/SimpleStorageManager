package net.interplay.simplestoragemanager.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalTitleLogicTest {
    private static final long DIRECTION_TICKS = TerminalTitleLogic.directionTicks();
    private static final long PAUSE_TICKS = TerminalTitleLogic.pauseTicks();
    private static final long CYCLE_TICKS = TerminalTitleLogic.cycleTicks();
    private static final int TITLE_BOX_WIDTH = 131;

    @Test
    void scrollRangeIsZeroWhenTheTitleFitsInsideTheBox() {
        assertEquals(0, TerminalTitleLogic.scrollRange(50, TITLE_BOX_WIDTH));
    }

    @Test
    void scrollRangeIsTheTextWidthMinusTheBoxWidth() {
        assertEquals(50, TerminalTitleLogic.scrollRange(181, TITLE_BOX_WIDTH));
        assertEquals(131, TerminalTitleLogic.scrollRange(262, TITLE_BOX_WIDTH));
        assertEquals(0, TerminalTitleLogic.scrollRange(131, TITLE_BOX_WIDTH));
    }

    @Test
    void scrollOffsetStartsAtZeroAndReachesMaxAfterTheTravelTicks() {
        long startTick = 100L;
        double maxScroll = 50D;
        assertEquals(0.0, TerminalTitleLogic.scrollOffset(startTick, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
        assertEquals(-25.0, TerminalTitleLogic.scrollOffset(startTick + 20L, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
        assertEquals(-50.0, TerminalTitleLogic.scrollOffset(startTick + DIRECTION_TICKS, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
    }

    @Test
    void scrollOffsetHoldsMaxForThreeSecondsAtTheLeftEnd() {
        long startTick = 0L;
        double maxScroll = 50D;
        long holdStart = startTick + DIRECTION_TICKS;
        for (long elapsed = holdStart; elapsed < holdStart + PAUSE_TICKS; elapsed++) {
            assertEquals(-maxScroll,
                    TerminalTitleLogic.scrollOffset(elapsed, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll),
                    "offset must stay at the left edge during the pause");
        }
    }

    @Test
    void scrollOffsetHoldsZeroForThreeSecondsAtTheReturnEnd() {
        long startTick = 0L;
        double maxScroll = 50D;
        long holdStart = startTick + DIRECTION_TICKS + PAUSE_TICKS + DIRECTION_TICKS;
        for (long elapsed = holdStart; elapsed < holdStart + PAUSE_TICKS; elapsed++) {
            assertEquals(0.0,
                    TerminalTitleLogic.scrollOffset(elapsed, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll),
                    "offset must stay at the start position during the pause");
        }
    }

    @Test
    void scrollOffsetLoopsBackToTheStartingPositionEveryCycle() {
        long startTick = 1000L;
        double maxScroll = 40D;
        assertEquals(0.0, TerminalTitleLogic.scrollOffset(startTick, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
        assertEquals(0.0, TerminalTitleLogic.scrollOffset(startTick + CYCLE_TICKS, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
        assertEquals(0.0, TerminalTitleLogic.scrollOffset(startTick + 2L * CYCLE_TICKS, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll));
    }

    @Test
    void scrollOffsetNeverExceedsTheScrollRangeAndStaysNonPositive() {
        long startTick = 0L;
        double maxScroll = 100D;
        for (long elapsed = 0L; elapsed <= CYCLE_TICKS; elapsed++) {
            double offset = TerminalTitleLogic.scrollOffset(elapsed, startTick, DIRECTION_TICKS, PAUSE_TICKS, maxScroll);
            assertTrue(offset <= 0.0, "offset must be zero or negative at tick " + elapsed);
            assertTrue(offset >= -maxScroll, "offset must not exceed the scroll range at tick " + elapsed);
        }
    }

    @Test
    void scrollOffsetIsZeroWithoutScrollRange() {
        assertEquals(0.0,
                TerminalTitleLogic.scrollOffset(12345L, 0L, DIRECTION_TICKS, PAUSE_TICKS, 0));
    }
}
