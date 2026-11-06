package net.interplay.simplestoragemanager.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiftDragPlannerTest {
    @Test
    void vanillaPartialStacksAreFilledBeforeEmptySlotsAcrossSources() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(10, 4), source(11, 4)),
                List.of(slot(0, 60, 64, ShiftDragInsertionRules.vanillaTotalLimit(64, 64)),
                        slot(1, 0, 0, ShiftDragInsertionRules.vanillaTotalLimit(64, 64))));

        assertEquals(List.of(new ShiftDragPlanner.SourceResult(10, 0),
                new ShiftDragPlanner.SourceResult(11, 0)), plan.sources());
        assertEquals(List.of(64, 4), counts(plan));
        assertEquals(List.of(4, 4), transferCounts(plan));
    }

    @Test
    void onlyAvailableEmptySlotIsReservedByTheFirstSource() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(20, 4), source(21, 4), source(22, 4)),
                List.of(slot(0, 64, 64, 64), slot(1, 0, 0, 64)));

        assertEquals(List.of(64, 12), counts(plan));
        assertEquals(List.of(0, 0, 0), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
        assertEquals(List.of(4, 4, 4), transferCounts(plan));
    }

    @Test
    void moddedSlotsAboveSixtyFourAccumulateAcrossSourceStacks() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(30, 64), source(31, 64)),
                List.of(slot(0, 70, 128, ShiftDragInsertionRules.moddedTotalLimit(128, 64))));

        assertEquals(List.of(128), counts(plan));
        assertEquals(List.of(6, 64), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
    }

    @Test
    void vanillaAndModdedStrategiesCoexistInConnectionOrder() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(40, 16), source(41, 16)),
                List.of(connectionSlot(0, 60,
                                ShiftDragInsertionRules.vanillaTotalLimit(64, 64)),
                        connectionSlot(1, 70,
                                ShiftDragInsertionRules.moddedTotalLimit(128, 64)),
                        connectionSlot(2, 0,
                                ShiftDragInsertionRules.vanillaTotalLimit(64, 64))));

        assertEquals(List.of(64, 98, 0), counts(plan));
        assertEquals(List.of(0, 0), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
        assertEquals(List.of(0, 1, 1), plan.transfers().stream()
                .map(ShiftDragPlanner.Transfer::section).toList());
    }

    @Test
    void completeDragUsesPriorVirtualStateForEachSource() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(50, 4), source(51, 4), source(52, 4), source(53, 4)),
                List.of(slot(0, 0, 0, 16)));

        assertEquals(List.of(16), counts(plan));
        assertEquals(List.of(0, 0, 0, 0), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
        assertEquals(List.of(4, 4, 4, 4), transferCounts(plan));
    }

    @Test
    void upgradeArmorAndCraftingSourcesShareLimitedPlayerInventoryCapacity() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(70, 4), source(71, 4), source(72, 4)),
                List.of(slot(0, 62, 64, 64), slot(1, 0, 0, 3)));

        assertEquals(List.of(64, 3), counts(plan));
        assertEquals(List.of(0, 3, 4), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
        assertEquals(List.of(2, 2, 1), transferCounts(plan));
    }

    @Test
    void specialExtractionLeavesIncompatiblePlayerStacksUntouched() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(source(80, 4)),
                List.of(slot(0, 0, 0, 64),
                        new ShiftDragPlanner.Destination(0, 1, -2, 5, Set.of(), Map.of(), Map.of())));

        assertEquals(List.of(4, 5), counts(plan));
        assertEquals(-2, plan.destinations().get(1).itemType());
        assertEquals(List.of(0), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
    }

    @Test
    void slotAndItemLimitsStayIndependentForEachStrategy() {
        assertEquals(16, ShiftDragInsertionRules.vanillaTotalLimit(64, 16));
        assertEquals(16, ShiftDragInsertionRules.moddedTotalLimit(64, 16));
        assertEquals(128, ShiftDragInsertionRules.moddedTotalLimit(128, 64));
        assertEquals(1, ShiftDragInsertionRules.moddedTotalLimit(128, 1));
    }

    @Test
    void originTargetsArePrioritizedAndTheirBudgetsAreConsumedSequentially() {
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(
                List.of(new ShiftDragPlanner.Source(60, 0, 4, false, 2),
                        new ShiftDragPlanner.Source(61, 0, 4, false, 2)),
                List.of(new ShiftDragPlanner.Destination(0, 0, 0, 0,
                                Set.of(0), Map.of(0, 0), Map.of(0, 4)),
                        new ShiftDragPlanner.Destination(1, 0, 0, 0,
                                Set.of(), Map.of(), Map.of(0, 4))));

        assertEquals(List.of(new ShiftDragPlanner.Transfer(60, 0, 0, 2, true),
                new ShiftDragPlanner.Transfer(60, 0, 0, 2, false),
                new ShiftDragPlanner.Transfer(61, 1, 0, 4, false)), plan.transfers());
        assertEquals(List.of(4, 4), counts(plan));
    }

    @Test
    void shiftDragFromPlayerPilesWithoutOriginAccumulatesIntoSingleSlot() {
        List<ShiftDragPlanner.Source> sources = List.of(
                new ShiftDragPlanner.Source(0, 0, 10, false, 0),
                new ShiftDragPlanner.Source(1, 0, 10, false, 0),
                new ShiftDragPlanner.Source(2, 0, 10, false, 0),
                new ShiftDragPlanner.Source(3, 0, 10, false, 0),
                new ShiftDragPlanner.Source(4, 0, 10, false, 0),
                new ShiftDragPlanner.Source(5, 0, 14, false, 0));
        ShiftDragPlanner.Plan plan = ShiftDragPlanner.plan(sources, List.of(slot(0, 0, 0, 64)));

        assertEquals(List.of(64), counts(plan));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), plan.sources().stream()
                .map(ShiftDragPlanner.SourceResult::remaining).toList());
    }

    private static ShiftDragPlanner.Source source(int slot, int count) {
        return new ShiftDragPlanner.Source(slot, 0, count, false, 0);
    }

    private static ShiftDragPlanner.Destination slot(int slot, int count, int itemLimit, int capacity) {
        return new ShiftDragPlanner.Destination(0, slot, count == 0 ? -1 : 0, count,
                Set.of(), Map.of(), Map.of(0, capacity));
    }

    private static ShiftDragPlanner.Destination connectionSlot(int section, int count, int capacity) {
        return new ShiftDragPlanner.Destination(section, 0, count == 0 ? -1 : 0, count,
                Set.of(), Map.of(), Map.of(0, capacity));
    }

    private static List<Integer> counts(ShiftDragPlanner.Plan plan) {
        return plan.destinations().stream().map(ShiftDragPlanner.DestinationResult::count).toList();
    }

    private static List<Integer> transferCounts(ShiftDragPlanner.Plan plan) {
        return plan.transfers().stream().map(ShiftDragPlanner.Transfer::count).toList();
    }
}
