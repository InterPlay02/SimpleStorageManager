/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.interplay.simplestoragemanager.init;

import net.minecraft.client.gui.screens.MenuScreens;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;

public class SimplestoragemanagerModScreens {
	// Fabric liga o MenuType ao Screen diretamente (substitui o RegisterMenuScreensEvent).
	public static void register() {
		MenuScreens.register(SimplestoragemanagerModMenus.STORAGE_MANAGER_GUI, StorageManagerGUIScreen::new);
	}

	// Contrato usado pelo menu e pelo bridge para enviar estado ao cliente aberto.
	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}
