package net.interplay.simplestoragemanager.access;

import net.minecraft.world.entity.player.Player;
import net.interplay.simplestoragemanager.data.TerminalConnection;

/** Extension point for range, dimension and ownership rules in later phases. */
@FunctionalInterface
public interface AccessRule {
    boolean allows(Player player, TerminalConnection connection);

    AccessRule ALWAYS = (player, connection) -> true;
}
