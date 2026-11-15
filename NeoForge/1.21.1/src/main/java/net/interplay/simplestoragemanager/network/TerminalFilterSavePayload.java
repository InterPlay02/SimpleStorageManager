package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.logic.InventoryFilterLogic;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Client-to-server request to save the item filter being edited for one connected inventory section. */
@EventBusSubscriber
public record TerminalFilterSavePayload(int section, int mode, List<ItemStack> items)
        implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEMS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, ItemStack>list().apply(InventoryFilterLogic.STACK_CODEC);

    public static final Type<TerminalFilterSavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_filter_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalFilterSavePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.section());
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.mode());
                        ITEMS_CODEC.encode(buffer, payload.items());
                    },
                    buffer -> new TerminalFilterSavePayload(
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ITEMS_CODEC.decode(buffer)));

    public TerminalFilterSavePayload {
        items = List.copyOf(items);
        mode = Math.max(0, Math.min(1, mode));
    }

    @Override
    public Type<TerminalFilterSavePayload> type() { return TYPE; }

    public static void handle(TerminalFilterSavePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleFilterSave(player, payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalFilterSavePayload::handle);
    }

    /** 0 = allow, 1 = deny. */
    public boolean isAllow() { return mode == 0; }
}
