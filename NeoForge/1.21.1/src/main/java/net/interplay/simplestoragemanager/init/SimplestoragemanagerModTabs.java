/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.interplay.simplestoragemanager.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

@EventBusSubscriber
public class SimplestoragemanagerModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimplestoragemanagerMod.MODID);

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			tabData.accept(SimplestoragemanagerModItems.STORAGE_MANAGER.get());
			tabData.accept(SimplestoragemanagerModItems.INFINITY_UPGRADE.get());
			tabData.accept(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get());
			tabData.accept(SimplestoragemanagerModItems.MULTIDIMENSIONAL_INFINITY_UPGRADE.get());
		}
	}
}