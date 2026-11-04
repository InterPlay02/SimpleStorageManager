package net.interplay.simplestoragemanager.integration;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.capabilities.PlatformDrawerItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

final class StorageDrawersStableItemHandler implements IItemHandler {
    private static final int FIRST_HANDLER_SLOT = 1;
    private static final int SINGLE_ITEM_COUNT = 1;

    private final Level level;
    private final BlockPos position;
    private final Direction side;

    StorageDrawersStableItemHandler(Level level, BlockPos position, Direction side) {
        this.level = level;
        this.position = position.immutable();
        this.side = side;
    }

    boolean matches(Level candidateLevel, BlockPos candidatePosition, Direction candidateSide) {
        return level == candidateLevel && position.equals(candidatePosition) && side == candidateSide;
    }

    @Override
    public int getSlots() {
        Context context = resolve();
        return context == null ? 0 : context.group().getDrawerCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Context context = resolve();
        int handlerSlot = mapToHandlerSlot(context, slot);
        return handlerSlot < 0 ? ItemStack.EMPTY : context.handler().getStackInSlot(handlerSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        Context context = resolve();
        IDrawer drawer = getPhysicalDrawer(context, slot);
        if (drawer == null || !canStore(drawer, stack)) return stack;

        int accepted = Math.min(stack.getCount(), remainingCapacity(drawer, stack));
        if (accepted <= 0 || simulate) return remainder(stack, accepted);

        if (drawer.isEmpty()) {
            drawer = drawer.setStoredItem(stack);
            if (drawer.isEmpty()
                    || !ItemStack.isSameItemSameComponents(drawer.getStoredItemPrototype(), stack)) {
                return stack;
            }
        }

        int before = drawer.getStoredItemCount();
        drawer.adjustStoredItemCount(accepted);
        int inserted = Math.max(0, Math.min(accepted, drawer.getStoredItemCount() - before));
        return remainder(stack, inserted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        Context context = resolve();
        int handlerSlot = mapToHandlerSlot(context, slot);
        return handlerSlot < 0 ? ItemStack.EMPTY : context.handler().extractItem(handlerSlot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        Context context = resolve();
        int handlerSlot = mapToHandlerSlot(context, slot);
        return handlerSlot < 0 ? 0 : context.handler().getSlotLimit(handlerSlot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        Context context = resolve();
        int handlerSlot = mapToHandlerSlot(context, slot);
        return handlerSlot >= 0 && context.handler().isItemValid(handlerSlot, stack);
    }

    private Context resolve() {
        if (level.isClientSide || !level.isLoaded(position)) return null;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof BlockEntityDrawers drawers)) return null;

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, position, side);
        IDrawerGroup group = BlockEntityDrawers.getGroup(drawers);
        if (!(handler instanceof PlatformDrawerItemHandler) || group == null || !group.isGroupValid()
                || handler.getSlots() != group.getDrawerCount() + FIRST_HANDLER_SLOT) return null;
        return new Context(group, handler);
    }

    private int mapToHandlerSlot(Context context, int stableSlot) {
        if (context == null || stableSlot < 0 || stableSlot >= context.group().getDrawerCount()) return -1;

        int[] order = context.group().getAccessibleDrawerSlots();
        for (int orderedSlot = 0; orderedSlot < order.length; orderedSlot++) {
            if (order[orderedSlot] == stableSlot) return orderedSlot + FIRST_HANDLER_SLOT;
        }
        return -1;
    }

    private IDrawer getPhysicalDrawer(Context context, int stableSlot) {
        if (context == null || stableSlot < 0 || stableSlot >= context.group().getDrawerCount()) return null;
        return context.group().getDrawer(stableSlot);
    }

    private static boolean canStore(IDrawer drawer, ItemStack stack) {
        return !stack.isEmpty() && drawer.isEnabled() && !drawer.isMissing()
                && !drawer.getAttributes().isSuspended()
                && !drawer.getAttributes().isUnlimitedVending()
                && drawer.canItemBeStored(stack);
    }

    private static int remainingCapacity(IDrawer drawer, ItemStack stack) {
        int stored = drawer.isEmpty() ? 0 : drawer.getStoredItemCount();
        int maximum = drawer.isEmpty() ? drawer.getMaxCapacity(stack) : drawer.getMaxCapacity();
        return Math.max(0, maximum - stored);
    }

    private static ItemStack remainder(ItemStack stack, int inserted) {
        if (inserted <= 0) return stack;
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }

    private record Context(IDrawerGroup group, IItemHandler handler) {
    }
}
