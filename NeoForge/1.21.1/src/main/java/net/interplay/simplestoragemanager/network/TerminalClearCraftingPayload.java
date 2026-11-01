package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
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

@EventBusSubscriber
public record TerminalClearCraftingPayload() implements CustomPacketPayload {
    public static final Type<TerminalClearCraftingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_clear_crafting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalClearCraftingPayload> STREAM_CODEC =
            StreamCodec.unit(new TerminalClearCraftingPayload());

    @Override
    public Type<TerminalClearCraftingPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalClearCraftingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.clearCraftingGrid(player));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalClearCraftingPayload::handle);
    }
}
