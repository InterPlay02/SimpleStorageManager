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
        if (!rightClick && remaining > 0
                && slotCapacities.stream().anyMatch(capacity -> capacity > 0 && capacity < leftClickShare)) {
            for (int i = 0; i < slotCapacities.size() && remaining > 0; i++) {
                int capacity = Math.max(0, slotCapacities.get(i));
                int additional = Math.min(remaining, capacity - insertions.get(i));
                if (additional > 0) {
                    insertions.set(i, insertions.get(i) + additional);
                    remaining -= additional;
                }
            }
        }
        return new DragPlan(List.copyOf(insertions), remaining);
    }

    public static int acceptedCount(int offeredCount, int remainderCount) {
        return Math.max(0, Math.min(Math.max(0, offeredCount), offeredCount - remainderCount));
    }

    public record DragPlan(List<Integer> insertions, int remainingCount) {
    }
}
