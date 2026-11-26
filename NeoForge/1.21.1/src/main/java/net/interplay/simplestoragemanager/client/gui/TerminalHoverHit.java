package net.interplay.simplestoragemanager.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.network.TerminalViewPayload;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/**
 * Hit-test do terminal para os visualizadores de receita (JEI, REI e EMI).
 *
 * <p>Enquanto a GUI do terminal está aberta, os inventários conectados não são slots reais do
 * menu: o servidor os envia como snapshots ({@link TerminalViewPayload.VisibleSlot}). Portanto o
 * hit-test precisa consultar as seções renderizadas antes dos slots locais. As teclas de atalho
 * (R = receitas, U = usos, favoritos) chamam este resolvedor com as coordenadas absolutas do mouse.</p>
 *
 * <p>Classe separada de {@link StorageManagerGUIScreen} para manter a tela enxuta: ela apenas
 * repassa seu estado por {@link Context} e expõe os dois métodos públicos que as integrações usam.</p>
 */
public final class TerminalHoverHit {

    private TerminalHoverHit() { }

    /** Estado da tela necessário para resolver o item sob o cursor. */
    public record Context(TerminalScreenLayout.Variant variant,
                          TerminalViewPayload view,
                          Map<Long, ItemStack> visualRemotePreviewItems,
                          Map<Long, ItemStack> remoteItems,
                          StorageManagerGUIMenu menu,
                          int leftPos,
                          int topPos) { }

    /** Item sob o cursor e o retângulo do slot que o contém (ambos em coordenadas da tela). */
    public record Result(ItemStack stack, Rect2i slotRect) { }

    private static final Result MISS = new Result(ItemStack.EMPTY, null);

    /**
     * Resolve o item sob o mouse, priorizando as seções remotas (snapshots), depois o slot de
     * resultado e, por fim, os slots locais (grade de craft e inventário/hotbar do jogador).
     *
     * @param context estado atual da tela
     * @param mouseX coordenada X absoluta do mouse
     * @param mouseY coordenada Y absoluta do mouse
     * @return o item e seu retângulo, ou {@link #MISS} quando não há item sob o cursor
     */
    public static Result hovered(Context context, double mouseX, double mouseY) {
        TerminalScreenLayout.Rect area = context.variant().contentArea();
        double x = mouseX - context.leftPos();
        double y = mouseY - context.topPos();

        Result remote = hoveredRemote(context, area, x, y, mouseX, mouseY);
        if (remote != null) return remote;

        TerminalScreenLayout.Point result = context.variant().result();
        if (pointHit(result, x, y)) {
            ItemStack stack = context.menu().slots.get(context.menu().craftingResultIndex).getItem();
            if (!stack.isEmpty()) {
                return new Result(stack, rect(context, result.x(), result.y()));
            }
        }

        Result local = hoveredLocalSlot(context, x, y);
        return local != null ? local : MISS;
    }

