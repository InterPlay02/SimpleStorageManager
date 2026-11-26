package net.interplay.simplestoragemanager.integration;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/**
 * Integração REI (Roughly Enough Items) do terminal, descoberta pelo entrypoint
 * {@code rei_client} do fabric.mod.json — sem anotação {@code @REIPluginClient}.
 *
 * <p>Registra o focused-stack provider do terminal e o transfer handler da grade 3x3.
 * O display da receita de upgrade é registrado no lado servidor por
 * {@link ReiUpgradeRecipePlugin}, para que o REI o sincronize aos clientes.
 *
 * <p>O Storage Manager não deve ser registrado como workstation da categoria de crafting:
 * o REI interpretaria "usos" (U) do item como todas as receitas feitas naquela bancada.
 */
public final class ReiTerminalIntegration implements REIClientPlugin {

    /** Mesmo id do {@code BuiltinPlugin.CRAFTING} do REI, sem depender do artefato de plugins. */
    private static final CategoryIdentifier<Display> CRAFTING =
            CategoryIdentifier.of("minecraft", "plugins/crafting");

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStackProvider());
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        // Os 9 slots da grade são o destino do botão "+" do REI; o inventário do jogador é a fonte.
        registry.register(SimpleTransferHandler.create(
                StorageManagerGUIMenu.class,
                CRAFTING,
                new SimpleTransferHandler.IntRange(
                        StorageManagerGUIMenu.CRAFTING_GRID_START,
                        StorageManagerGUIMenu.CRAFTING_GRID_START + StorageManagerGUIMenu.CRAFTING_GRID_COUNT)));
    }

    /** Responde pelo item sob o mouse em qualquer área da GUI do terminal. */
    private static final class TerminalFocusedStackProvider implements FocusedStackProvider {

        @Override
        public dev.architectury.event.CompoundEventResult<EntryStack<?>> provide(Screen screen, Point mouse) {
            if (!(screen instanceof StorageManagerGUIScreen terminal)) {
                return pass();
            }
            ItemStack stack = terminal.getHoveredItem(mouse.x, mouse.y);
            return stack.isEmpty() ? pass() : focused(EntryStacks.of(new ItemStack(stack.getItem())));
        }

        @Override
        public double getPriority() {
            // O provider padrão do REI usa hoveredSlot e roda em prioridade -10.
            return 100.0;
        }
    }

    /**
     * A API REI 20.0.811 devolve {@code CompoundEventResult<EntryStack<?>>}. O tipo é usado
     * totalmente qualificado para não criar um import direto de architectury (dependência
     * transitiva do REI, disponível apenas quando o REI está instalado).
     */
    private static dev.architectury.event.CompoundEventResult<EntryStack<?>> focused(EntryStack<?> stack) {
        return dev.architectury.event.CompoundEventResult.interruptTrue(stack);
    }

    private static dev.architectury.event.CompoundEventResult<EntryStack<?>> pass() {
        return dev.architectury.event.CompoundEventResult.pass();
    }
}
