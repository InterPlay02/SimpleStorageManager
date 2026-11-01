package net.interplay.simplestoragemanager.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SimplestoragemanagerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PROTECT_CONNECTED_INVENTORIES =
            BUILDER.comment("Allow each connected inventory to be linked to only one terminal.")
                    .define("protectConnectedInventories", false);

    public static final ModConfigSpec SERVER_SPEC = BUILDER.build();

    private SimplestoragemanagerConfig() {
    }
}
