package net.interplay.simplestoragemanager.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Exposes the terminal's upgrade component as a Fabric Transfer API storage. */
public final class UpgradeItemStorage {
    private UpgradeItemStorage() { }

    public static Storage<ItemVariant> of(ItemStack terminal) {
        return InventoryStorage.of(new UpgradeContainer(terminal), null);
    }

    private static final class UpgradeContainer implements Container {
        private final ItemStack terminal;

        private UpgradeContainer(ItemStack terminal) {
            this.terminal = terminal;
        }

        private ItemContainerContents contents() {
            return terminal.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        }

        private void set(ItemContainerContents contents) {
            terminal.set(DataComponents.CONTAINER, contents);
        }

        @Override
        public int getContainerSize() {
            return StorageManagerInventoryCapability.SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return contents().nonEmptyStream().findAny().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return new StorageManagerInventoryCapability(terminal).getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return new StorageManagerInventoryCapability(terminal).extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            contents().copyInto(items);
            if (slot >= 0 && slot < items.size()) items.set(slot, stack);
            set(ItemContainerContents.fromItems(items));
        }

        @Override
        public void clearContent() {
            set(ItemContainerContents.EMPTY);
        }

        @Override
        public void setChanged() {
            // The component is written directly, so no block entity requires notification.
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return new StorageManagerInventoryCapability(terminal).isItemValid(slot, stack);
        }
    }
}
