package net.interplay.simplestoragemanager.logic;

import java.util.UUID;

public final class TerminalOwnershipRules {
    private TerminalOwnershipRules() {
    }

    public static boolean canUseConnectedInventories(UUID owner, UUID player) {
        return owner != null && owner.equals(player);
    }

    public static boolean canConnectInventory(UUID owner, UUID player) {
        return owner == null || owner.equals(player);
    }
}
