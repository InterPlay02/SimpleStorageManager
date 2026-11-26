package net.interplay.simplestoragemanager.integration;

/**
 * Notas sobre as integrações opcionais do build Fabric.
 *
 * <p>REI (Roughly Enough Items) é o recipe viewer com plugin dedicado: a API Fabric 20.0.811
 * é compilada como {@code modCompileOnly}. O plugin cliente é descoberto pelo entrypoint
 * {@code rei_client} (focused-stack provider + transfer handler da grade 3x3) e o plugin
 * comum pelo entrypoint {@code rei_common}, que registra o display da receita de upgrade
 * no servidor para sincronização. O terminal não é registrado como workstation de crafting:
 * isso faria o atalho "usos" (U) mostrar todas as receitas de crafting do modpack.
 *
 * <p>Storage Drawers também é suportado: a capacidade {@code ITEM_HANDLER} (Chameleon) e o
 * {@code DrawerItemHandler} substituem os tipos NeoForge {@code PlatformDrawerItemHandler}/
 * {@code Capabilities.ItemHandler.BLOCK}. O handler estável mapeia os slots pela ordem de
 * {@code getAccessibleDrawerSlots()} com {@code FIRST_HANDLER_SLOT = 1}.
 */
public final class IntegrationNotes {
    private IntegrationNotes() {
    }
}
