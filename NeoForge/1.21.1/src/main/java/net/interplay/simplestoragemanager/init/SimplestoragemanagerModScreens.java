/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.interplay.simplestoragemanager.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;

@EventBusSubscriber(Dist.CLIENT)
public class SimplestoragemanagerModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(SimplestoragemanagerModMenus.STORAGE_MANAGER_GUI.get(), StorageManagerGUIScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}
