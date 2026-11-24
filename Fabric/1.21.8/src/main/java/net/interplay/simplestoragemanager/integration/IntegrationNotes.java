package net.interplay.simplestoragemanager.integration;

/**
 * Notas sobre as integrações opcionais do build Fabric.
 *
 * <p>REI (Roughly Enough Items) é o recipe viewer com plugin dedicado: a API Fabric 20.0.811
 * é compilada como {@code modCompileOnly} e o plugin é descoberto pelo entrypoint
 * {@code rei_client}. O terminal é registrado como workstation de crafting, com transfer
 * handler para a grade 3x3 e focused-stack provider para os slots desenhados na tela.
 *
 * <p>EMI e JEI não são suportados neste build. O caminho genérico do terminal continua
 * funcionando sem eles e {@code RecipeViewerPriority} apenas detecta os ids dos mods por
 * reflexão protegida (sem dependência obrigatória).
 *
 * <p>Storage Drawers também é suportado: a capacidade {@code ITEM_HANDLER} (Chameleon) e o
 * {@code DrawerItemHandler} substituem os tipos NeoForge {@code PlatformDrawerItemHandler}/
 * {@code Capabilities.ItemHandler.BLOCK}. O handler estável mapeia os slots pela ordem de
 * {@code getAccessibleDrawerSlots()} com {@code FIRST_HANDLER_SLOT = 1}.
 *
 * <p>Sophisticated Storage/Backpacks não são suportados: dependem de APIs NeoForge
 * ({@code InventoryHandler}, {@code getExternalItemHandler}) sem equivalente Fabric 1.21.8.
 * Seus inventários seguem pelo caminho genérico {@code InventoryAccess}/Transfer API.
 */
public final class IntegrationNotes {
    private IntegrationNotes() {
    }
}
