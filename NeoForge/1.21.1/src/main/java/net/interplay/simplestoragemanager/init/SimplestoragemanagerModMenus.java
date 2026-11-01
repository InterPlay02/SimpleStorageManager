/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.interplay.simplestoragemanager.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.interplay.simplestoragemanager.network.MenuStateUpdateMessage;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.Map;

public class SimplestoragemanagerModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, SimplestoragemanagerMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<StorageManagerGUIMenu>> STORAGE_MANAGER_GUI = REGISTRY.register("storage_manager_gui", () -> IMenuTypeExtension.create(StorageManagerGUIMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof SimplestoragemanagerModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, Class<T> valueType, T defaultValue) {
			Object value = getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			return value == null || !valueType.isInstance(value) ? defaultValue : valueType.cast(value);
		}
	}
}
