package net.interplay.simplestoragemanager;

import net.fabricmc.api.ModInitializer;

/** Common entrypoint. All registration lives in {@link SimplestoragemanagerMod}. */
public final class SimpleStorageManager implements ModInitializer {
    @Override
    public void onInitialize() {
        SimplestoragemanagerMod.init();
    }
}
