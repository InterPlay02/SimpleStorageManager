package net.interplay.simplestoragemanager.logic;

import java.util.ArrayList;
import java.util.List;

public final class ManualInsertionRules {
    private static final int RIGHT_CLICK_ITEM_COUNT = 1;

    private ManualInsertionRules() {
    }

    public static int clickRequest(int cursorCount, boolean rightClick) {
        int available = Math.max(0, cursorCount);
        return rightClick ? Math.min(available, RIGHT_CLICK_ITEM_COUNT) : available;
    }

    public static DragPlan planDrag(int availableCount, List<Integer> slotCapacities, boolean rightClick) {
        int remaining = Math.max(0, availableCount);
        int eligibleSlots = (int) slotCapacities.stream().filter(capacity -> capacity > 0).count();
        int leftClickShare = eligibleSlots == 0 ? 0 : remaining / eligibleSlots;
        List<Integer> insertions = new ArrayList<>(slotCapacities.size());
        for (int capacity : slotCapacities) {
            int requested = rightClick ? RIGHT_CLICK_ITEM_COUNT : leftClickShare;
            int inserted = Math.min(remaining, Math.min(Math.max(0, capacity), requested));
            insertions.add(inserted);
            remaining -= inserted;
        }
        return new DragPlan(List.copyOf(insertions), remaining);
    }

    public static int acceptedCount(int offeredCount, int remainderCount) {
        return Math.max(0, Math.min(Math.max(0, offeredCount), offeredCount - remainderCount));
    }

    public static int availableCapacity(int maxCount, int storedCount, int requestedCount) {
        return Math.min(remainingCapacity(maxCount, storedCount), Math.max(0, requestedCount));
    }

    public static int remainingCapacity(int maxCount, int storedCount) {
        return Math.max(0, maxCount - Math.max(0, storedCount));
    }

    /**
     * Regra do cadeado trancado para arrastos manuais sem Shift: quando o terminal esta trancado
     * e os itens no cursor possuem um registro valido de origem, o arrasto so pode comecar dentro
     * do inventario de origem. Iniciado em outro inventario, a operacao e cancelada antes da
     * execucao final; sem registro de origem ou com o cadeado destrancado, as regras normais valem.
     */
    public static boolean isOriginDragStartAllowed(boolean inventoryLocked, boolean cursorHasOrigin,
                                                   boolean dragStartInOriginInventory) {
        if (!inventoryLocked || !cursorHasOrigin) return true;
        return dragStartInOriginInventory;
    }

    /**
     * Política de início de arrasto manual sem Shift em terminal com cadeado: o arrasto só pode
     * começar no inventário de origem da pilha do cursor ou em um slot do inventário do jogador;
     * qualquer outro início é cancelado antes da execução. Sem cadeado, sem origem válida ou com
     * início autorizado, as regras normais se mantêm.
     */
    public static boolean isManualDragStartAllowed(boolean inventoryLocked, boolean cursorHasOrigin, boolean startInOriginInventory, boolean startInPlayerInventory) {
        if (!inventoryLocked || !cursorHasOrigin) return true;
        return startInOriginInventory || startInPlayerInventory;
    }

    /**
     * Valida o primeiro slot do arrasto manual sem Shift: o slot precisa estar vazio ou conter um
     * item compativel com o cursor. Arrastar com o botao direito so aceita slots vazios/compativeis;
     * com o botao esquerdo, um slot com item diferente do cursor realiza a substituicao (swap).
     */
    public static boolean mayDragFromSlot(boolean rightClick, boolean slotEmpty, boolean sameItemType) {
        if (slotEmpty) return true;
        return !rightClick || sameItemType;
    }

    public record DragPlan(List<Integer> insertions, int remainingCount) {
    }
}
