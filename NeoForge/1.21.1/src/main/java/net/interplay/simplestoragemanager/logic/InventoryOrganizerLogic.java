package net.interplay.simplestoragemanager.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure logic for auto-organizing a single inventory handler.
 *
 * <p>Organizing moves item stacks within the same {@link IItemHandler}, packing them into the
 * first slots (top rows) and leaving empty slots at the end. It never changes stack counts or
 * item components, and it never touches other inventories.
 *
 * <p>Supported organization modes:
 * <ul>
 *   <li>{@link OrganizeOption#NAME}: by display name, same-name stacks sub-sorted by quantity
 *       descending.</li>
 *   <li>{@link OrganizeOption#QUANTITY}: by stack quantity, equal-quantity stacks sub-sorted by
 *       name ascending.</li>
 *   <li>{@link OrganizeOption#ITEM_TYPE}: by item type. Types are defined by the Minecraft item
 *       tags each item belongs to (including tags added by mods), plus classifications made by
 *       mods themselves (item class). Items without any known type sort last, always at the
 *       bottom of the inventory.</li>
 *   <li>{@link OrganizeOption#MOD}: by mod namespace, same-mod stacks sub-sorted by name
 *       ascending.</li>
 * </ul>
 */
public final class InventoryOrganizerLogic {
    private InventoryOrganizerLogic() {
    }

    /** Organization mode. */
    public enum OrganizeOption {
        NAME, QUANTITY, ITEM_TYPE, MOD
    }

    /**
     * Persistent configuration for one connected inventory.
     *
     * @param option   selected organization mode
     * @param ascending current direction (true = ascending/A-Z, false = descending/Z-A)
     * @param organized whether this inventory has been organized at least once
     */
    public record OrganizeConfig(OrganizeOption option, boolean ascending, boolean organized) {
        public OrganizeConfig {
            option = option == null ? OrganizeOption.NAME : option;
        }
    }

    /** Default configuration for a mode: ascending except QUANTITY which starts descending. */
    public static OrganizeConfig defaultConfig(OrganizeOption option) {
        return new OrganizeConfig(option, option != OrganizeOption.QUANTITY, false);
    }

    /** Toggle the direction and mark as organized. */
    public static OrganizeConfig toggleDirection(OrganizeConfig config) {
        return new OrganizeConfig(config.option(), !config.ascending(), true);
    }

    /**
     * Organizes the given handler in place according to the config. Returns true if the handler
     * contents changed.
     */
    public static boolean organize(IItemHandler handler, OrganizeConfig config) {
        if (handler == null) return false;
        int slotCount = handler.getSlots();
        if (slotCount <= 0) return false;

        // Snapshot current state to detect actual change.
        List<ItemStack> before = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            before.add(handler.getStackInSlot(i).copy());
        }

        // Extract all non-empty stacks.
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                if (!extracted.isEmpty()) stacks.add(extracted);
            }
        }

        if (stacks.isEmpty()) return false;

        // Sort according to the selected mode and direction.
        stacks.sort(itemComparator(config.option(), config.ascending()));

        // Re-insert packed from the first slot, leaving empty slots at the end.
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack;
            for (int slot = 0; slot < slotCount && !remaining.isEmpty(); slot++) {
                if (!handler.isItemValid(slot, remaining)) continue;
                remaining = handler.insertItem(slot, remaining, false);
            }
        }

        // Detect whether anything actually changed.
        List<ItemStack> after = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            after.add(handler.getStackInSlot(i).copy());
        }
        return !before.equals(after);
    }

    /** Comparator for sorting stacks by the selected mode and direction. */
    public static Comparator<ItemStack> itemComparator(OrganizeOption option, boolean ascending) {
        Comparator<ItemStack> primary;
        switch (option) {
            case QUANTITY -> primary = Comparator.comparing(
                    (ItemStack stack) -> String.format("%010d", stack.getCount()));
            case ITEM_TYPE -> primary = Comparator.comparingInt(
                    (ItemStack stack) -> isUnknownType(stack) ? 1 : 0)
                    .thenComparing((ItemStack stack) -> itemType(stack));
            case MOD -> primary = Comparator.comparing(
                    (ItemStack stack) -> modName(stack));
            case NAME -> primary = Comparator.comparing(
                    (ItemStack stack) -> stack.getDisplayName().getString());
            default -> primary = Comparator.comparing(
                    (ItemStack stack) -> stack.getDisplayName().getString());
        }
        if (!ascending) primary = primary.reversed();

        // Secondary: within each group, always sort by name ascending (A-Z).
        Comparator<ItemStack> byName = Comparator.comparing(
                (ItemStack stack) -> stack.getDisplayName().getString());

        // For NAME, same-name stacks sub-sort by quantity descending.
        if (option == OrganizeOption.NAME) {
            Comparator<ItemStack> qtyDesc = Comparator.comparingInt(ItemStack::getCount).reversed();
            return primary.thenComparing(qtyDesc).thenComparing(byName);
        }
        return primary.thenComparing(byName);
    }

    // ------------------------------------------------------------------
    // Item type classification via Minecraft item tags
    // ------------------------------------------------------------------

    private static final String TYPE_UNKNOWN_PREFIX = "mod:";

    /** Minecraft item tags used to classify items into known type categories. */
    private enum ItemTypeTag {
        WOOD("minecraft:logs", "minecraft:wood", "minecraft:planks", "minecraft:hyphae", "minecraft:stem"),
        STONE("minecraft:stone", "minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:deepslate", "minecraft:bricks"),
        COPPER("minecraft:copper_ores", "minecraft:raw_copper", "minecraft:copper_ingots", "minecraft:copper_blocks", "minecraft:copper_items"),
        IRON("minecraft:iron_ores", "minecraft:raw_iron", "minecraft:iron_ingots", "minecraft:iron_blocks", "minecraft:iron_items"),
        GOLD("minecraft:gold_ores", "minecraft:raw_gold", "minecraft:gold_ingots", "minecraft:gold_blocks", "minecraft:gold_items"),
        DIAMOND("minecraft:diamond_ores", "minecraft:diamonds", "minecraft:diamond_blocks", "minecraft:diamond_items"),
        REDSTONE("minecraft:redstone_ores", "minecraft:redstone", "minecraft:redstone_blocks");

        private final List<TagKey<Item>> tags;

        ItemTypeTag(String... tagLocations) {
            List<TagKey<Item>> parsed = new ArrayList<>(tagLocations.length);
            for (String location : tagLocations) {
                ResourceLocation id = ResourceLocation.parse(location);
                parsed.add(TagKey.create(Registries.ITEM, id));
            }
            this.tags = List.copyOf(parsed);
        }

        public boolean matches(ItemStack stack) {
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) return true;
            }
            return false;
        }
    }

    /**
     * Classifies an item into a type category. Classification is based on the Minecraft item tags
     * the item belongs to (including tags added by mods), plus mod-made classifications via item
     * class. Items without any known type return {@code "mod:<namespace>"} and sort after all
     * known types.
     */
    public static String itemType(ItemStack stack) {
        Item item = stack.getItem();

        // Special item-based classifications (not tag-based).
        if (item == Items.ENCHANTED_BOOK) return "enchanted_book";
        if (item == Items.POTION || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW) return "potion";
        if (item instanceof SpawnEggItem) return "spawn_egg";

        // Material-based detection via Minecraft item tags (mods can add their items to these tags).
        if (ItemTypeTag.WOOD.matches(stack)) return "wood";
        if (ItemTypeTag.STONE.matches(stack)) return "stone";
        if (ItemTypeTag.COPPER.matches(stack)) return "copper";
        if (ItemTypeTag.IRON.matches(stack)) return "iron";
        if (ItemTypeTag.GOLD.matches(stack)) return "gold";
        if (ItemTypeTag.DIAMOND.matches(stack)) return "diamond";
        if (ItemTypeTag.REDSTONE.matches(stack)) return "redstone";

        // Tool / armor / weapon classification via item class (mod classifications).
        if (item instanceof ArmorItem) return "armor";
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof PickaxeItem
                || item instanceof ShovelItem || item instanceof HoeItem || item instanceof TieredItem)
            return "weapons_equipment";

        // Food detection via the Minecraft "foods" tag if present, otherwise fall back to unknown.
        if (isFoodTag(stack)) return "food";

        // No known type: sort by mod namespace, after all known types.
        return TYPE_UNKNOWN_PREFIX + modName(stack);
    }

    private static boolean isFoodTag(ItemStack stack) {
        try {
            TagKey<Item> foodsTag = TagKey.create(Registries.ITEM,
                    ResourceLocation.parse("minecraft:foods"));
            return stack.is(foodsTag);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isUnknownType(ItemStack stack) {
        return itemType(stack).startsWith(TYPE_UNKNOWN_PREFIX);
    }

    /** Returns the mod namespace of the item's registry ID. */
    public static String modName(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getNamespace();
    }
}
