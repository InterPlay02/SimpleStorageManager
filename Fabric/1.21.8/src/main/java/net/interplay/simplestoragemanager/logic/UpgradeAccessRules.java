package net.interplay.simplestoragemanager.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Centralizes effective upgrade level, range, dimension and selection rules. */
public final class UpgradeAccessRules {
    public static final int MAX_SELECTED_DIMENSIONS = 5;

    private UpgradeAccessRules() {
    }

    public static int effectiveLevel(boolean level1Installed, boolean level2Installed,
                                     boolean level2HasSelectedDimensions, boolean level3Installed) {
        if (level3Installed) return UpgradeSlotRules.LEVEL_3;
        if (level2Installed && level2HasSelectedDimensions) return UpgradeSlotRules.LEVEL_2;
        if (level1Installed) return UpgradeSlotRules.LEVEL_1;
        return 0;
    }

    public static boolean allowsDistanceLimit(int effectiveLevel) {
        return effectiveLevel == 0;
    }

    public static boolean isWithinRange(int effectiveLevel, double playerX, double playerY, double playerZ,
                                        int inventoryX, int inventoryY, int inventoryZ) {
        return !allowsDistanceLimit(effectiveLevel)
                || TerminalRange.isWithinBasicRange(playerX, playerY, playerZ, inventoryX, inventoryY, inventoryZ);
    }

    public static boolean allowsDimension(int effectiveLevel, String playerDimension, String inventoryDimension,
                                          Set<String> selectedDimensions) {
        return switch (effectiveLevel) {
            case UpgradeSlotRules.LEVEL_1 -> playerDimension.equals(inventoryDimension);
            case UpgradeSlotRules.LEVEL_2 -> selectedDimensions.contains(inventoryDimension);
            case UpgradeSlotRules.LEVEL_3 -> true;
            default -> playerDimension.equals(inventoryDimension);
        };
    }

    public static boolean isValidSelection(List<String> selection, Set<String> registeredDimensions) {
        if (selection.size() > MAX_SELECTED_DIMENSIONS) return false;
        Set<String> unique = new HashSet<>(selection);
        return unique.size() == selection.size() && registeredDimensions.containsAll(unique);
    }
}
