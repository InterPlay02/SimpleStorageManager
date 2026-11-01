package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.logic.UpgradeAccessRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber
public record DimensionalUpgradeSavePayload(List<String> selected, boolean offhand) implements CustomPacketPayload {
    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);
    public static final Type<DimensionalUpgradeSavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "dimensional_upgrade_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalUpgradeSavePayload> STREAM_CODEC =
            StreamCodec.composite(
                    STRING_LIST_CODEC, DimensionalUpgradeSavePayload::selected,
                    ByteBufCodecs.BOOL, DimensionalUpgradeSavePayload::offhand,
                    DimensionalUpgradeSavePayload::new);

    public DimensionalUpgradeSavePayload {
        selected = List.copyOf(selected);
    }

    @Override
    public Type<DimensionalUpgradeSavePayload> type() {
        return TYPE;
    }

    public static void handle(DimensionalUpgradeSavePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            boolean valid = stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get());
            Set<String> registered = new HashSet<>();
            player.registryAccess().registryOrThrow(Registries.DIMENSION).keySet()
                    .forEach(key -> registered.add(key.toString()));
            valid &= UpgradeAccessRules.isValidSelection(payload.selected(), registered);
            if (valid) {
                stack.set(TerminalDataComponents.DIMENSION_SELECTION.get(), payload.selected());
                player.getInventory().setChanged();
            }
            PacketDistributor.sendToPlayer(player, new DimensionalUpgradeSaveResultPayload(valid));
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, DimensionalUpgradeSavePayload::handle);
    }
}
