package net.interplay.simplestoragemanager.logic;

/** Shared ordering and capacity rules for terminal insertion previews and server mutations. */
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

    public static int capacity(int itemStackLimit, int existingCount, int slotLimit) {
        return Math.max(0, Math.min(itemStackLimit, slotLimit) - existingCount);
    }
}
