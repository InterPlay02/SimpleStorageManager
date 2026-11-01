package net.interplay.simplestoragemanager.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class PlayerArmorGuiSlot extends Slot {
    private final StorageManagerGUIMenu menu;
    private final Player player;
    private final EquipmentSlot equipmentSlot;

    PlayerArmorGuiSlot(StorageManagerGUIMenu menu, Container container, int index, int x, int y, EquipmentSlot equipmentSlot) {
        super(container, index, x, y);
        this.menu = menu;
        this.player = menu.entity;
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.canEquip(equipmentSlot, player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return !menu.world.isClientSide;
    }
}
