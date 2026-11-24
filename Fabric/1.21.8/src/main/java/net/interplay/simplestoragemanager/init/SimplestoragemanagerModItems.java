package net.interplay.simplestoragemanager.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.item.DimensionalInfinityUpgradeItem;
import net.interplay.simplestoragemanager.item.InfinityUpgradeItem;
import net.interplay.simplestoragemanager.item.MultidimensionalInfinityUpgradeItem;
import net.interplay.simplestoragemanager.item.StorageManagerItem;
import net.interplay.simplestoragemanager.item.inventory.UpgradeItemStorage;

/** Registers the four items and exposes their inventory storage to other mods. */
public final class SimplestoragemanagerModItems {
    // 1.21.8 requires the registry id on Properties before the Item is constructed.
    public static final Item STORAGE_MANAGER = new StorageManagerItem(properties("storage_manager"));
    public static final Item INFINITY_UPGRADE = new InfinityUpgradeItem(properties("infinity_upgrade"));
    public static final Item DIMENSIONAL_INFINITY_UPGRADE =
            new DimensionalInfinityUpgradeItem(properties("dimensional_infinity_upgrade"));
    public static final Item MULTIDIMENSIONAL_INFINITY_UPGRADE =
            new MultidimensionalInfinityUpgradeItem(properties("multidimensional_infinity_upgrade"));

    private SimplestoragemanagerModItems() { }

    public static void register() {
        // BuiltInRegistries holds the live registry; Registries only holds resource keys.
        Registry.register(BuiltInRegistries.ITEM, id("storage_manager"), STORAGE_MANAGER);
        Registry.register(BuiltInRegistries.ITEM, id("infinity_upgrade"), INFINITY_UPGRADE);
        Registry.register(BuiltInRegistries.ITEM, id("dimensional_infinity_upgrade"), DIMENSIONAL_INFINITY_UPGRADE);
        Registry.register(BuiltInRegistries.ITEM, id("multidimensional_infinity_upgrade"), MULTIDIMENSIONAL_INFINITY_UPGRADE);

        // Upgrades live in the vanilla container component; expose them through the Transfer API.
        ItemStorage.ITEM.registerForItems((stack, context) -> UpgradeItemStorage.of(stack), STORAGE_MANAGER);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(STORAGE_MANAGER);
            entries.accept(INFINITY_UPGRADE);
            entries.accept(DIMENSIONAL_INFINITY_UPGRADE);
            entries.accept(MULTIDIMENSIONAL_INFINITY_UPGRADE);
        });
    }

    private static Item.Properties properties(String path) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, path);
    }
}
