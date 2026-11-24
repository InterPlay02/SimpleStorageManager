package net.interplay.simplestoragemanager.item.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.interplay.simplestoragemanager.access.InventoryAccess;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/**
 * Three upgrade slots stored in the terminal's vanilla {@code minecraft:container}
 * component. The Fabric Transfer API view is provided by UpgradeItemStorage.
 */
public final class StorageManagerInventoryCapability implements InventoryAccess {
    public static final int SLOT_COUNT = 3;

    private final ItemStack stack;

    public StorageManagerInventoryCapability(ItemStack stack) {
        this.stack = stack;
    }

    /** Closes the terminal when the carried item is tossed, as the NeoForge drop event did. */
    public static void onTerminalDropped(ItemStack dropped, Player player) {
        if (dropped.is(SimplestoragemanagerModItems.STORAGE_MANAGER)
                && player.containerMenu instanceof StorageManagerGUIMenu) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        }
    }

    private ItemContainerContents contents() {
        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private void setContents(ItemContainerContents contents) {
        stack.set(DataComponents.CONTAINER, contents);
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        contents().copyInto(items);
        return items.get(slot).copy();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack inserted, boolean simulate) {
        if (slot < 0 || slot >= SLOT_COUNT || inserted.isEmpty() || !isItemValid(slot, inserted)) {
            return inserted;
        }
        if (!getStackInSlot(slot).isEmpty()) return inserted;
        if (!simulate) {
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            contents().copyInto(items);
            items.set(slot, inserted.copyWithCount(1));
            setContents(ItemContainerContents.fromItems(items));
        }
        return inserted.getCount() <= 1 ? ItemStack.EMPTY : inserted.copyWithCount(inserted.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = getStackInSlot(slot);
        if (existing.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int taken = Math.min(amount, existing.getCount());
        if (!simulate) {
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            contents().copyInto(items);
            items.set(slot, ItemStack.EMPTY);
            setContents(ItemContainerContents.fromItems(items));
        }
        return existing.copyWithCount(taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? 1 : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack candidate) {
        return slot >= 0 && slot < SLOT_COUNT && !candidate.isEmpty()
                && !candidate.is(SimplestoragemanagerModItems.STORAGE_MANAGER);
    }
}
