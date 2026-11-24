package net.interplay.simplestoragemanager.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Plans one ordered Shift-drag against a virtual set of destination slots. */
public final class ShiftDragPlanner {
    private ShiftDragPlanner() {
    }

    public static Plan plan(List<Source> sources, List<Destination> destinations) {
        List<MutableDestination> state = destinations.stream().map(MutableDestination::new).toList();
        List<SourceResult> sourceResults = new ArrayList<>(sources.size());
        List<Transfer> transfers = new ArrayList<>();
        Map<Integer, Integer> originBudgets = new java.util.HashMap<>();

        for (Source source : sources) {
            if (source.originBudget() > 0) {
                originBudgets.putIfAbsent(source.itemType(), source.originBudget());
            }
            int remaining = source.count();
            int originBudget = Math.min(remaining, originBudgets.getOrDefault(source.itemType(), 0));
            if (originBudget > 0) {
                remaining -= insert(source, state, transfers, remaining, originBudget, true, false);
            }
            boolean emptySlotsOnly = source.emptySlotsOnly();
            remaining -= insert(source, state, transfers, remaining, remaining,
                    false, emptySlotsOnly);
            int inserted = source.count() - remaining;
            if (source.originBudget() > 0) {
                originBudgets.compute(source.itemType(), (ignored, current) ->
                        Math.max(0, (current == null ? 0 : current) - inserted));
            }
            sourceResults.add(new SourceResult(source.menuSlot(), remaining));
        }
        List<DestinationResult> finalDestinations = state.stream()
                .map(MutableDestination::result).toList();
        return new Plan(sourceResults, finalDestinations, transfers);
    }

    private static int insert(Source source, List<MutableDestination> destinations,
                              List<Transfer> transfers, int movingCount, int maximum,
                              boolean originsOnly, boolean emptySlotsOnly) {
        int remaining = Math.min(movingCount, maximum);
        int inserted = 0;
        int firstPass = emptySlotsOnly ? 1 : 0;
        List<MutableDestination> ordered = destinations;
        if (originsOnly) {
            ordered = new ArrayList<>(destinations);
            ordered.sort(java.util.Comparator.comparingInt(destination ->
                    destination.destination.originOrder().getOrDefault(source.itemType(), Integer.MAX_VALUE)));
        }
        for (int pass = firstPass; pass < TerminalInsertionRules.PASS_COUNT && remaining > 0; pass++) {
            for (MutableDestination destination : ordered) {
                if (remaining <= 0) break;
                if (originsOnly && !destination.destination.originTypes().contains(source.itemType())) continue;
                if (pass == 0 && (destination.count == 0
                        || destination.itemType != source.itemType())) continue;
                if (pass == 1 && destination.count != 0) continue;
                int maxCount = destination.destination.maxCounts().getOrDefault(source.itemType(), 0);
                int accepted = Math.min(remaining, Math.max(0, maxCount - destination.count));
                if (accepted <= 0) continue;
                if (destination.count == 0) destination.itemType = source.itemType();
                destination.count += accepted;
                remaining -= accepted;
                inserted += accepted;
                transfers.add(new Transfer(source.menuSlot(), destination.destination.section(),
                        destination.destination.slot(), accepted, originsOnly));
            }
        }
        return inserted;
    }

    public record Source(int menuSlot, int itemType, int count, boolean emptySlotsOnly, int originBudget) {
        public Source {
            count = Math.max(0, count);
            originBudget = Math.max(0, originBudget);
        }
    }

    public record Destination(int section, int slot, int itemType, int count,
                              java.util.Set<Integer> originTypes, Map<Integer, Integer> originOrder,
                              Map<Integer, Integer> maxCounts) {
        public Destination {
            count = Math.max(0, count);
            originTypes = java.util.Set.copyOf(originTypes);
            originOrder = Map.copyOf(originOrder);
            maxCounts = Map.copyOf(maxCounts);
            if (count == 0) itemType = -1;
        }
    }

    public record SourceResult(int menuSlot, int remaining) { }
    public record DestinationResult(int section, int slot, int itemType, int count) { }
    public record Transfer(int sourceMenuSlot, int section, int slot, int count, boolean origin) { }
    public record Plan(List<SourceResult> sources, List<DestinationResult> destinations,
                       List<Transfer> transfers) {
        public Plan {
            sources = List.copyOf(sources);
            destinations = List.copyOf(destinations);
            transfers = List.copyOf(transfers);
        }
    }

    private static final class MutableDestination {
        private final Destination destination;
        private int itemType;
        private int count;

        private MutableDestination(Destination destination) {
            this.destination = destination;
            itemType = destination.itemType();
            count = destination.count();
        }

        private DestinationResult result() {
            return new DestinationResult(destination.section(), destination.slot(), itemType, count);
        }
    }
}
