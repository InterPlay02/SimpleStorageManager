package net.interplay.simplestoragemanager.init;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.interplay.simplestoragemanager.ClientBridge;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.network.MenuStateUpdateMessage;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.Map;

/** Registers the terminal menu type and the generic menu-state accessor. */
public final class SimplestoragemanagerModMenus {
    // The hand travels as an extra screen-opening byte: 0 = main hand, 1 = off hand.
    public static final ExtendedScreenHandlerType<StorageManagerGUIMenu, Byte> STORAGE_MANAGER_GUI =
            new ExtendedScreenHandlerType<>((id, inventory, hand) -> new StorageManagerGUIMenu(
                    id, inventory, hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND),
                    ByteBufCodecs.BYTE.cast());

    private SimplestoragemanagerModMenus() { }

    public static void register() {
        // BuiltInRegistries holds the live registry; Registries only holds resource keys.
        Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "storage_manager_gui"),
                STORAGE_MANAGER_GUI);
    }

    /** Shared menu state map plus the two-sided menu-state message. */
    public interface MenuAccessor {
        Map<String, Object> getMenuState();

        Map<Integer, Slot> getSlots();

        default void sendMenuStateUpdate(Player player, int elementType, String name,
                                         Object elementState, boolean needClientUpdate) {
            getMenuState().put(elementType + ":" + name, elementState);
            MenuStateUpdateMessage message = new MenuStateUpdateMessage(elementType, name, elementState);
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, message);
            } else if (player.level().isClientSide) {
                if (needClientUpdate) ClientBridge.menuStateHandler.accept(message);
                ClientBridge.menuStateSender.accept(message);
            }
        }

        default <T> T getMenuState(int elementType, String name, Class<T> valueType, T defaultValue) {
            Object value = getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
            return value == null || !valueType.isInstance(value) ? defaultValue : valueType.cast(value);
        }
    }
}
