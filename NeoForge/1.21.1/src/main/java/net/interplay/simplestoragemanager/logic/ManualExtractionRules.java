package net.interplay.simplestoragemanager.logic;

public final class ManualExtractionRules {
    private static final int HALF_DIVISOR = 2;
    private static final int MINIMUM_EXTRACTION = 1;

    private ManualExtractionRules() {
    }

    public static int rightClickAmount(int count, int itemLimit) {
        int available = Math.max(0, count);
        if (available == 0) return 0;
        int halfRoundedUp = available / HALF_DIVISOR + available % HALF_DIVISOR;
        int itemHalfLimit = Math.max(MINIMUM_EXTRACTION, Math.max(0, itemLimit) / HALF_DIVISOR);
        return Math.min(halfRoundedUp, itemHalfLimit);
    }
}
