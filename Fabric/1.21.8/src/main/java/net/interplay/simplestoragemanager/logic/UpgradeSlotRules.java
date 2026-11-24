package net.interplay.simplestoragemanager.logic;

public final class UpgradeSlotRules {
    public static final int TOP_SLOT = 0;
    public static final int MIDDLE_SLOT = 1;
    public static final int BOTTOM_SLOT = 2;

    public static final int LEVEL_1 = 1;
    public static final int LEVEL_2 = 2;
    public static final int LEVEL_3 = 3;

    private UpgradeSlotRules() {
    }

    public static boolean acceptsItem(int slot, int level) {
        return switch (slot) {
            case TOP_SLOT -> level == LEVEL_3;
            case MIDDLE_SLOT -> level == LEVEL_2;
            case BOTTOM_SLOT -> level == LEVEL_1;
            default -> false;
        };
    }

    public static boolean canPlace(int slot, int level, boolean hasLevel1, boolean hasLevel2) {
        if (!acceptsItem(slot, level)) return false;
        return switch (level) {
            case LEVEL_1 -> true;
            case LEVEL_2 -> hasLevel1;
            case LEVEL_3 -> hasLevel1 && hasLevel2;
            default -> false;
        };
    }

    public static boolean canRemove(int slot, boolean hasLevel2, boolean hasLevel3) {
        return switch (slot) {
            case TOP_SLOT -> true;
            case MIDDLE_SLOT -> !hasLevel3;
            case BOTTOM_SLOT -> !hasLevel2 && !hasLevel3;
            default -> false;
        };
    }
}
