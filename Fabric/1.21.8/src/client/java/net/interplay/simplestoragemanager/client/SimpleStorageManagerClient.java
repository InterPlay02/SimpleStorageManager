package net.interplay.simplestoragemanager.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entrypoint for the Fabric port.
 *
 * <p>Phase 2 scaffold only: screen registration (HandledScreens), client-side
 * event listeners and recipe-viewer integrations are intentionally not ported
 * yet — that is Phase 3. Kept empty/loadable so the dev client starts.</p>
 */
public final class SimpleStorageManagerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Phase 3: HandledScreens.register(...), client events, EMI/JEI/REI hooks.
    }
}
