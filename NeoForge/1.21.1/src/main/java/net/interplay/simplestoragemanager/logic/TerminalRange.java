package net.interplay.simplestoragemanager.logic;

public final class TerminalRange {
    public static final double BASIC_RANGE = 500.0;
    private static final double BASIC_RANGE_SQUARED = BASIC_RANGE * BASIC_RANGE;

    private TerminalRange() {
    }

    public static boolean isWithinBasicRange(double playerX, double playerY, double playerZ,
                                             int inventoryBlockX, int inventoryBlockY, int inventoryBlockZ) {
        return distanceSquaredToBlockCenter(playerX, playerY, playerZ,
                inventoryBlockX, inventoryBlockY, inventoryBlockZ) <= BASIC_RANGE_SQUARED;
    }

    public static double distanceSquaredToBlockCenter(double playerX, double playerY, double playerZ,
                                                       int inventoryBlockX, int inventoryBlockY, int inventoryBlockZ) {
        double dx = playerX - (inventoryBlockX + 0.5);
        double dy = playerY - (inventoryBlockY + 0.5);
        double dz = playerZ - (inventoryBlockZ + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }
}
