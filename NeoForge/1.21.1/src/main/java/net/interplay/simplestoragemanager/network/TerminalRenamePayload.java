package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber
public record TerminalRenamePayload(int section, String name) implements CustomPacketPayload {
    public static final Type<TerminalRenamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRenamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalRenamePayload::section,
                    ByteBufCodecs.STRING_UTF8, TerminalRenamePayload::name,
                    TerminalRenamePayload::new);

    @Override
    public Type<TerminalRenamePayload> type() {
        return TYPE;
    }

    public static void handle(TerminalRenamePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleRename(player, payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalRenamePayload::handle);
    }
}
