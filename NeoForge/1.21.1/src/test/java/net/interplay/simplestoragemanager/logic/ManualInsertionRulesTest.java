package net.interplay.simplestoragemanager.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualInsertionRulesTest {
    @Test
    void clickRequestsPreserveTheCursorRemainder() {
        assertEquals(12, ManualInsertionRules.clickRequest(12, false));
        assertEquals(1, ManualInsertionRules.clickRequest(12, true));
        assertEquals(0, ManualInsertionRules.clickRequest(0, true));
    }

    @Test
    void leftAndRightDragPlansConserveItemsAndSkipFullSlots() {
        ManualInsertionRules.DragPlan left = ManualInsertionRules.planDrag(
                11, List.of(12, 0, 20), false);
        assertEquals(List.of(5, 0, 5), left.insertions());
        assertEquals(1, left.remainingCount());
        assertEquals(11, left.insertions().stream().mapToInt(Integer::intValue).sum() + left.remainingCount());

        ManualInsertionRules.DragPlan right = ManualInsertionRules.planDrag(
                4, List.of(0, 20, 20), true);
        assertEquals(List.of(0, 1, 1), right.insertions());
        assertEquals(2, right.remainingCount());
        assertEquals(4, right.insertions().stream().mapToInt(Integer::intValue).sum() + right.remainingCount());
    }

    @Test
    void leftDragLeavesCapacityShortfallOnTheCursor() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(
                9, List.of(1, 20), false);
        assertEquals(List.of(1, 4), plan.insertions());
        assertEquals(4, plan.remainingCount());
        assertEquals(9, plan.insertions().stream().mapToInt(Integer::intValue).sum() + plan.remainingCount());
    }

    @Test
    void leftDragDividesAcrossFourAcceptingSlots() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(
                64, List.of(64, 64, 64, 64), false);
        assertEquals(List.of(16, 16, 16, 16), plan.insertions());
        assertEquals(0, plan.remainingCount());
    }

    @Test
    void leftDragSkipsIncompatibleSlotsAndLeavesRemainder() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(
                64, List.of(64, 64, 64, 0), false);
        assertEquals(List.of(21, 21, 21, 0), plan.insertions());
        assertEquals(1, plan.remainingCount());
    }

    @Test
    void rightDragInsertsOneIntoEachAcceptingSlot() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(
                4, List.of(0, 10, 10, 10), true);
        assertEquals(List.of(0, 1, 1, 1), plan.insertions());
        assertEquals(1, plan.remainingCount());
    }

    @Test
    void executionShortfallDebitsOnlyTheAmountActuallyAccepted() {
        int cursorBefore = 13;
        int slotBefore = 7;
        int offered = ManualInsertionRules.clickRequest(cursorBefore, false);
        int simulatedRemainder = 3;
        int simulatedAccepted = ManualInsertionRules.acceptedCount(offered, simulatedRemainder);
        int executionRemainder = 6;
        int actuallyAccepted = ManualInsertionRules.acceptedCount(simulatedAccepted, executionRemainder);

        int cursorAfter = cursorBefore - actuallyAccepted;
        int slotAfter = slotBefore + actuallyAccepted;
        assertEquals(cursorBefore + slotBefore, cursorAfter + slotAfter);
    }

    @Test
    void capacityIsLimitedByTheMeasuredDrawerCount() {
        assertEquals(12, ManualInsertionRules.availableCapacity(20, 8, 12));
        assertEquals(0, ManualInsertionRules.availableCapacity(20, 20, 12));
        assertEquals(0, ManualInsertionRules.availableCapacity(20, 22, 12));
        assertEquals(5, ManualInsertionRules.availableCapacity(20, 8, 5));
    }

    @Test
    void lockedOriginDragMustStartInsideTheOriginInventory() {
        assertTrue(ManualInsertionRules.isOriginDragStartAllowed(true, true, true));
    }

    @Test
    void lockedOriginDragOutsideOriginIsRejected() {
        assertFalse(ManualInsertionRules.isOriginDragStartAllowed(true, true, false));
    }

    @Test
    void lockedDragWithoutOriginKeepsNormalBehavior() {
        assertTrue(ManualInsertionRules.isOriginDragStartAllowed(true, false, false));
    }

    @Test
    void unlockedDragKeepsNormalBehavior() {
        assertTrue(ManualInsertionRules.isOriginDragStartAllowed(false, true, false));
    }

    @Test
    void lockedManualDragStartMayBeginInOriginOrPlayerInventory() {
        assertTrue(ManualInsertionRules.isManualDragStartAllowed(true, true, true, false));
        assertTrue(ManualInsertionRules.isManualDragStartAllowed(true, true, false, true));
        assertFalse(ManualInsertionRules.isManualDragStartAllowed(true, true, false, false));
    }

    @Test
    void lockedManualDragStartWithoutOriginKeepsNormalBehavior() {
        assertTrue(ManualInsertionRules.isManualDragStartAllowed(true, false, false, false));
    }

    @Test
    void unlockedManualDragStartKeepsNormalBehavior() {
        assertTrue(ManualInsertionRules.isManualDragStartAllowed(false, true, false, false));
    }

    @Test
    void rightClickDragRejectsIncompatibleFirstSlot() {
        assertFalse(ManualInsertionRules.mayDragFromSlot(true, false, false));
    }

    @Test
    void rightClickDragAcceptsCompatibleFirstSlot() {
        assertTrue(ManualInsertionRules.mayDragFromSlot(true, false, true));
        assertTrue(ManualInsertionRules.mayDragFromSlot(true, true, false));
    }

    @Test
    void leftClickDragAcceptsSwapOnIncompatibleFirstSlot() {
        assertTrue(ManualInsertionRules.mayDragFromSlot(false, false, false));
    }

    @Test
    void leftClickPicksUpTheEntireStackFromTheSlot() {
        assertEquals(64, ManualInsertionRules.clickRequest(64, false));
        assertEquals(12, ManualInsertionRules.clickRequest(12, false));
        assertEquals(0, ManualInsertionRules.clickRequest(0, false));
    }

    @Test
    void leftClickDepositsOnlyIntoAcceptingSlotsRespectingCapacities() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(12, List.of(8, 8, 0), false);
        assertEquals(List.of(6, 6, 0), plan.insertions());
        assertEquals(0, plan.remainingCount());
    }

    @Test
    void leftDragDistributesEquallyAndKeepsRemainderOnTheCursor() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(10, List.of(6, 6, 6), false);
        assertEquals(List.of(3, 3, 3), plan.insertions());
        assertEquals(1, plan.remainingCount());
    }

    @Test
    void leftDragEqualizesQuantitiesAcrossSelectedSlots() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(10, List.of(4, 4, 4, 4), false);
        assertEquals(List.of(2, 2, 2, 2), plan.insertions());
        assertEquals(2, plan.remainingCount());
    }

    @Test
    void fullSlotsAreSkippedAndRemaindersStayOnTheCursor() {
        ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(64, List.of(0, 8), false);
        assertEquals(List.of(0, 8), plan.insertions());
        assertEquals(56, plan.remainingCount());
    }

    @Test
    void availableCapacityRespectsItemMaximumAndStoredCount() {
        assertEquals(2, ManualInsertionRules.availableCapacity(16, 14, 16));
        assertEquals(0, ManualInsertionRules.availableCapacity(64, 64, 64));
        assertEquals(4, ManualInsertionRules.availableCapacity(64, 60, 64));
        assertEquals(16, ManualInsertionRules.availableCapacity(16, 0, 64));
    }

    @Test
    void lockedOriginDragAllowsSwapInsideOriginOnLeftClick() {
        assertTrue(ManualInsertionRules.isOriginDragStartAllowed(true, true, true));
        assertTrue(ManualInsertionRules.mayDragFromSlot(false, false, false));
    }

    @Test
    void lockedOriginRightClickDragFromOriginRejectsIncompatibleFirstSlot() {
        assertFalse(ManualInsertionRules.mayDragFromSlot(true, false, false));
    }

    @Test
    void manualDragPlansNeverCreateOrDestroyItems() {
        for (int available : new int[]{0, 1, 7, 10, 12, 64}) {
            for (List<Integer> capacities : List.of(List.of(0), List.of(8), List.of(6, 6, 6), List.of(64, 64))) {
                ManualInsertionRules.DragPlan plan = ManualInsertionRules.planDrag(available, capacities, false);
                int plannedTotal = plan.insertions().stream().mapToInt(Integer::intValue).sum() + plan.remainingCount();
                assertEquals(available, plannedTotal);
            }
        }
    }
}
