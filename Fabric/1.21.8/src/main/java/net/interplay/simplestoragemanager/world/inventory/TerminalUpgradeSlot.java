package net.interplay.simplestoragemanager.world.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.interplay.simplestoragemanager.logic.UpgradeSlotRules;
import net.interplay.simplestoragemanager.item.inventory.StorageManagerInventoryCapability;

final class TerminalUpgradeSlot extends Slot {
    private final StorageManagerGUIMenu menu;
    private final int index;

    TerminalUpgradeSlot(StorageManagerGUIMenu menu, int index) {
        super(menu.entity.getInventory(), 0, 0, 0);
        this.menu = menu;
        this.index = index;
    }

    private StorageManagerInventoryCapability handler() {
        return new StorageManagerInventoryCapability(menu.terminalStack());
    }

    @Override
    public ItemStack getItem() {
        return handler().getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        // 1.21.8: InventoryAccess has no setStackInSlot; clear the slot, then insert the new stack.
        handler().extractItem(index, 1, false);
        if (!stack.isEmpty()) handler().insertItem(index, stack, false);
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        if (!menu.canRemoveUpgrade(index)) return ItemStack.EMPTY;
        return handler().extractItem(index, amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        int level = menu.upgradeLevel(stack);
        if (!UpgradeSlotRules.acceptsItem(index, level)) return false;
        return menu.world.isClientSide || menu.canPlaceUpgrade(index, level);
    }

    @Override
    public boolean mayPickup(Player player) {
        return menu.canRemoveUpgrade(index);
    }

    @Override
    public int getMaxStackSize() {
        return handler().getSlotLimit(index);
    }

    @Override
    public boolean isActive() {
        return !menu.world.isClientSide;
    }
}
