package net.interplay.simplestoragemanager.logic;

import net.interplay.simplestoragemanager.client.gui.TerminalScreenLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

class StorageManagerLogicTest {
    @Test
    // Checks insertion priorities and stack limits for connected slots.
    void terminalInsertionRules() {
        assertTrue(TerminalInsertionRules.acceptsSlot(false, true, 0));
        assertFalse(TerminalInsertionRules.acceptsSlot(true, false, 0));
        assertFalse(TerminalInsertionRules.acceptsSlot(false, false, 0));
        assertTrue(TerminalInsertionRules.acceptsSlot(true, false, 1));
        assertFalse(TerminalInsertionRules.acceptsSlot(false, true, 1));
        assertEquals(108, TerminalInsertionRules.previewCapacity(20, 128));
        assertEquals(64, TerminalInsertionRules.extractionCount(128, 64, false));
        assertEquals(32, TerminalInsertionRules.extractionCount(128, 64, true));
        assertEquals(8, TerminalInsertionRules.extractionCount(50, 16, true));
        assertEquals(1, TerminalInsertionRules.extractionCount(50, 1, true));
        assertEquals(12, TerminalInsertionRules.extractionCount(12, 16, false));
        assertEquals(0, TerminalInsertionRules.extractionCount(12, 0, true));
        assertEquals(64, TerminalInsertionRules.shiftExtractionCount(128, 64));
        assertEquals(40, TerminalInsertionRules.shiftExtractionCount(40, 64));
        assertEquals(0, TerminalInsertionRules.shiftExtractionCount(128, 0));
        assertEquals(32, TerminalInsertionRules.confirmedTransferCount(64, 40, 32));
        assertEquals(0, TerminalInsertionRules.confirmedTransferCount(64, 64, 0));
    }

    @Test
    void searchIsCaseInsensitiveAndBlankMatchesEverything() {
        assertTrue(StorageSearch.matches("Diamond Chest", "diamond"));
        assertFalse(StorageSearch.matches("Diamond Chest", "iron"));
        assertTrue(StorageSearch.matches("anything", ""));
    }

    @Test
    void depositRulesPreferStorageAndRejectOutputs() {
        assertTrue(DepositRules.prefer(DepositRules.STORAGE, true));
        assertTrue(DepositRules.prefer(DepositRules.MACHINE_INPUT, true));
        assertFalse(DepositRules.prefer(DepositRules.MACHINE_OUTPUT, true));
        assertFalse(DepositRules.prefer(DepositRules.STORAGE, false));
    }

    @Test
    void scrollRangeUsesTheInsetContentHeights() {
        assertEquals(2, TerminalScreenLayout.scrollRange(TerminalScreenLayout.SMALL, 43));
        assertEquals(2, TerminalScreenLayout.scrollRange(TerminalScreenLayout.BIG, 155));
        assertEquals(114, TerminalScreenLayout.scrollRange(TerminalScreenLayout.SMALL, 155));
        assertEquals(111, TerminalScreenLayout.BIG.playerPortrait().top() - TerminalScreenLayout.SMALL.playerPortrait().top());
    }

    @Test
    void craftingAndClearButtonCoordinatesMatchBothGuiVariants() {
        assertEquals(8, TerminalScreenLayout.BUTTON_SIZE);
        assertEquals(10, TerminalScreenLayout.ALERT_ICON_SIZE);
        assertEquals(new TerminalScreenLayout.Point(103, 80), TerminalScreenLayout.SMALL.craftOrigin());
        assertEquals(new TerminalScreenLayout.Point(175, 98), TerminalScreenLayout.SMALL.result());
        assertEquals(new TerminalScreenLayout.Point(156, 70), TerminalScreenLayout.SMALL.cross());
        assertEquals(new TerminalScreenLayout.Point(103, 191), TerminalScreenLayout.BIG.craftOrigin());
        assertEquals(new TerminalScreenLayout.Point(175, 209), TerminalScreenLayout.BIG.result());
        assertEquals(new TerminalScreenLayout.Point(156, 182), TerminalScreenLayout.BIG.cross());
    }

