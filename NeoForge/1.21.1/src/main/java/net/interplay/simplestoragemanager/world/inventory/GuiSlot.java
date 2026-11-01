package net.interplay.simplestoragemanager.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/** Slots are rendered by the custom screen so their coordinates can scroll and use BIG layout. */
public class GuiSlot extends Slot {
    private final StorageManagerGUIMenu menu;

    public GuiSlot(StorageManagerGUIMenu menu, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean isActive() {
        return !menu.world.isClientSide;
    }
}
