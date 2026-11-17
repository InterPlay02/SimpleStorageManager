package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client request to auto-organize one connected inventory section. */
@EventBusSubscriber
public record TerminalOrganizePayload(int section, int option) implements CustomPacketPayload {
    public static final Type<TerminalOrganizePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_organize"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalOrganizePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.section());
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.option());
                    },
                    buffer -> new TerminalOrganizePayload(
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer)));

    @Override
    public Type<TerminalOrganizePayload> type() {
        return TYPE;
    }

    public static void handle(TerminalOrganizePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleOrganize(player, payload.section(), payload.option()));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalOrganizePayload::handle);
    }
}
