package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** Bidirectional: syncs a single menu-state entry (string, boolean, or double). */
public record MenuStateUpdateMessage(int elementType, String name, Object elementState) implements CustomPacketPayload {
	public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "guistate_update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.of(MenuStateUpdateMessage::write, MenuStateUpdateMessage::read);

	// elementType selects which value payload follows the name: 0 = string, 1 = boolean, 2 = number.
	public static void write(FriendlyByteBuf buffer, MenuStateUpdateMessage message) {
		buffer.writeInt(message.elementType);
		buffer.writeUtf(message.name);
		if (message.elementType == 0) {
			buffer.writeUtf((String) message.elementState);
		} else if (message.elementType == 1) {
			buffer.writeBoolean((boolean) message.elementState);
		} else if (message.elementType == 2 && message.elementState instanceof Number n) {
			buffer.writeDouble(n.doubleValue());
		}
	}

	public static MenuStateUpdateMessage read(FriendlyByteBuf buffer) {
		int elementType = buffer.readInt();
		String name = buffer.readUtf();
		Object elementState = null;
		if (elementType == 0) {
			elementState = buffer.readUtf();
		} else if (elementType == 1) {
			elementState = buffer.readBoolean();
		} else if (elementType == 2) {
			elementState = buffer.readDouble();
		}
		return new MenuStateUpdateMessage(elementType, name, elementState);
	}

	@Override
	public Type<MenuStateUpdateMessage> type() {
		return TYPE;
	}
}
