package net.interplay.simplestoragemanager.logic;

/** Shared ordering and extraction rules for terminal inventory operations. */
public final class TerminalInsertionRules {
    public static final int PASS_COUNT = 2;

    private TerminalInsertionRules() {
    }

    public static boolean acceptsSlot(boolean empty, boolean matchesIncoming, int pass) {
        if (pass == 0) {
            return !empty && matchesIncoming;
        }
        return pass == 1 && empty;
    }

    /** Snapshot-only estimate; server mutations use the handler's insertion simulation. */
    public static int previewCapacity(int existingCount, int slotLimit) {
        return Math.max(0, slotLimit - existingCount);
    }

    public static int extractionCount(int existingCount, int itemStackLimit, boolean rightClick) {
        int limit = Math.max(0, itemStackLimit);
        if (limit == 0) return 0;
        int requested = rightClick ? Math.max(1, limit / 2) : limit;
        return Math.min(Math.max(0, existingCount), requested);
    }

    public static int shiftExtractionCount(int existingCount, int itemStackLimit) {
        return Math.min(Math.max(0, existingCount), Math.max(0, itemStackLimit));
    }

    public static int confirmedTransferCount(int requested, int simulatedExtractionCount,
                                            int simulatedInsertionCount) {
        return Math.max(0, Math.min(requested,
                Math.min(simulatedExtractionCount, simulatedInsertionCount)));
    }
}
