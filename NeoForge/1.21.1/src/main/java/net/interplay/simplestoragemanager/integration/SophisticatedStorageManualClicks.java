package net.interplay.simplestoragemanager.integration;

import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

public final class SophisticatedStorageManualClicks {
    public static final int UNSUPPORTED = -1;

    private SophisticatedStorageManualClicks() {
    }

    public static boolean supports(Level level, InventoryAdapter adapter, int slot) {
        return resolve(level, adapter, slot) != null;
    }

    public static int insert(Level level, InventoryAdapter adapter, int slot, ItemStack carried, int requested) {
        Target target = resolve(level, adapter, slot);
        if (target == null) return UNSUPPORTED;
        if (carried.isEmpty() || requested <= 0 || !isValidTarget(target, slot, carried)) return 0;

        ItemStack exposedStack = target.exposed().getStackInSlot(slot);
        ItemStack before = target.inventory().getStackInSlot(slot).copy();
        if (!ItemStack.matches(exposedStack, before)
                || !before.isEmpty() && !ItemStack.isSameItemSameComponents(before, carried)) return 0;

        int offeredCount = Math.min(requested, carried.getCount());
        ItemStack offered = carried.copyWithCount(offeredCount);
        ItemStack simulatedRemainder = target.inventory().insertItemOnlyToSlot(slot, offered.copy(), true);
        if (!isValidRemainder(carried, offeredCount, simulatedRemainder)) return 0;
        int simulatedAccepted = ManualInsertionRules.acceptedCount(offeredCount, simulatedRemainder.getCount());
        if (simulatedAccepted <= 0) return 0;

        ItemStack executionOffer = carried.copyWithCount(simulatedAccepted);
        ItemStack executionRemainder = target.inventory().insertItemOnlyToSlot(slot, executionOffer, false);
        int inserted = isValidRemainder(carried, simulatedAccepted, executionRemainder)
                ? ManualInsertionRules.acceptedCount(simulatedAccepted, executionRemainder.getCount())
                : observedInsertionCount(target.inventory().getStackInSlot(slot), before, carried, simulatedAccepted);
        carried.shrink(inserted);
        return inserted;
    }

    public static int simulatedCapacity(Level level, InventoryAdapter adapter, int slot, ItemStack carried) {
        Target target = resolve(level, adapter, slot);
        if (target == null) return UNSUPPORTED;
        if (carried.isEmpty() || !isValidTarget(target, slot, carried)) return 0;

        ItemStack exposedStack = target.exposed().getStackInSlot(slot);
        ItemStack before = target.inventory().getStackInSlot(slot).copy();
        if (!ItemStack.matches(exposedStack, before)
                || !before.isEmpty() && !ItemStack.isSameItemSameComponents(before, carried)) return 0;

        ItemStack offered = carried.copy();
        ItemStack remainder = target.inventory().insertItemOnlyToSlot(slot, offered, true);
        return isValidRemainder(carried, offered.getCount(), remainder)
                ? ManualInsertionRules.acceptedCount(offered.getCount(), remainder.getCount()) : 0;
    }

    public static boolean restore(Level level, InventoryAdapter adapter, int slot, ItemStack stack) {
        Target target = resolve(level, adapter, slot);
        if (target == null || slot >= target.inventory().getSlots() || stack.isEmpty()) return false;
        target.inventory().setStackInSlot(slot, stack.copy());
        return ItemStack.matches(target.inventory().getStackInSlot(slot), stack);
    }

    private static boolean isValidTarget(Target target, int slot, ItemStack stack) {
        return slot >= 0 && slot < target.inventory().getSlots()
                && target.exposed().isItemValid(slot, stack);
    }

    private static Target resolve(Level level, InventoryAdapter adapter, int slot) {
        if (level == null || level.isClientSide || adapter == null || adapter.handler() == null
                || slot < 0 || slot >= adapter.getSlots()) return null;

        IItemHandler exposed = adapter.handler();
        for (BlockPos member : adapter.connection().members()) {
            if (!level.isLoaded(member)) continue;
            BlockEntity blockEntity = level.getBlockEntity(member);
            if (blockEntity instanceof ChestBlockEntity chest && !chest.getMainPos().equals(member)) {
                BlockPos mainPos = chest.getMainPos();
                if (!level.isLoaded(mainPos)) continue;
                blockEntity = level.getBlockEntity(mainPos);
            }
            if (!(blockEntity instanceof StorageBlockEntity storage)
                    || storage.getExternalItemHandler(adapter.connection().side()) != exposed) continue;

            InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
            if (inventory == null || slot >= inventory.getSlots() || inventory.getSlots() != exposed.getSlots()) {
                continue;
            }
            return new Target(exposed, inventory);
        }
        return null;
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

    private record Target(IItemHandler exposed, InventoryHandler inventory) {
    }
}
