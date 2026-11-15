package net.interplay.simplestoragemanager.logic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized logic for the per-connected-inventory item filter.
 *
 * <p>Each connected inventory (identified by its connection UUID) may have a filter configuration:
 * a list of items and a mode ({@link FilterMode#ALLOW} or {@link FilterMode#DENY}).
 *
 * <ul>
 *   <li>ALLOW with items: only items matching a filter entry may be inserted (quantities ignored).</li>
 *   <li>DENY with items: items matching a filter entry may NOT be inserted.</li>
 *   <li>Empty filter: no restriction, regardless of the selected mode.</li>
 * </ul>
 *
 * <p>Matching compares the registry name (item) and the display name of the stacks, ignoring count
 * and NBT components beyond what contributes to the display name. This class is intentionally pure:
 * it holds no persistent state and performs no network I/O.
 */
public final class InventoryFilterLogic {
    private InventoryFilterLogic() {
    }

    public enum FilterMode {
        ALLOW,
        DENY
    }

    /** A filter configuration for one connected inventory. */
    public record FilterConfig(List<ItemStack> items, FilterMode mode) {
        public FilterConfig {
            items = List.copyOf(items);
        }
    }

    /**
     * Reusable codec for an {@link ItemStack} that serializes the count separately from the item
     * data, mirroring {@code TerminalViewPayload} so remote stacks above the vanilla 99 limit are
     * preserved.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STACK_CODEC = StreamCodec.of(
            (buffer, stack) -> {
                int count = stack.isEmpty() ? 0 : stack.getCount();
                ByteBufCodecs.VAR_INT.encode(buffer, count);
                if (count > 0) {
                    buffer.writeNbt((CompoundTag) stack.copyWithCount(1).save(buffer.registryAccess()));
                }
            },
            buffer -> {
                int count = ByteBufCodecs.VAR_INT.decode(buffer);
                if (count <= 0) return ItemStack.EMPTY;
                CompoundTag tag = buffer.readNbt();
                if (tag == null) return ItemStack.EMPTY;
                ItemStack stack = ItemStack.parseOptional(buffer.registryAccess(), tag);
                if (!stack.isEmpty()) stack.setCount(count);
                return stack;
            });

    /**
     * Whether a filter entry matches a candidate stack. Only the registry name and the display name
     * are compared; the count is intentionally ignored.
     */
    public static boolean matchesFilterItem(ItemStack filterItem, ItemStack candidate) {
        if (filterItem.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        return filterItem.getItem() == candidate.getItem()
                && filterItem.getDisplayName().getString().equals(candidate.getDisplayName().getString());
    }

    /**
     * Whether a stack may be inserted into an inventory protected by the given filter config.
     * An empty filter imposes no restriction.
     */
    public static boolean isInsertAllowed(FilterConfig config, ItemStack stack) {
        if (stack.isEmpty() || config == null || config.items().isEmpty()) {
            return true;
        }
        boolean matches = config.items().stream()
                .anyMatch(filterItem -> matchesFilterItem(filterItem, stack));
        return config.mode() == FilterMode.ALLOW ? matches : !matches;
    }

    /**
     * A copy of the given stack reduced to a single unit, as used to represent a filter entry in a
     * filter slot without consuming the cursor stack.
     */
    public static ItemStack filterCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public static CompoundTag toTag(HolderLookup.Provider provider, FilterConfig config) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Mode", (byte) (config.mode() == FilterMode.ALLOW ? 0 : 1));
        ListTag items = new ListTag();
        for (ItemStack item : config.items()) {
            items.add(item.save(provider));
        }
        tag.put("Items", items);
        return tag;
    }

    public static FilterConfig fromTag(HolderLookup.Provider provider, CompoundTag tag) {
        FilterMode mode = tag.getByte("Mode") == 1 ? FilterMode.DENY : FilterMode.ALLOW;
        List<ItemStack> items = new ArrayList<>();
        ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(provider, list.getCompound(i));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return new FilterConfig(items, mode);
    }
}