    /** Item em uma seção de inventário conectado (snapshot). */
    private static Result hoveredRemote(Context context, TerminalScreenLayout.Rect area,
                                        double x, double y, double mouseX, double mouseY) {
        if (!(mouseX >= context.leftPos() + area.left()
                && mouseX < context.leftPos() + area.right() + 1
                && mouseY >= context.topPos() + area.top()
                && mouseY < context.topPos() + area.bottom() + 1)) {
            return null;
        }

        int cursor = area.top() + 2 - context.view().scrollOffset();
        List<TerminalViewPayload.Notice> notices = new ArrayList<>(context.view().notices());
        notices.sort(Comparator.comparingInt(TerminalViewPayload.Notice::sectionIndex));
        int noticeIndex = 0;
        List<TerminalViewPayload.Section> sections = context.view().sections();

        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            while (noticeIndex < notices.size()
                    && notices.get(noticeIndex).sectionIndex() <= sectionIndex) {
                cursor += sectionHeight(notices.get(noticeIndex).slotCount());
                noticeIndex++;
            }
            TerminalViewPayload.Section section = sections.get(sectionIndex);
            int slotY = cursor + 12;
            for (int slot = 0; slot < section.slotCount(); slot++) {
                int slotX = area.left() + (slot % TerminalScreenLayout.MAIN_COLUMNS) * TerminalScreenLayout.SLOT_SIZE;
                int currentY = slotY + (slot / TerminalScreenLayout.MAIN_COLUMNS) * TerminalScreenLayout.SLOT_SIZE;
                if (x >= slotX && x < slotX + TerminalScreenLayout.SLOT_SIZE
                        && y >= currentY && y < currentY + TerminalScreenLayout.SLOT_SIZE) {
                    long slotKey = key(sectionIndex, slot);
                    ItemStack stack = context.visualRemotePreviewItems().getOrDefault(slotKey,
                            context.remoteItems().getOrDefault(slotKey, ItemStack.EMPTY));
                    if (!stack.isEmpty()) {
                        return new Result(stack, rect(context, slotX, currentY));
                    }
                }
            }
            cursor += sectionHeight(section.slotCount());
        }
        return null;
    }

    /** Item em um slot local: grade de craft (3x3), inventário principal (3x9) ou hotbar (1x9). */
    private static Result hoveredLocalSlot(Context context, double x, double y) {
        TerminalScreenLayout.Variant variant = context.variant();
        StorageManagerGUIMenu menu = context.menu();

        TerminalScreenLayout.Point craft = variant.craftOrigin();
        if (x >= craft.x() && x < craft.x() + 54 && y >= craft.y() && y < craft.y() + 54) {
            int column = (int) ((x - craft.x()) / TerminalScreenLayout.SLOT_SIZE);
            int row = (int) ((y - craft.y()) / TerminalScreenLayout.SLOT_SIZE);
            if (column >= 0 && column < 3 && row >= 0 && row < 3) {
                return localResult(context, menu.craftingGridStart + row * 3 + column,
                        craft.x() + column * TerminalScreenLayout.SLOT_SIZE,
                        craft.y() + row * TerminalScreenLayout.SLOT_SIZE);
            }
        }

        TerminalScreenLayout.Point inventory = variant.inventoryOrigin();
        if (x >= inventory.x() && x < inventory.x() + 162 && y >= inventory.y() && y < inventory.y() + 54) {
            int column = (int) ((x - inventory.x()) / TerminalScreenLayout.SLOT_SIZE);
            int row = (int) ((y - inventory.y()) / TerminalScreenLayout.SLOT_SIZE);
            if (column >= 0 && column < 9 && row >= 0 && row < 3) {
                return localResult(context, menu.mainInventoryStart + row * TerminalScreenLayout.MAIN_COLUMNS + column,
                        inventory.x() + column * TerminalScreenLayout.SLOT_SIZE,
                        inventory.y() + row * TerminalScreenLayout.SLOT_SIZE);
            }
        }

        TerminalScreenLayout.Point hotbar = variant.hotbarOrigin();
        if (x >= hotbar.x() && x < hotbar.x() + 162 && y >= hotbar.y() && y < hotbar.y() + 18) {
            int column = (int) ((x - hotbar.x()) / TerminalScreenLayout.SLOT_SIZE);
            if (column >= 0 && column < 9) {
                return localResult(context, menu.hotbarStart + column,
                        hotbar.x() + column * TerminalScreenLayout.SLOT_SIZE, hotbar.y());
            }
        }
        return null;
    }

    private static Result localResult(Context context, int slotIndex, int slotX, int slotY) {
        Slot slot = context.menu().slots.get(slotIndex);
        if (slot.getItem().isEmpty()) return null;
        return new Result(slot.getItem(), rect(context, slotX, slotY));
    }

    private static Rect2i rect(Context context, int relativeX, int relativeY) {
        return new Rect2i(context.leftPos() + relativeX, context.topPos() + relativeY, 16, 16);
    }

    private static boolean pointHit(TerminalScreenLayout.Point point, double x, double y) {
        return x >= point.x() && x < point.x() + TerminalScreenLayout.SLOT_SIZE
                && y >= point.y() && y < point.y() + TerminalScreenLayout.SLOT_SIZE;
    }

    private static long key(int section, int slot) {
        return ((long) section << 32) | (slot & 0xFFFFFFFFL);
    }

    private static int sectionHeight(int slotCount) {
        int rows = Math.max(1, (slotCount + 8) / TerminalScreenLayout.MAIN_COLUMNS);
        return 12 + rows * TerminalScreenLayout.SLOT_SIZE + 2 + 4;
    }
}
