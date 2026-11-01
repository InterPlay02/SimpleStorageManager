package net.interplay.simplestoragemanager.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.Item;

import net.interplay.simplestoragemanager.item.StorageManagerItem;
import net.interplay.simplestoragemanager.item.inventory.StorageManagerInventoryCapability;
import net.interplay.simplestoragemanager.item.MultidimensionalInfinityUpgradeItem;
import net.interplay.simplestoragemanager.item.InfinityUpgradeItem;
import net.interplay.simplestoragemanager.item.DimensionalInfinityUpgradeItem;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

@EventBusSubscriber
public class SimplestoragemanagerModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(SimplestoragemanagerMod.MODID);
	public static final DeferredItem<Item> STORAGE_MANAGER;
	public static final DeferredItem<Item> INFINITY_UPGRADE;
	public static final DeferredItem<Item> DIMENSIONAL_INFINITY_UPGRADE;
	public static final DeferredItem<Item> MULTIDIMENSIONAL_INFINITY_UPGRADE;
	static {
		STORAGE_MANAGER = REGISTRY.register("storage_manager", StorageManagerItem::new);
		INFINITY_UPGRADE = REGISTRY.register("infinity_upgrade", InfinityUpgradeItem::new);
		DIMENSIONAL_INFINITY_UPGRADE = REGISTRY.register("dimensional_infinity_upgrade", DimensionalInfinityUpgradeItem::new);
		MULTIDIMENSIONAL_INFINITY_UPGRADE = REGISTRY.register("multidimensional_infinity_upgrade", MultidimensionalInfinityUpgradeItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> new StorageManagerInventoryCapability(stack), STORAGE_MANAGER.get());
	}
}
