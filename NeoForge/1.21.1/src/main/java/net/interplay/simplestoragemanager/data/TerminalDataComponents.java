package net.interplay.simplestoragemanager.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import io.netty.buffer.ByteBuf;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public final class TerminalDataComponents {
    public static final DeferredRegister.DataComponents REGISTRY =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SimplestoragemanagerMod.MODID);

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.toString()),
            buffer -> UUID.fromString(buffer.readUtf(64)));

    /** The ItemStack carries only this identity. Connections live in TerminalSavedData. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TERMINAL_ID =
            REGISTRY.registerComponentType("terminal_id", builder -> builder
                    .persistent(UUID_CODEC)
                    .networkSynchronized(UUID_STREAM_CODEC));

    /** Whether the terminal has been upgraded to Advanced mode. Defaults to false. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TERMINAL_ADVANCED =
            REGISTRY.registerComponentType("advanced", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> DIMENSION_SELECTION =
            REGISTRY.registerComponentType("dimension_selection", builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.<ByteBuf, String>list()
                            .apply(ByteBufCodecs.STRING_UTF8)));

    private TerminalDataComponents() {
    }
}
