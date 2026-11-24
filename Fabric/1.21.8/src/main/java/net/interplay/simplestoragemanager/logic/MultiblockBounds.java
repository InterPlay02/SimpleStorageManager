package net.interplay.simplestoragemanager.logic;

/** Pure size rule for the generic 5x5x5 multiblock discovery. */
public final class MultiblockBounds {
    private MultiblockBounds() { }

    public static boolean fits(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                               int nextX, int nextY, int nextZ) {
        return Math.max(maxX, nextX) - Math.min(minX, nextX) + 1 <= 5
                && Math.max(maxY, nextY) - Math.min(minY, nextY) + 1 <= 5
                && Math.max(maxZ, nextZ) - Math.min(minZ, nextZ) + 1 <= 5;
    }
}
