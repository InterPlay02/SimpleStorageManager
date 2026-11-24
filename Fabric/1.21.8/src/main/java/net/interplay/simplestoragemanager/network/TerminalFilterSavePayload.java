package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.logic.InventoryFilterLogic;

import java.util.List;

/** Client-to-server request to save the item filter being edited for one connected inventory section. */
public record TerminalFilterSavePayload(int section, int mode, List<ItemStack> items)
        implements CustomPacketPayload {
    // The shared filter codec keeps stack counts intact even above the vanilla 99 limit.
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

    /** 0 = allow, 1 = deny. */
    public boolean isAllow() { return mode == 0; }
}
