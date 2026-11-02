package net.interplay.simplestoragemanager.integration;

import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public final class SophisticatedBackpacksManualClicks {
    public static final int UNSUPPORTED = -1;

    private SophisticatedBackpacksManualClicks() {
    }

    public static boolean supports(Level level, InventoryAdapter adapter, int slot) {
        return resolve(level, adapter, slot) != null;
    }

    public static int insert(Level level, InventoryAdapter adapter, int slot,
                             ItemStack carried, int requested) {
        Target target = resolve(level, adapter, slot);
        return target == null ? UNSUPPORTED
                : SophisticatedInventoryManualClicks.insert(
                        target.exposed(), target.inventory(), slot, carried, requested);
    }

    public static int simulatedCapacity(Level level, InventoryAdapter adapter, int slot, ItemStack carried) {
        Target target = resolve(level, adapter, slot);
        return target == null ? UNSUPPORTED
                : SophisticatedInventoryManualClicks.simulatedCapacity(
                        target.exposed(), target.inventory(), slot, carried);
    }

    public static boolean restore(Level level, InventoryAdapter adapter, int slot, ItemStack stack) {
        Target target = resolve(level, adapter, slot);
        return target != null
                && SophisticatedInventoryManualClicks.restore(target.exposed(), target.inventory(), slot, stack);
    }

    private static Target resolve(Level level, InventoryAdapter adapter, int slot) {
        if (level == null || level.isClientSide || adapter == null || adapter.handler() == null
                || slot < 0 || slot >= adapter.getSlots()) return null;

        IItemHandler exposed = adapter.handler();
        for (BlockPos member : adapter.connection().members()) {
            if (!level.isLoaded(member)) continue;
            BlockEntity blockEntity = level.getBlockEntity(member);
            if (!(blockEntity instanceof BackpackBlockEntity backpack)
                    || backpack.getExternalItemHandler(adapter.connection().side()) != exposed) continue;

            InventoryHandler inventory = backpack.getBackpackWrapper().getInventoryHandler();
            if (inventory == null || slot >= inventory.getSlots()
                    || inventory.getSlots() != exposed.getSlots()) continue;
            return new Target(exposed, inventory);
        }
        return null;
    }

    private record Target(IItemHandler exposed, InventoryHandler inventory) {
    }
}
