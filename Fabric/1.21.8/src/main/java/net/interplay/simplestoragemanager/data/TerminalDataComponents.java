package net.interplay.simplestoragemanager.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;
import java.util.UUID;

/** The three data components carried by the terminal and its upgrades. */
public final class TerminalDataComponents {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.toString()),
            buffer -> UUID.fromString(buffer.readUtf(64)));

    public static final DataComponentType<UUID> TERMINAL_ID = DataComponentType.<UUID>builder()
            .persistent(UUID_CODEC)
            .networkSynchronized(UUID_STREAM_CODEC)
            .build();

    public static final DataComponentType<Boolean> TERMINAL_ADVANCED = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build();

    public static final DataComponentType<List<String>> DIMENSION_SELECTION =
            DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf())
                    // STRING_UTF8 targets ByteBuf; cast it to the registry-aware buffer type used by data components.
                    .networkSynchronized(ByteBufCodecs.<RegistryFriendlyByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8.cast()))
                    .build();

    private TerminalDataComponents() { }

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_id"), TERMINAL_ID);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "advanced"), TERMINAL_ADVANCED);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "dimension_selection"),
                DIMENSION_SELECTION);
    }
}
