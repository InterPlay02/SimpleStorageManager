package net.interplay.simplestoragemanager.logic;

/**
 * Pure helpers for the horizontal scroll animation of terminal section titles inside the
 * fixed-width title box. The movement is a smooth ease-in/ease-out loop: the title slides
 * LEFT first to reveal the end of the text that overflows the box, holds at the maximum
 * displacement, slides back to the starting position and holds there before the next cycle.
 */
public final class TerminalTitleLogic {
    private TerminalTitleLogic() {
    }

    /** Ticks to travel from one edge to the other; preserves the original two-second speed (20 ticks per second). */
    public static long directionTicks() {
        return 40L;
    }

    /** Ticks to hold at each end of the movement; 3 seconds at 20 ticks per second. */
    public static long pauseTicks() {
        return 60L;
    }

    /** Full cycle: left travel, pause, right travel, pause. */
    public static long cycleTicks() {
        return directionTicks() + pauseTicks() + directionTicks() + pauseTicks();
    }

    public static int scrollRange(int textWidth, int boxWidth) {
        return Math.max(0, textWidth - boxWidth);
    }

    /**
     * Returns a negative offset (or zero) that moves the text LEFT to reveal overflow.
     * The animation goes: 0 → -maxScroll (hold 3 s) → 0 (hold 3 s) → repeat.
     */
    public static double scrollOffset(long nowTick, long startTick, long directionTicks, long pauseTicks,
                                      double maxScroll) {
        if (maxScroll <= 0) return 0.0;
        long direction = Math.max(1L, directionTicks);
        long pause = Math.max(0L, pauseTicks);
        long cycle = direction + pause + direction + pause;
        long elapsed = Math.max(0L, nowTick - startTick);
        long position = elapsed % cycle;
        long forwardEnd = direction;
        long holdEnd = forwardEnd + pause;
        long backEnd = holdEnd + direction;

        if (position < forwardEnd) {
            double progress = (double) position / (double) direction;
            double eased = easeInOut(progress);
            double offset = eased * maxScroll;
            return offset == 0.0 ? 0.0 : -offset;
        } else if (position < holdEnd) {
            return -maxScroll;
        } else if (position < backEnd) {
            double progress = (double) (position - holdEnd) / (double) direction;
            return -maxScroll + easeInOut(progress) * maxScroll;
        } else {
            return 0.0;
        }
    }

    private static double easeInOut(double progress) {
        return progress * progress * (3.0 - 2.0 * progress);
    }
}
