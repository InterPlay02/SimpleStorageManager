package net.interplay.simplestoragemanager.integration;

import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

final class SophisticatedInventoryManualClicks {
    private SophisticatedInventoryManualClicks() {
    }

    static int insert(IItemHandler exposed, InventoryHandler inventory, int slot,
                      ItemStack carried, int requested) {
        if (!isValidTarget(exposed, inventory, slot, carried) || requested <= 0) return 0;

        ItemStack before = inventory.getStackInSlot(slot).copy();
        if (!ItemStack.matches(exposed.getStackInSlot(slot), before)
                || !before.isEmpty() && !ItemStack.isSameItemSameComponents(before, carried)) return 0;

        int offeredCount = Math.min(requested, carried.getCount());
        ItemStack offered = carried.copyWithCount(offeredCount);
        ItemStack simulatedRemainder = inventory.insertItemOnlyToSlot(slot, offered.copy(), true);
        if (!isValidRemainder(carried, offeredCount, simulatedRemainder)) return 0;
        int simulatedAccepted = ManualInsertionRules.acceptedCount(offeredCount, simulatedRemainder.getCount());
        if (simulatedAccepted <= 0) return 0;

        ItemStack executionOffer = carried.copyWithCount(simulatedAccepted);
        ItemStack executionRemainder = inventory.insertItemOnlyToSlot(slot, executionOffer, false);
        int inserted = isValidRemainder(carried, simulatedAccepted, executionRemainder)
                ? ManualInsertionRules.acceptedCount(simulatedAccepted, executionRemainder.getCount())
                : observedInsertionCount(inventory.getStackInSlot(slot), before, carried, simulatedAccepted);
        carried.shrink(inserted);
        return inserted;
    }

    static int simulatedCapacity(IItemHandler exposed, InventoryHandler inventory, int slot, ItemStack carried) {
        if (!isValidTarget(exposed, inventory, slot, carried)) return 0;

        ItemStack before = inventory.getStackInSlot(slot).copy();
        if (!ItemStack.matches(exposed.getStackInSlot(slot), before)
                || !before.isEmpty() && !ItemStack.isSameItemSameComponents(before, carried)) return 0;

        ItemStack offered = carried.copy();
        ItemStack remainder = inventory.insertItemOnlyToSlot(slot, offered, true);
        return isValidRemainder(carried, offered.getCount(), remainder)
                ? ManualInsertionRules.acceptedCount(offered.getCount(), remainder.getCount()) : 0;
    }

    static boolean restore(IItemHandler exposed, InventoryHandler inventory, int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.getSlots() || stack.isEmpty()) return false;
        inventory.setStackInSlot(slot, stack.copy());
        return ItemStack.matches(inventory.getStackInSlot(slot), stack);
    }

    private static boolean isValidTarget(IItemHandler exposed, InventoryHandler inventory,
                                         int slot, ItemStack stack) {
        return exposed != null && inventory != null && !stack.isEmpty()
                && slot >= 0 && slot < inventory.getSlots() && slot < exposed.getSlots()
                && exposed.isItemValid(slot, stack);
    }

    private static boolean isValidRemainder(ItemStack original, int offeredCount, ItemStack remainder) {
        return remainder.isEmpty()
                || ItemStack.isSameItemSameComponents(original, remainder)
                && remainder.getCount() >= 0 && remainder.getCount() <= offeredCount;
    }

    private static int observedInsertionCount(ItemStack after, ItemStack before, ItemStack inserted, int limit) {
        if (after.isEmpty() || !ItemStack.isSameItemSameComponents(after, inserted)) return 0;
        int beforeCount = before.isEmpty() ? 0 : before.getCount();
        return Math.min(limit, Math.max(0, after.getCount() - beforeCount));
    }
}
