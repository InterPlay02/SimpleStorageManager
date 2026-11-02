package net.interplay.simplestoragemanager.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualExtractionRulesTest {
    @Test
    void rightClickUsesRoundedHalfAndItemLimitForStackLimitSixtyFour() {
        assertAmounts(64, new int[]{1, 2, 3, 32, 63, 64, 65, 128},
                new int[]{1, 1, 2, 16, 32, 32, 32, 32});
    }

    @Test
    void rightClickUsesRoundedHalfAndItemLimitForStackLimitSixteen() {
        assertAmounts(16, new int[]{1, 2, 8, 16, 32, 48},
                new int[]{1, 1, 4, 8, 8, 8});
    }

    @Test
    void rightClickExtractsOneWhenItemLimitIsOne() {
        assertEquals(1, ManualExtractionRules.rightClickAmount(1, 1));
    }

    @Test
    void extractionConservesItemsBetweenInventoryAndCursor() {
        int inventoryBefore = 65;
        int cursorBefore = 0;
        int extracted = ManualExtractionRules.rightClickAmount(inventoryBefore, 64);
        int inventoryAfter = inventoryBefore - extracted;
        int cursorAfter = cursorBefore + extracted;

        assertEquals(inventoryBefore + cursorBefore, inventoryAfter + cursorAfter);
    }

    private static void assertAmounts(int itemLimit, int[] counts, int[] expected) {
        for (int i = 0; i < counts.length; i++) {
            assertEquals(expected[i], ManualExtractionRules.rightClickAmount(counts[i], itemLimit),
                    "count=" + counts[i] + ", itemLimit=" + itemLimit);
        }
    }
}
