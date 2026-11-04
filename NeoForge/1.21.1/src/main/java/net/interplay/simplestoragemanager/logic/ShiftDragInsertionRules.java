package net.interplay.simplestoragemanager.logic;

/** Separate slot ceilings for Container-backed inventories and handler-backed inventories. */
public final class ShiftDragInsertionRules {
    private ShiftDragInsertionRules() {
    }

    public static int vanillaTotalLimit(int slotLimit, int itemLimit) {
        return Math.min(Math.max(0, slotLimit), Math.max(0, itemLimit));
    }

    public static int moddedTotalLimit(int slotLimit, int itemLimit) {
        int safeSlotLimit = Math.max(0, slotLimit);
        int safeItemLimit = Math.max(0, itemLimit);
        return safeSlotLimit > TerminalInsertionRules.VANILLA_MAX_SLOT_SIZE && safeItemLimit > 1
                ? safeSlotLimit : Math.min(safeSlotLimit, safeItemLimit);
    }
}
