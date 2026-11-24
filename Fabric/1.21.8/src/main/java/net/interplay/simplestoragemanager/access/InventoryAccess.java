package net.interplay.simplestoragemanager.access;

import net.minecraft.world.item.ItemStack;

/**
 * Slot-oriented view of an inventory used by the terminal logic.
 *
 * <p>The operations mirror the semantics previously provided by item handlers:
 * every slot can be queried, an insertion/extraction can be simulated, and the
 * effective per-slot capacity may exceed the item's vanilla stack size.</p>
 */
public interface InventoryAccess {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    /** Returns the part of {@code stack} that could not be inserted. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Returns the stack that was extracted, possibly with a count above 99. */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);
}