    @Test
    void toggleUsesDimensionAndPositionAndDoesNotDuplicate() {
        ConnectionToggleLogic.ConnectionId connection = new ConnectionToggleLogic.ConnectionId("minecraft:overworld", 1, 2, 3);
        ConnectionToggleLogic.ConnectionId sameBlock = new ConnectionToggleLogic.ConnectionId("minecraft:overworld", 1, 2, 3);

        ConnectionToggleLogic.ToggleResult connected = ConnectionToggleLogic.toggle(List.of(), connection);
        assertTrue(connected.connected());
        ConnectionToggleLogic.ToggleResult disconnected = ConnectionToggleLogic.toggle(connected.connections(), sameBlock);
        assertFalse(disconnected.connected());
        assertTrue(disconnected.connections().isEmpty());
    }

    @Test
    void configuredGuiScaleUsesTheRequestedTextureTable() {
        assertFalse(TerminalScaleChoice.useBig(0));
        assertTrue(TerminalScaleChoice.useBig(1));
        assertTrue(TerminalScaleChoice.useBig(2));
        assertFalse(TerminalScaleChoice.useBig(3));
        assertFalse(TerminalScaleChoice.useBig(4));
    }

    @Test
    void genericMultiblockUsesAnInclusiveFiveBlockBoundary() {
        assertTrue(MultiblockBounds.fits(0, 0, 0, 4, 4, 4, 4, 4, 4));
        assertFalse(MultiblockBounds.fits(0, 0, 0, 4, 4, 4, 5, 4, 4));
    }

    @Test
    void basicTerminalRangeUsesInventoryBlockCenterAndSquaredDistance() {
        assertTrue(TerminalRange.isWithinBasicRange(500.5, 0.5, 0.5, 0, 0, 0));
        assertFalse(TerminalRange.isWithinBasicRange(500.5001, 0.5, 0.5, 0, 0, 0));
        assertTrue(TerminalRange.isWithinBasicRange(0.5, 500.5, 0.5, 0, 0, 0));
        assertFalse(TerminalRange.isWithinBasicRange(0.5, 500.5, 500.5, 0, 0, 0));
    }

    @Test
    void upgradePlacementAndRemovalRespectSlotAndDependencyRules() {
        assertTrue(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.BOTTOM_SLOT, UpgradeSlotRules.LEVEL_1));
        assertTrue(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.MIDDLE_SLOT, UpgradeSlotRules.LEVEL_2));
        assertTrue(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_3));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.MIDDLE_SLOT, UpgradeSlotRules.LEVEL_1));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.BOTTOM_SLOT, UpgradeSlotRules.LEVEL_2));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_2));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.BOTTOM_SLOT, UpgradeSlotRules.LEVEL_3));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.MIDDLE_SLOT, UpgradeSlotRules.LEVEL_3));
        assertFalse(UpgradeSlotRules.acceptsItem(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_1));
        assertFalse(UpgradeSlotRules.canPlace(UpgradeSlotRules.MIDDLE_SLOT, UpgradeSlotRules.LEVEL_2, false, false));
        assertTrue(UpgradeSlotRules.canPlace(UpgradeSlotRules.MIDDLE_SLOT, UpgradeSlotRules.LEVEL_2, true, false));
        assertFalse(UpgradeSlotRules.canPlace(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_3, true, false));
        assertTrue(UpgradeSlotRules.canPlace(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_3, true, true));
        assertFalse(UpgradeSlotRules.canPlace(UpgradeSlotRules.TOP_SLOT, UpgradeSlotRules.LEVEL_3, false, true));

        assertFalse(UpgradeSlotRules.canRemove(UpgradeSlotRules.BOTTOM_SLOT, true, false));
        assertFalse(UpgradeSlotRules.canRemove(UpgradeSlotRules.BOTTOM_SLOT, false, true));
        assertTrue(UpgradeSlotRules.canRemove(UpgradeSlotRules.BOTTOM_SLOT, false, false));
        assertFalse(UpgradeSlotRules.canRemove(UpgradeSlotRules.MIDDLE_SLOT, false, true));
        assertTrue(UpgradeSlotRules.canRemove(UpgradeSlotRules.MIDDLE_SLOT, false, false));
        assertTrue(UpgradeSlotRules.canRemove(UpgradeSlotRules.TOP_SLOT, true, true));
    }

    @Test
    void onlyTheHighestInstalledUpgradeIsEffective() {
        assertEquals(0, UpgradeAccessRules.effectiveLevel(false, false, false, false));
        assertEquals(1, UpgradeAccessRules.effectiveLevel(true, false, false, false));
        assertEquals(1, UpgradeAccessRules.effectiveLevel(true, true, false, false));
        assertEquals(0, UpgradeAccessRules.effectiveLevel(false, true, false, false));
        assertEquals(2, UpgradeAccessRules.effectiveLevel(true, true, true, false));
        assertEquals(3, UpgradeAccessRules.effectiveLevel(true, true, false, true));
        assertEquals(3, UpgradeAccessRules.effectiveLevel(false, false, false, true));
    }

    @Test
    void onlyTheTerminalOwnerCanUseConnectedInventories() {
        UUID owner = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        assertTrue(TerminalOwnershipRules.canConnectInventory(null, owner));
        assertTrue(TerminalOwnershipRules.canConnectInventory(owner, owner));
        assertFalse(TerminalOwnershipRules.canConnectInventory(owner, otherPlayer));
        assertTrue(TerminalOwnershipRules.canUseConnectedInventories(owner, owner));
        assertFalse(TerminalOwnershipRules.canUseConnectedInventories(owner, otherPlayer));
        assertFalse(TerminalOwnershipRules.canUseConnectedInventories(null, owner));
    }

    @Test
    void upgradeLevelsApplyTheirOwnDistanceAndDimensionRules() {
        assertTrue(UpgradeAccessRules.allowsDistanceLimit(0));
        assertFalse(UpgradeAccessRules.allowsDistanceLimit(1));
        assertFalse(UpgradeAccessRules.allowsDistanceLimit(2));
        assertFalse(UpgradeAccessRules.allowsDistanceLimit(3));
        assertTrue(UpgradeAccessRules.isWithinRange(0, 100, 0.5, 0.5, 0, 0, 0));
        assertFalse(UpgradeAccessRules.isWithinRange(0, 501, 0.5, 0.5, 0, 0, 0));
        assertTrue(UpgradeAccessRules.isWithinRange(1, 10000, 0.5, 0.5, 0, 0, 0));
        assertTrue(UpgradeAccessRules.isWithinRange(2, 10000, 0.5, 0.5, 0, 0, 0));
        assertTrue(UpgradeAccessRules.isWithinRange(3, 10000, 0.5, 0.5, 0, 0, 0));

        assertTrue(UpgradeAccessRules.allowsDimension(0, "minecraft:overworld", "minecraft:overworld", java.util.Set.of()));
        assertFalse(UpgradeAccessRules.allowsDimension(0, "minecraft:overworld", "minecraft:the_nether", java.util.Set.of()));
        assertTrue(UpgradeAccessRules.allowsDimension(1, "minecraft:overworld", "minecraft:overworld", java.util.Set.of()));
        assertFalse(UpgradeAccessRules.allowsDimension(1, "minecraft:overworld", "minecraft:the_nether", java.util.Set.of()));
        assertTrue(UpgradeAccessRules.allowsDimension(2, "minecraft:overworld", "mod:moon", java.util.Set.of("mod:moon")));
        assertFalse(UpgradeAccessRules.allowsDimension(2, "minecraft:overworld", "minecraft:overworld", java.util.Set.of("mod:moon")));
        assertTrue(UpgradeAccessRules.allowsDimension(3, "minecraft:overworld", "mod:moon", java.util.Set.of()));
    }

    @Test
    void dimensionSelectionIsUniqueRegisteredAndLimitedToFive() {
        java.util.Set<String> registered = java.util.Set.of(
                "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
                "mod:one", "mod:two", "mod:three");
        assertTrue(UpgradeAccessRules.isValidSelection(List.of(), registered));
        assertTrue(UpgradeAccessRules.isValidSelection(List.of(
                "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "mod:one", "mod:two"), registered));
        assertFalse(UpgradeAccessRules.isValidSelection(List.of(
                "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "mod:one", "mod:two", "mod:three"), registered));
        assertFalse(UpgradeAccessRules.isValidSelection(List.of("mod:one", "mod:one"), registered));
        assertFalse(UpgradeAccessRules.isValidSelection(List.of("mod:missing"), registered));
    }
}
