package net.interplay.simplestoragemanager.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.interplay.simplestoragemanager.access.AccessRule;
import net.interplay.simplestoragemanager.access.ConnectionStatus;
import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.interplay.simplestoragemanager.access.MultiblockDiscovery;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.item.inventory.StorageManagerInventoryCapability;
import net.interplay.simplestoragemanager.logic.StorageSearch;
import net.interplay.simplestoragemanager.logic.TerminalInsertionRules;
import net.interplay.simplestoragemanager.logic.UpgradeSlotRules;
import net.interplay.simplestoragemanager.logic.UpgradeAccessRules;
import net.interplay.simplestoragemanager.logic.TerminalOwnershipRules;
import net.interplay.simplestoragemanager.network.TerminalNoticeDismissPayload;
import net.interplay.simplestoragemanager.network.TerminalStorageClickPayload;
import net.interplay.simplestoragemanager.network.TerminalConnectionStatusPayload;
import net.interplay.simplestoragemanager.network.TerminalLockTogglePayload;
import net.interplay.simplestoragemanager.network.TerminalViewPayload;
import net.interplay.simplestoragemanager.network.TerminalViewRequestPayload;
import net.interplay.simplestoragemanager.world.TerminalSavedData;
import net.interplay.simplestoragemanager.world.TerminalOriginMemory;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.client.gui.TerminalScreenLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Menu for the terminal. Remote inventories are data snapshots, never menu slots. */
public class StorageManagerGUIMenu extends AbstractContainerMenu implements SimplestoragemanagerModMenus.MenuAccessor {
    private static final int SECTION_LABEL_HEIGHT = 12;
    private static final int SECTION_GAP = 4;
    private static final int SLOT_SIZE = 18;

    public final Map<String, Object> menuState = new HashMap<>();
    public final Level world;
    public final Player entity;
    public final InteractionHand hand;
    public final int upgradeStart;
    public final int armorStart;
    public final int offhandIndex;
    public final int mainInventoryStart;
    public final int hotbarStart;
    public final int craftingResultIndex;
    public final int craftingGridStart;

    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private final Container visualContainer = new SimpleContainer(1);
    private final CraftingContainer craftingGrid;
    private final ResultContainer craftingResult = new ResultContainer();
    private UUID terminalId;
    private String search = "";
    private int scrollOffset;
    private boolean big;
    private boolean closed;
    private int passiveCheckTicks;
    private long lastOperationSequence;
    private long appliedOperationSequence;
    private List<ServerSection> serverSections = List.of();
    private Map<TerminalConnection, ConnectionStatus> connectionStatuses = new HashMap<>();
    private TerminalViewPayload view = new TerminalViewPayload(false, 0, 0, List.of(), List.of(), List.of());

    public StorageManagerGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, readHand(extraData));
    }

    public StorageManagerGUIMenu(int id, Inventory inventory, InteractionHand hand) {
        super(SimplestoragemanagerModMenus.STORAGE_MANAGER_GUI.get(), id);
        this.entity = inventory.player;
        this.world = entity.level();
        this.hand = hand;
        if (!world.isClientSide) {
            terminalId = terminalId();
            refreshServerView();
        }

        upgradeStart = slots.size();
        for (int i = 0; i < 3; i++) {
            Slot slot = new TerminalUpgradeSlot(this, i);
            addSlot(slot);
            customSlots.put(slot.index, slot);
        }

        armorStart = slots.size();
        addGuiSlot(new PlayerArmorGuiSlot(this, inventory, 39, 0, 0, EquipmentSlot.HEAD));
        addGuiSlot(new PlayerArmorGuiSlot(this, inventory, 38, 0, 0, EquipmentSlot.CHEST));
        addGuiSlot(new PlayerArmorGuiSlot(this, inventory, 37, 0, 0, EquipmentSlot.LEGS));
        addGuiSlot(new PlayerArmorGuiSlot(this, inventory, 36, 0, 0, EquipmentSlot.FEET));
        offhandIndex = slots.size();
        addGuiSlot(new GuiSlot(this, inventory, 40, 0, 0));

        mainInventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addGuiSlot(new GuiSlot(this, inventory, column + (row + 1) * 9, 0, 0));
            }
        }
        hotbarStart = slots.size();
        for (int column = 0; column < 9; column++) {
            addGuiSlot(new GuiSlot(this, inventory, column, 0, 0));
        }

        craftingGrid = new TransientCraftingContainer(this, 3, 3);
        craftingResultIndex = slots.size();
        Slot resultSlot = new ResultSlot(entity, craftingGrid, craftingResult, 0, 0, 0) {
            @Override
            public boolean isActive() {
                return !world.isClientSide;
            }
        };
        addSlot(resultSlot);
        customSlots.put(resultSlot.index, resultSlot);
        craftingGridStart = slots.size();
        for (int i = 0; i < 9; i++) {
            addGuiSlot(new GuiSlot(this, craftingGrid, i, 0, 0));
        }
    }

    private void addGuiSlot(Slot slot) {
        addSlot(slot);
        customSlots.put(slot.index, slot);
    }

    private static InteractionHand readHand(FriendlyByteBuf buffer) {
        return buffer != null && buffer.readableBytes() > 0 && buffer.readByte() == 1
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public ItemStack terminalStack() {
        ItemStack stack = entity.getItemInHand(hand);
        return stack.is(SimplestoragemanagerModItems.STORAGE_MANAGER.get()) ? stack : ItemStack.EMPTY;
    }

    public UUID terminalId() {
        ItemStack stack = terminalStack();
        if (stack.isEmpty()) return null;
        UUID id = stack.get(TerminalDataComponents.TERMINAL_ID.get());
        if (id == null && !world.isClientSide) {
            id = UUID.randomUUID();
            stack.set(TerminalDataComponents.TERMINAL_ID.get(), id);
            entity.getInventory().setChanged();
        }
        terminalId = id;
        return id;
    }

    public boolean isTerminalSlot(int slotId) {
        if (hand == InteractionHand.OFF_HAND) return slotId == offhandIndex;
        return slotId == hotbarStart + entity.getInventory().selected;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isTerminalSlot(slotId)) return;
        if (slotId >= armorStart && slotId < armorStart + 4) {
            ItemStack previousArmor = slots.get(slotId).getItem().copy();
            super.clicked(slotId, button, clickType, player);
            ItemStack equipped = slots.get(slotId).getItem();
            if (!world.isClientSide && !equipped.isEmpty() && !ItemStack.matches(previousArmor, equipped)) {
                playArmorEquipSound(player, equipped);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void playArmorEquipSound(Player player, ItemStack equipped) {
        Equipable equipable = Equipable.get(equipped);
        if (equipable != null) {
            player.playNotifySound(equipable.getEquipSound().value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public void clearCraftingGrid(ServerPlayer player) {
        if (world.isClientSide || !stillValid(player)) return;
        returnCraftingItems(player, false);
        broadcastChanges();
        broadcastFullState();
        refreshServerView();
    }

    public void toggleInventoryLock(ServerPlayer player) {
        if (world.isClientSide || !stillValid(player) || !canUseConnectedInventories(player, true)) return;
        UUID id = terminalId();
        if (id == null) return;
        TerminalSavedData data = TerminalSavedData.get(player.serverLevel());
        boolean locked = !data.isLocked(id);
        data.setLocked(id, locked);
        if (!locked) TerminalOriginMemory.clearTerminal(player, id);
        refreshServerView();
    }

    public TerminalViewPayload view() { return view; }

    public void applyView(TerminalViewPayload payload) {
        if (world.isClientSide && payload.operationSequence() < appliedOperationSequence) return;
        if (world.isClientSide) appliedOperationSequence = payload.operationSequence();
        view = payload;
        if (world.isClientSide) {
            big = payload.big();
            scrollOffset = payload.scrollOffset();
        }
    }

    public void applyConnectionStatus(int sectionIndex, int status, Component label) {
        if (sectionIndex < 0 || sectionIndex >= view.sections().size()) return;
        List<TerminalViewPayload.Section> sections = new ArrayList<>(view.sections());
        TerminalViewPayload.Section current = sections.get(sectionIndex);
        sections.set(sectionIndex, new TerminalViewPayload.Section(label, current.coordinate(), status,
                current.slotCount()));
        List<TerminalViewPayload.VisibleSlot> visibleSlots = view.visibleSlots().stream()
                .filter(slot -> slot.section() != sectionIndex)
                .toList();
        view = new TerminalViewPayload(view.big(), view.scrollOffset(), view.maxScroll(),
                sections, visibleSlots, view.notices(), view.locked(), view.originTargets(),
                view.operationSequence(), view.playerSlots(), view.canMutateInventories());
    }

    public void handleViewRequest(TerminalViewRequestPayload request) {
        if (world.isClientSide) return;
        search = request.search() == null ? "" : request.search().substring(0, Math.min(256, request.search().length()));
        scrollOffset = Math.max(0, request.scrollOffset());
        big = request.big();
        refreshServerView();
    }

    public void refreshServerView() {
        refreshServerView(false);
    }

    private void refreshServerView(boolean operationResult) {
        if (world.isClientSide || !(entity instanceof ServerPlayer player)) return;
        terminalId = terminalId();
        if (terminalId == null) return;
        TerminalSavedData data = TerminalSavedData.get(player.serverLevel());
        List<TerminalConnection> connections = validateConnections(data, terminalId);
        List<ServerSection> sections = new ArrayList<>();
        Map<TerminalConnection, ConnectionStatus> statuses = new HashMap<>();
        if (search.isBlank()) {
            for (TerminalConnection connection : connections) {
                InventoryAdapter.Resolution resolution = resolve(connection);
                statuses.put(connection, resolution.status());
                InventoryAdapter adapter = resolution.adapter();
                int slotCount = adapter == null ? connection.lastSlotCount() : adapter.getSlots();
                Component label = adapter == null
                        ? Component.literal(ComponentText.statusLabel(resolution.status())) : adapter.displayName();
                sections.add(new ServerSection(connection, resolution.status(), label, slotCount,
                        snapshotSlots(adapter), snapshotSlotLimits(adapter), List.of()));
            }
        } else {
            List<SearchEntry> results = new ArrayList<>();
            for (TerminalConnection connection : connections) {
                InventoryAdapter.Resolution resolution = resolve(connection);
                statuses.put(connection, resolution.status());
                if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) continue;
                for (int slot = 0; slot < resolution.adapter().getSlots(); slot++) {
                    ItemStack stack = resolution.adapter().handler().getStackInSlot(slot).copy();
                    if (!stack.isEmpty() && StorageSearch.matches(stack.getHoverName().getString(), search)) {
                        results.add(new SearchEntry(connection, slot, stack,
                                resolution.adapter().handler().getSlotLimit(slot)));
                    }
                }
            }
            sections.add(new ServerSection(null, ConnectionStatus.AVAILABLE,
                    Component.translatable("screen.simplestoragemanager.search_results"),
                    results.size(), List.of(), List.of(), results));
        }
        serverSections = List.copyOf(sections);
        connectionStatuses = new HashMap<>(statuses);
        sendView(player, operationResult);
    }

    public void tickConnectionAvailability() {
        if (closed || world.isClientSide || !(entity instanceof ServerPlayer player)) return;
        if (++passiveCheckTicks < 20) return;
        passiveCheckTicks = 0;
        UUID id = terminalId();
        if (id == null) return;
        TerminalSavedData data = TerminalSavedData.get(player.serverLevel());
        List<TerminalConnection> original = data.connections(id);
        List<TerminalConnection> connections = validateConnections(data, id);
        if (!original.equals(connections)) {
            refreshServerView();
            return;
        }
        if (!connectionStatuses.keySet().equals(new java.util.HashSet<>(connections))) {
            refreshServerView();
            return;
        }
        for (TerminalConnection connection : connections) {
            verifyConnection(connection, false);
        }
    }

    private List<TerminalConnection> validateConnections(TerminalSavedData data, UUID id) {
        List<TerminalConnection> original = data.connections(id);
        List<TerminalConnection> updated = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TerminalConnection connection : original) {
            if (connection.brokenConnectionExpired(now)) {
                continue;
            }
            ConnectionStatus availability = basicAvailabilityStatus(connection);
            if (availability != null) {
                updated.add(connection);
                continue;
            }
            if (isReplacement(connection)) {
                data.addNotice(id, connection.dimension(), firstBrokenOrAnchor(connection),
                        updated.size(), connection.lastSlotCount());
                continue;
            }
            TerminalConnection current = connection;
            ServerLevel target = targetLevel(connection);
            if (!current.broken() && target != null) {
                for (BlockPos member : connection.members()) {
                    if (InventoryAdapter.isChunkLoaded(target, member)
                            && !TerminalSavedData.matchesCurrentBlock(target, connection, member)) {
                        current = current.withoutMember(member);
                        if (current == null) break;
                    }
                }
                if (current == null) {
                    data.addNotice(id, connection.dimension(), connection.pos(),
                            updated.size(), connection.lastSlotCount());
                    continue;
                }
                if (!current.equals(connection)) {
                    for (TerminalConnection rebuilt : MultiblockDiscovery.rediscover(target, current)) {
                        InventoryAdapter.Resolution resolution = resolve(rebuilt);
                        if (resolution.status() == ConnectionStatus.AVAILABLE && resolution.adapter() != null) {
                            rebuilt = rebuilt.withSlotCount(resolution.adapter().getSlots());
                        }
                        updated.add(rebuilt);
                    }
                    continue;
                }
            }
            if (target != null) {
                for (net.minecraft.core.BlockPos member : connection.members()) {
                    if (InventoryAdapter.isChunkLoaded(target, member)
                            && target.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                            member, connection.side()) == null) {
                        current = current.withUnconfirmedMissingMember(member);
                    }
                }
            }
            if (!current.broken() && target != null) {
                List<TerminalConnection> recalculated = MultiblockDiscovery.rediscover(target, current);
                for (TerminalConnection rebuilt : recalculated) {
                    InventoryAdapter.Resolution resolution = resolve(rebuilt);
                    if (resolution.status() == ConnectionStatus.AVAILABLE && resolution.adapter() != null) {
                        rebuilt = rebuilt.withSlotCount(resolution.adapter().getSlots());
                    }
                    updated.add(rebuilt);
                }
            } else {
                InventoryAdapter.Resolution resolution = resolve(current);
                if (resolution.status() == ConnectionStatus.AVAILABLE && resolution.adapter() != null) {
                    current = current.withSlotCount(resolution.adapter().getSlots());
                }
                updated.add(current);
            }
        }
        if (!original.equals(updated)) data.putConnections(id, updated);
        return updated;
    }

    private BlockPos firstBrokenOrAnchor(TerminalConnection connection) {
        return connection.brokenMembers().stream().findFirst().orElse(connection.pos());
    }

    private boolean isReplacement(TerminalConnection connection) {
        if (!connection.broken()) return false;
        ServerLevel target = targetLevel(connection);
        if (target == null) return false;
        for (net.minecraft.core.BlockPos broken : connection.brokenMembers()) {
            if (InventoryAdapter.isChunkLoaded(target, broken)
                    && target.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    broken, connection.side()) != null) {
                return true;
            }
        }
        return false;
    }

    private ServerLevel targetLevel(TerminalConnection connection) {
        return entity.getServer() == null ? null : entity.getServer().getLevel(connection.dimension());
    }

    private InventoryAdapter.Resolution resolve(TerminalConnection connection) {
        ConnectionStatus availability = basicAvailabilityStatus(connection);
        if (availability != null) {
            return new InventoryAdapter.Resolution(availability, null);
        }
        ServerLevel target = targetLevel(connection);
        if (target == null) return new InventoryAdapter.Resolution(ConnectionStatus.DIMENSION_MISSING, null);
        return InventoryAdapter.resolve(target, connection, entity, AccessRule.ALWAYS);
    }

    private ConnectionStatus basicAvailabilityStatus(TerminalConnection connection) {
        int level = getEffectiveUpgradeLevel();
        Set<String> selected = level == UpgradeSlotRules.LEVEL_2
                ? new java.util.HashSet<>(selectedDimensions()) : Set.of();
        if (!UpgradeAccessRules.allowsDimension(level, entity.level().dimension().location().toString(),
                connection.dimension().location().toString(), selected)) {
            return ConnectionStatus.DIMENSION_MISSING;
        }
        if (!UpgradeAccessRules.isWithinRange(level, entity.getX(), entity.getY(), entity.getZ(),
                connection.pos().getX(), connection.pos().getY(), connection.pos().getZ())) {
            return ConnectionStatus.BASIC_UNAVAILABLE;
        }
        ServerLevel target = targetLevel(connection);
        if (target == null) return ConnectionStatus.DIMENSION_MISSING;
        if (!InventoryAdapter.isChunkLoaded(target, connection.pos())) {
            return ConnectionStatus.CHUNK_UNLOADED;
        }
        return null;
    }

    private InventoryAdapter.Resolution verifyConnection(TerminalConnection connection, boolean active) {
        if (active) passiveCheckTicks = 0;
        InventoryAdapter.Resolution resolution = resolve(connection);
        ConnectionStatus previous = connectionStatuses.get(connection);
        if (previous != resolution.status()) {
            connectionStatuses.put(connection, resolution.status());
            updateServerSection(connection, resolution);
            sendConnectionStatusUpdate(connection, resolution);
        }
        return resolution;
    }

    private void updateServerSection(TerminalConnection connection, InventoryAdapter.Resolution resolution) {
        if (!search.isBlank()) return;
        List<ServerSection> updated = new ArrayList<>(serverSections);
        for (int section = 0; section < updated.size(); section++) {
            ServerSection current = updated.get(section);
            if (connection.equals(current.connection())) {
                int slotCount = resolution.adapter() == null
                        ? current.slotCount() : resolution.adapter().getSlots();
                Component label = resolution.adapter() == null
                        ? Component.literal(ComponentText.statusLabel(resolution.status()))
                        : resolution.adapter().displayName();
                updated.set(section, new ServerSection(connection, resolution.status(), label, slotCount,
                        snapshotSlots(resolution.adapter()), snapshotSlotLimits(resolution.adapter()), List.of()));
                break;
            }
        }
        serverSections = List.copyOf(updated);
    }

    private List<ItemStack> snapshotSlots(InventoryAdapter adapter) {
        if (adapter == null) return List.of();
        List<ItemStack> contents = new ArrayList<>(adapter.getSlots());
        for (int slot = 0; slot < adapter.getSlots(); slot++) {
            contents.add(adapter.handler().getStackInSlot(slot).copy());
        }
        return List.copyOf(contents);
    }

    private List<Integer> snapshotSlotLimits(InventoryAdapter adapter) {
        if (adapter == null) return List.of();
        List<Integer> limits = new ArrayList<>(adapter.getSlots());
        for (int slot = 0; slot < adapter.getSlots(); slot++) {
            limits.add(Math.max(0, adapter.handler().getSlotLimit(slot)));
        }
        return List.copyOf(limits);
    }

    private void sendConnectionStatusUpdate(TerminalConnection connection,
                                            InventoryAdapter.Resolution resolution) {
        if (!(entity instanceof ServerPlayer player)) return;
        int sectionIndex = -1;
        if (search.isBlank()) {
            for (int section = 0; section < serverSections.size(); section++) {
                if (connection.equals(serverSections.get(section).connection())) {
                    sectionIndex = section;
                    break;
                }
            }
        }
        Component label = resolution.adapter() == null
                ? Component.literal(ComponentText.statusLabel(resolution.status()))
                : resolution.adapter().displayName();
        List<TerminalViewPayload.VisibleSlot> visibleSlots = sectionIndex < 0
                ? List.of() : visibleSlotsForSection(sectionIndex);
        boolean refreshView = !search.isBlank();
        player.connection.send(new TerminalConnectionStatusPayload(sectionIndex,
                resolution.status().ordinal(), label, visibleSlots, refreshView));
    }

    private List<TerminalViewPayload.VisibleSlot> visibleSlotsForSection(int targetSection) {
        if (targetSection < 0 || targetSection >= serverSections.size()) return List.of();
        ServerSection section = serverSections.get(targetSection);
        List<TerminalViewPayload.VisibleSlot> visible = new ArrayList<>(section.slotCount());
        for (int slot = 0; slot < section.slotCount(); slot++) {
            ItemStack stack = section.stackAt(slot);
            visible.add(new TerminalViewPayload.VisibleSlot(targetSection, slot, stack.copy(),
                    section.slotLimitAt(slot, stack)));
        }
        return visible;
    }

    public int getEffectiveUpgradeLevel() {
        ItemStack stack = terminalStack();
        if (stack.isEmpty()) return 0;
        StorageManagerInventoryCapability upgrades = new StorageManagerInventoryCapability(stack);
        boolean level1 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.BOTTOM_SLOT)) == UpgradeSlotRules.LEVEL_1;
        boolean level2 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.MIDDLE_SLOT)) == UpgradeSlotRules.LEVEL_2;
        boolean level3 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.TOP_SLOT)) == UpgradeSlotRules.LEVEL_3;
        return UpgradeAccessRules.effectiveLevel(level1, level2, level2 && !selectedDimensions().isEmpty(), level3);
    }

    private boolean canUseConnectedInventories(ServerPlayer player, boolean notify) {
        UUID id = terminalId();
        UUID owner = id == null ? null : TerminalSavedData.get(player.serverLevel()).owner(id);
        if (TerminalOwnershipRules.canUseConnectedInventories(owner, player.getUUID())) return true;
        if (notify) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner"));
        }
        return false;
    }

    private boolean inventoryLockEnabled(ServerPlayer player) {
        UUID id = terminalId();
        return id != null && TerminalSavedData.get(player.serverLevel()).isLocked(id);
    }

    private void recordExtraction(ServerPlayer player, TerminalConnection connection, int slot, ItemStack extracted) {
        if (inventoryLockEnabled(player)) {
            TerminalOriginMemory.record(player, terminalId(), connection, slot, extracted);
        }
    }

    // Locked returns merge into matching origin stacks before using empty slots.
    private int insertIntoRememberedOrigins(ItemStack stack, int maximum) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty() || maximum <= 0
                || !inventoryLockEnabled(player)) return 0;
        UUID id = terminalId();
        if (id == null) return 0;
        List<TerminalConnection> currentConnections = TerminalSavedData.get(player.serverLevel()).connections(id);
        int inserted = 0;
        for (TerminalOriginMemory.OriginRecord record : TerminalOriginMemory.records(player, id)) {
            if (stack.isEmpty() || inserted >= maximum) break;
            if (!ItemStack.isSameItemSameComponents(record.item(), stack)) continue;
            TerminalConnection current = currentConnections.stream()
                    .filter(connection -> sameOriginConnection(connection, record.connection()))
                    .findFirst().orElse(null);
            if (current == null) {
                TerminalOriginMemory.discard(player, id, record.id());
                return inserted;
            }
            InventoryAdapter.Resolution resolution = verifyConnection(current, true);
            if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) {
                TerminalOriginMemory.discard(player, id, record.id());
                return inserted;
            }
            int requested = Math.min(Math.min(stack.getCount(), maximum - inserted), record.remaining());
            if (requested <= 0) continue;
            int moved = insertIntoOriginInventory(resolution.adapter().handler(), stack, requested);
            if (moved > 0) {
                stack.shrink(moved);
                inserted += moved;
                TerminalOriginMemory.consume(player, id, record.id(), moved);
            }
            if (moved < requested) {
                TerminalOriginMemory.discard(player, id, record.id());
                return inserted;
            }
        }
        return inserted;
    }

    private boolean sameOriginConnection(TerminalConnection current, TerminalConnection origin) {
        return current != null && origin != null
                && current.dimension().equals(origin.dimension()) && current.pos().equals(origin.pos())
                && java.util.Objects.equals(current.side(), origin.side());
    }

    private int insertIntoOriginInventory(IItemHandler handler, ItemStack stack, int requested) {
        if (stack.isEmpty() || requested <= 0) return 0;
        int inserted = 0;
        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && inserted < requested; pass++) {
            for (int slot = 0; slot < handler.getSlots() && inserted < requested; slot++) {
                ItemStack existing = handler.getStackInSlot(slot);
                if (!acceptsInsertionSlot(existing, stack, pass)) continue;
                if (!handler.isItemValid(slot, stack)) continue;
                int offeredCount = Math.min(requested - inserted,
                        insertionCapacity(stack, existing, handler.getSlotLimit(slot)));
                if (offeredCount <= 0) continue;
                ItemStack offered = stack.copyWithCount(offeredCount);
                ItemStack simulated = handler.insertItem(slot, offered, true);
                if (!simulated.isEmpty() && !ItemStack.isSameItemSameComponents(stack, simulated)) continue;
                int accepted = Math.min(offeredCount, offeredCount - simulated.getCount());
                if (accepted <= 0) continue;
                ItemStack remainder = handler.insertItem(slot, offered.copyWithCount(accepted), false);
                if (!remainder.isEmpty() && !ItemStack.isSameItemSameComponents(stack, remainder)) continue;
                inserted += Math.max(0, Math.min(accepted, accepted - remainder.getCount()));
            }
        }
        return inserted;
    }

    private List<String> selectedDimensions() {
        ItemStack terminal = terminalStack();
        if (terminal.isEmpty()) return List.of();
        ItemStack level2 = new StorageManagerInventoryCapability(terminal)
                .getStackInSlot(UpgradeSlotRules.MIDDLE_SLOT);
        return List.copyOf(level2.getOrDefault(TerminalDataComponents.DIMENSION_SELECTION.get(), List.of()));
    }

    int upgradeLevel(ItemStack stack) {
        if (stack.is(SimplestoragemanagerModItems.INFINITY_UPGRADE.get())) return UpgradeSlotRules.LEVEL_1;
        if (stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get())) return UpgradeSlotRules.LEVEL_2;
        if (stack.is(SimplestoragemanagerModItems.MULTIDIMENSIONAL_INFINITY_UPGRADE.get())) return UpgradeSlotRules.LEVEL_3;
        return 0;
    }

    boolean canPlaceUpgrade(int slot, int level) {
        StorageManagerInventoryCapability upgrades = new StorageManagerInventoryCapability(terminalStack());
        boolean hasLevel1 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.BOTTOM_SLOT))
                == UpgradeSlotRules.LEVEL_1;
        boolean hasLevel2 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.MIDDLE_SLOT))
                == UpgradeSlotRules.LEVEL_2;
        return UpgradeSlotRules.canPlace(slot, level, hasLevel1, hasLevel2);
    }

    boolean canRemoveUpgrade(int slot) {
        StorageManagerInventoryCapability upgrades = new StorageManagerInventoryCapability(terminalStack());
        boolean hasLevel2 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.MIDDLE_SLOT))
                == UpgradeSlotRules.LEVEL_2;
        boolean hasLevel3 = upgradeLevel(upgrades.getStackInSlot(UpgradeSlotRules.TOP_SLOT))
                == UpgradeSlotRules.LEVEL_3;
        return UpgradeSlotRules.canRemove(slot, hasLevel2, hasLevel3);
    }

    private void sendView(ServerPlayer player, boolean operationResult) {
        TerminalScreenLayout.Variant layout = big ? TerminalScreenLayout.BIG : TerminalScreenLayout.SMALL;
        List<TerminalSavedData.Notice> savedNotices = search.isBlank()
                ? TerminalSavedData.get(player.serverLevel()).notices(terminalId) : List.of();
        List<LayoutEntry> entries = layoutEntries(savedNotices);
        int contentHeight = contentHeight(entries);
        int maxScroll = TerminalScreenLayout.scrollRange(layout, contentHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        int cursor = layout.contentArea().top() + 2 - scrollOffset;
        List<TerminalViewPayload.Section> sections = new ArrayList<>();
        List<TerminalViewPayload.VisibleSlot> visible = new ArrayList<>();
        List<TerminalViewPayload.Notice> noticePayload = new ArrayList<>();
        for (LayoutEntry entry : entries) {
            if (entry.notice() != null) {
                TerminalSavedData.Notice notice = entry.notice();
                noticePayload.add(new TerminalViewPayload.Notice(notice.dimension().location().toString(), notice.pos(),
                        notice.sectionIndex(), notice.slotCount()));
                cursor += sectionHeight(notice.slotCount());
                continue;
            }
            int index = entry.sectionIndex();
            ServerSection section = entry.section();
            int rows = Math.max(1, (section.slotCount() + 8) / 9);
            int slotY = cursor + SECTION_LABEL_HEIGHT;
            sections.add(new TerminalViewPayload.Section(label(section), coordinate(section.connection()),
                    section.status().ordinal(), section.slotCount()));
            for (int slot = 0; slot < section.slotCount(); slot++) {
                ItemStack stack = section.stackAt(slot);
                visible.add(new TerminalViewPayload.VisibleSlot(index, slot, stack.copy(),
                        section.slotLimitAt(slot, stack)));
            }
            cursor = slotY + rows * SLOT_SIZE + 2 + 4;
        }
        boolean locked = TerminalSavedData.get(player.serverLevel()).isLocked(terminalId);
        List<TerminalViewPayload.PlayerSlot> playerSlots = operationResult
                ? slots.stream().map(slot -> new TerminalViewPayload.PlayerSlot(slot.index, slot.getItem()))
                .toList()
                : List.of();
        player.connection.send(new TerminalViewPayload(big, scrollOffset, maxScroll, sections, visible,
                noticePayload, locked, originTargetsForView(player), lastOperationSequence, playerSlots,
                canUseConnectedInventories(player, false)));
    }

    private List<TerminalViewPayload.OriginTarget> originTargetsForView(ServerPlayer player) {
        if (!inventoryLockEnabled(player)) return List.of();
        UUID id = terminalId();
        if (id == null) return List.of();
        List<TerminalViewPayload.OriginTarget> targets = new ArrayList<>();
        for (TerminalOriginMemory.OriginRecord record : TerminalOriginMemory.records(player, id)) {
            int targetSection = -1;
            int targetSlot = -1;
            int targetSlotLimit = 0;
            if (search.isBlank()) {
                for (int section = 0; section < serverSections.size(); section++) {
                    ServerSection candidate = serverSections.get(section);
                    if (sameOriginConnection(candidate.connection(), record.connection())) {
                        targetSection = section;
                        targetSlot = record.slot();
                        targetSlotLimit = candidate.slotLimitAt(record.slot(), record.item());
                        break;
                    }
                }
            } else if (serverSections.size() == 1) {
                List<SearchEntry> results = serverSections.get(0).searchResults();
                for (int slot = 0; slot < results.size(); slot++) {
                    SearchEntry entry = results.get(slot);
                    if (sameOriginConnection(entry.connection(), record.connection())
                            && entry.slot() == record.slot()) {
                        targetSection = 0;
                        targetSlot = slot;
                        targetSlotLimit = entry.slotLimit();
                        break;
                    }
                }
            }
            targets.add(new TerminalViewPayload.OriginTarget(targetSection, targetSlot,
                    record.item(), record.remaining(), targetSlotLimit));
        }
        return List.copyOf(targets);
    }

    private List<LayoutEntry> layoutEntries(List<TerminalSavedData.Notice> notices) {
        List<TerminalSavedData.Notice> sorted = new ArrayList<>(notices);
        sorted.sort(Comparator.comparingInt(TerminalSavedData.Notice::sectionIndex));
        List<LayoutEntry> entries = new ArrayList<>();
        int noticeIndex = 0;
        for (int sectionIndex = 0; sectionIndex < serverSections.size(); sectionIndex++) {
            while (noticeIndex < sorted.size() && sorted.get(noticeIndex).sectionIndex() <= sectionIndex) {
                entries.add(LayoutEntry.notice(sorted.get(noticeIndex)));
                noticeIndex++;
            }
            entries.add(LayoutEntry.section(sectionIndex, serverSections.get(sectionIndex)));
        }
        while (noticeIndex < sorted.size()) {
            entries.add(LayoutEntry.notice(sorted.get(noticeIndex++)));
        }
        return entries;
    }

    private int contentHeight(List<LayoutEntry> entries) {
        int cursor = 2;
        for (LayoutEntry entry : entries) {
            cursor += sectionHeight(entry.notice() == null ? entry.section().slotCount() : entry.notice().slotCount());
        }
        return cursor;
    }

    private int sectionHeight(int slotCount) {
        int rows = Math.max(1, (slotCount + 8) / 9);
        return SECTION_LABEL_HEIGHT + rows * SLOT_SIZE + 2 + SECTION_GAP;
    }

    private Component label(ServerSection section) {
        return section.label();
    }

    private String coordinate(TerminalConnection connection) {
        if (connection == null) return "";
        return "(" + connection.pos().getX() + ", " + connection.pos().getY() + ", " + connection.pos().getZ() + ")";
    }

    public void handleStorageClick(ServerPlayer player, TerminalStorageClickPayload click) {
        if (world.isClientSide || !stillValid(player)) return;
        long sequence = click.operationSequence();
        if (sequence > 0 && sequence <= lastOperationSequence) {
            if (sequence == lastOperationSequence) refreshServerView(true);
            return;
        }
        if (sequence > 0) lastOperationSequence = sequence;
        handleStorageClickValidated(player, click);
        broadcastChanges();
        broadcastFullState();
        refreshServerView(sequence > 0);
    }

    private void handleStorageClickValidated(ServerPlayer player, TerminalStorageClickPayload click) {
        if (click.button() < 0 || click.button() > 2 || click.menuDragSlots().size() > 256
                || click.dragTargets().size() > 256 || click.shift() && click.button() != 0) return;
        boolean menuDrag = !click.menuDragSlots().isEmpty();
        boolean remoteDrag = !click.dragTargets().isEmpty();
        if (menuDrag && (click.shift()
                ? click.button() != 0
                : click.button() != 2 || !player.getAbilities().instabuild)) return;
        if (menuDrag || remoteDrag) {
            if (remoteDrag) {
                if (!validRemoteTarget(click.section(), click.slot())
                        || !canUseConnectedInventories(player, true)) return;
            }
            if (menuDrag) {
                if (click.shift()) handleMenuShiftDrag(player, click.menuDragSlots());
                else handleCreativeMenuDrag(player, click.menuDragSlots());
            }
            if (remoteDrag) {
                if (click.shift()) handleRemoteShiftDrag(player, click);
                else handleRemoteDrag(player, click);
            }
            return;
        }
        if (!validRemoteTarget(click.section(), click.slot()) || !canUseConnectedInventories(player, true)) return;
        if (click.button() == 2 && !player.getAbilities().instabuild) return;
        ServerSection section = serverSections.get(click.section());
        TerminalConnection connection = section.connection();
        int handlerSlot = click.slot();
        if (!section.searchResults().isEmpty()) {
            SearchEntry entry = section.searchResults().get(click.slot());
            connection = entry.connection();
            handlerSlot = entry.slot();
        }
        if (connection == null) return;
        InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
        InventoryAdapter adapter = resolution.adapter();
        if (resolution.status() != ConnectionStatus.AVAILABLE || adapter == null
                || handlerSlot >= adapter.getSlots()) return;
        if (click.shift()) {
            moveRemoteToPlayer(player, adapter, handlerSlot);
        } else {
            clickRemote(adapter, handlerSlot, click.button());
        }
    }

    private boolean validRemoteTarget(int section, int slot) {
        return section >= 0 && section < serverSections.size()
                && slot >= 0 && slot < serverSections.get(section).slotCount();
    }

    private void handleCreativeMenuDrag(ServerPlayer player, List<Integer> dragSlots) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || !player.getAbilities().instabuild) return;
        Set<Integer> seen = new HashSet<>();
        for (int index : dragSlots) {
            if (seen.size() >= 256 || !seen.add(index) || index < 0 || index >= slots.size()) continue;
            Slot slot = slots.get(index);
            if (isTerminalSlot(index) || !canDragTo(slot) || !slot.mayPlace(carried)) continue;
            ItemStack existing = slot.getItem();
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, carried)) continue;
            int count = Math.min(carried.getCount(), slot.getMaxStackSize(carried));
            if (count <= (existing.isEmpty() ? 0 : existing.getCount())) continue;
            slot.setByPlayer(carried.copyWithCount(count));
            slot.setChanged();
        }
    }

    // Shift-drag returns to remembered origins first, then uses one remote target per item type.
    private void handleMenuShiftDrag(ServerPlayer player, List<Integer> dragSlots) {
        if (!canUseConnectedInventories(player, true)) return;
        UUID id = terminalId();
        if (id == null) return;
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        if (connections.isEmpty()) return;

        List<RemoteDestination> destinations = displayedDestinations();
        List<ShiftDragDestination> selectedDestinations = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int index : dragSlots) {
            if (seen.size() >= 256 || !seen.add(index) || index < 0 || index >= slots.size()
                    || isTerminalSlot(index)) continue;
            Slot source = slots.get(index);
            if (!source.hasItem() || !source.mayPickup(player)) continue;
            if (index == craftingResultIndex) {
                quickMoveCraftingResult(player);
                continue;
            }

            ItemStack moving = source.getItem().copy();
            ItemStack original = moving.copy();
            insertIntoRememberedOrigins(moving, moving.getCount());
            insertShiftDragRemainder(moving, destinations, selectedDestinations);
            int inserted = original.getCount() - moving.getCount();
            if (inserted <= 0) continue;
            source.set(moving);
            source.onTake(player, original.copyWithCount(inserted));
        }
        player.getInventory().setChanged();
    }

    private List<RemoteDestination> displayedDestinations() {
        List<RemoteDestination> destinations = new ArrayList<>();
        for (ServerSection section : serverSections) {
            if (!section.searchResults().isEmpty()) {
                for (SearchEntry entry : section.searchResults()) {
                    addDisplayedDestination(destinations, entry.connection(), entry.slot());
                }
                continue;
            }
            if (section.connection() == null) continue;
            InventoryAdapter.Resolution resolution = verifyConnection(section.connection(), true);
            InventoryAdapter adapter = resolution.adapter();
            if (resolution.status() != ConnectionStatus.AVAILABLE || adapter == null) continue;
            for (int slot = 0; slot < Math.min(section.slotCount(), adapter.getSlots()); slot++) {
                destinations.add(new RemoteDestination(adapter.handler(), slot));
            }
        }
        return destinations;
    }

    private void insertShiftDragRemainder(ItemStack stack, List<RemoteDestination> destinations,
                                          List<ShiftDragDestination> selectedDestinations) {
        if (stack.isEmpty()) return;
        for (ShiftDragDestination selected : selectedDestinations) {
            if (ItemStack.isSameItemSameComponents(selected.item(), stack)) {
                insertRemoteSlot(selected.destination().handler(), selected.destination().slot(),
                        stack, stack.getCount());
                return;
            }
        }

        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT; pass++) {
            for (RemoteDestination destination : destinations) {
                IItemHandler handler = destination.handler();
                int slot = destination.slot();
                ItemStack existing = handler.getStackInSlot(slot);
                if (!acceptsInsertionSlot(existing, stack, pass) || !canAcceptRemoteSlot(handler, slot, stack)) {
                    continue;
                }
                int inserted = insertRemoteSlot(handler, slot, stack, stack.getCount());
                if (inserted > 0) {
                    selectedDestinations.add(new ShiftDragDestination(destination, handler.getStackInSlot(slot)));
                    return;
                }
            }
        }
    }

    // Apply origin-lock routing before the ordinary clicked-slot operation.
    private void clickRemote(InventoryAdapter adapter, int slot, int button) {
        ItemStack carried = getCarried();
        if (button != 2 && !carried.isEmpty() && entity instanceof ServerPlayer player
                && inventoryLockEnabled(player)) {
            UUID id = terminalId();
            if (id != null) {
                List<TerminalOriginMemory.OriginRecord> matchingOrigins = matchingOriginRecords(player, id, carried);
                if (!matchingOrigins.isEmpty()) {
                    boolean sameOrigin = matchingOrigins.stream()
                            .anyMatch(record -> sameOriginConnection(adapter.connection(), record.connection()));
                    if (sameOrigin) {
                        int inserted = clickRemoteAtSlot(adapter, slot, button);
                        List<TerminalOriginMemory.OriginRecord> prioritizedOrigins = new ArrayList<>(matchingOrigins);
                        prioritizedOrigins.sort((first, second) -> Boolean.compare(
                                !sameOriginConnection(adapter.connection(), first.connection()),
                                !sameOriginConnection(adapter.connection(), second.connection())));
                        consumeOriginRecords(player, id, prioritizedOrigins, inserted);
                        return;
                    }

                    int returnedToOrigin = insertIntoFirstEmptyOrigins(player, id, carried,
                            button == 1 ? 1 : carried.getCount(), matchingOrigins);
                    if (returnedToOrigin > 0) {
                        setCarried(carried);
                        return;
                    }
                    int inserted = clickRemoteAtSlot(adapter, slot, button);
                    consumeOriginRecords(player, id, matchingOrigins, inserted);
                    return;
                }
            }
        }
        clickRemoteAtSlot(adapter, slot, button);
    }

    private List<TerminalOriginMemory.OriginRecord> matchingOriginRecords(
            ServerPlayer player, UUID id, ItemStack stack) {
        return TerminalOriginMemory.records(player, id).stream()
                .filter(record -> ItemStack.isSameItemSameComponents(record.item(), stack))
                .toList();
    }

    private void consumeOriginRecords(ServerPlayer player, UUID id,
                                      List<TerminalOriginMemory.OriginRecord> records, int count) {
        int remaining = count;
        for (TerminalOriginMemory.OriginRecord record : records) {
            if (remaining <= 0) break;
            int consumed = Math.min(remaining, record.remaining());
            if (consumed <= 0) continue;
            TerminalOriginMemory.consume(player, id, record.id(), consumed);
            remaining -= consumed;
        }
    }

    private int insertIntoFirstEmptyOrigins(ServerPlayer player, UUID id, ItemStack stack, int maximum,
                                            List<TerminalOriginMemory.OriginRecord> records) {
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        List<TerminalConnection> visited = new ArrayList<>();
        int inserted = 0;
        for (TerminalOriginMemory.OriginRecord record : records) {
            if (stack.isEmpty() || inserted >= maximum) break;
            if (visited.stream().anyMatch(connection -> sameOriginConnection(connection, record.connection()))) continue;
            visited.add(record.connection());
            List<TerminalOriginMemory.OriginRecord> originRecords = records.stream()
                    .filter(candidate -> sameOriginConnection(candidate.connection(), record.connection()))
                    .toList();
            int pending = originRecords.stream().mapToInt(TerminalOriginMemory.OriginRecord::remaining).sum();
            TerminalConnection current = connections.stream()
                    .filter(connection -> sameOriginConnection(connection, record.connection()))
                    .findFirst().orElse(null);
            if (current == null) {
                discardOriginRecords(player, id, originRecords);
                continue;
            }
            InventoryAdapter.Resolution resolution = verifyConnection(current, true);
            if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) {
                discardOriginRecords(player, id, originRecords);
                continue;
            }
            int requested = Math.min(Math.min(stack.getCount(), maximum - inserted), pending);
            int moved = insertIntoOriginInventory(resolution.adapter().handler(), stack, requested);
            if (moved > 0) {
                stack.shrink(moved);
                inserted += moved;
                consumeOriginRecords(player, id, originRecords, moved);
            }
        }
        return inserted;
    }

    private void discardOriginRecords(ServerPlayer player, UUID id,
                                      List<TerminalOriginMemory.OriginRecord> records) {
        for (TerminalOriginMemory.OriginRecord record : records) {
            TerminalOriginMemory.discard(player, id, record.id());
        }
    }

    // Manual clicks retain vanilla left/right behavior on the requested remote slot.
    private int clickRemoteAtSlot(InventoryAdapter adapter, int slot, int button) {
        IItemHandler handler = adapter.handler();
        ItemStack carried = getCarried();
        if (button == 2) {
            if (!entity.getAbilities().instabuild) return 0;
            ItemStack existing = handler.getStackInSlot(slot);
            if (!existing.isEmpty()) setCarried(existing.copyWithCount(existing.getMaxStackSize()));
            return 0;
        }
        if (carried.isEmpty()) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty()) return 0;
            int amount = Math.min(existing.getMaxStackSize(),
                    button == 1 ? (existing.getCount() + 1) / 2 : existing.getCount());
            ItemStack simulated = handler.extractItem(slot, amount, true);
            if (!ItemStack.isSameItemSameComponents(existing, simulated) || simulated.isEmpty()) return 0;
            int extractedCount = Math.min(amount, simulated.getCount());
            ItemStack extracted;
            if (handler instanceof IItemHandlerModifiable modifiable) {
                ItemStack remaining = existing.copyWithCount(existing.getCount() - extractedCount);
                extracted = existing.copyWithCount(extractedCount);
                modifiable.setStackInSlot(slot, remaining);
            } else {
                extracted = handler.extractItem(slot, extractedCount, false);
                if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(existing, extracted)) return 0;
            }
            recordExtraction((ServerPlayer) entity, adapter.connection(), slot, extracted);
            setCarried(extracted);
            return 0;
        }
        int incomingCount = carried.getCount();
        int requested = button == 1 ? 1 : incomingCount;
        ItemStack existing = handler.getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, carried)) {
            ItemStack simulatedExtraction = handler.extractItem(slot, existing.getCount(), true);
            if (button == 0 && handler instanceof IItemHandlerModifiable modifiable
                    && ItemStack.isSameItemSameComponents(existing, simulatedExtraction)
                    && simulatedExtraction.getCount() == existing.getCount()
                    && handler.isItemValid(slot, carried)
                    && carried.getCount() <= Math.min(handler.getSlotLimit(slot), carried.getMaxStackSize())
                    && existing.getCount() <= carried.getMaxStackSize()) {
                modifiable.setStackInSlot(slot, carried.copy());
                recordExtraction((ServerPlayer) entity, adapter.connection(), slot, existing);
                setCarried(existing.copy());
                return incomingCount;
            }
            return 0;
        }
        if (!handler.isItemValid(slot, carried)) {
            setCarried(carried);
            return 0;
        }
        int limit = Math.max(0, handler.getSlotLimit(slot));
        limit = Math.min(limit, carried.getMaxStackSize());
        int currentCount = existing.isEmpty() ? 0 : existing.getCount();
        int capacity = Math.max(0, limit - currentCount);
        int offeredCount = Math.min(requested, capacity);
        if (offeredCount <= 0) return 0;
        ItemStack offered = carried.copyWithCount(offeredCount);
        ItemStack simulated = handler.insertItem(slot, offered, true);
        if (!simulated.isEmpty() && !ItemStack.isSameItemSameComponents(carried, simulated)) return 0;
        int accepted = offeredCount - simulated.getCount();
        if (accepted <= 0) return 0;
        int inserted;
        if (handler instanceof IItemHandlerModifiable modifiable) {
            ItemStack updated = existing.isEmpty()
                    ? carried.copyWithCount(accepted)
                    : existing.copyWithCount(currentCount + accepted);
            modifiable.setStackInSlot(slot, updated);
            inserted = accepted;
        } else {
            ItemStack remainder = handler.insertItem(slot, offered.copyWithCount(accepted), false);
            if (!remainder.isEmpty() && !ItemStack.isSameItemSameComponents(carried, remainder)) return 0;
            inserted = accepted - remainder.getCount();
        }
        carried.shrink(inserted);
        setCarried(carried);
        return inserted;
    }

    // Remote drag targets are resolved and mutated as one server-side operation.
    private void handleRemoteDrag(ServerPlayer player, TerminalStorageClickPayload click) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;
        boolean creativeFill = click.button() == 2;
        if (creativeFill && !player.getAbilities().instabuild) return;
        int requested = click.button() == 1
                ? Math.min(carried.getCount(), click.dragTargets().size()) : carried.getCount();

        List<RemoteDestination> destinations = new ArrayList<>();
        Set<TerminalStorageClickPayload.SlotTarget> seen = new HashSet<>();
        boolean targetsOrigin = false;
        List<TerminalOriginMemory.OriginRecord> matchingOrigins = inventoryLockEnabled(player)
                ? matchingOriginRecords(player, terminalId(), carried) : List.of();
        for (TerminalStorageClickPayload.SlotTarget target : click.dragTargets()) {
            if (destinations.size() >= 256 || !seen.add(target)
                    || target.section() < 0 || target.section() >= serverSections.size()) continue;
            ServerSection section = serverSections.get(target.section());
            if (target.slot() < 0 || target.slot() >= section.slotCount()) continue;

            TerminalConnection connection = section.connection();
            int handlerSlot = target.slot();
            if (!section.searchResults().isEmpty()) {
                SearchEntry entry = section.searchResults().get(target.slot());
                connection = entry.connection();
                handlerSlot = entry.slot();
            }
            if (connection == null) continue;
            InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
            InventoryAdapter adapter = resolution.adapter();
            if (resolution.status() != ConnectionStatus.AVAILABLE || adapter == null
                    || handlerSlot >= adapter.getSlots()
                    || !canAcceptRemoteSlot(adapter.handler(), handlerSlot, carried)) continue;
            TerminalConnection targetConnection = connection;
            if (matchingOrigins.stream()
                    .anyMatch(record -> sameOriginConnection(targetConnection, record.connection()))) {
                targetsOrigin = true;
            }
            destinations.add(new RemoteDestination(adapter.handler(), handlerSlot));
        }

        int originInserted = 0;
        if (!creativeFill && !targetsOrigin) {
            originInserted = insertIntoRememberedOrigins(carried, requested);
        }

        if (destinations.isEmpty()) {
            setCarried(carried);
            return;
        }
        if (creativeFill) {
            for (RemoteDestination destination : destinations) {
                fillRemoteSlotCreative(destination.handler(), destination.slot(), carried);
            }
        } else {
            int distributionBudget = Math.max(0, requested - originInserted);
            if (click.button() == 1) {
                for (RemoteDestination destination : destinations) {
                    if (distributionBudget <= 0 || carried.isEmpty()) break;
                    int inserted = insertRemoteDragSlot(destination.handler(), destination.slot(), carried, 1);
                    distributionBudget -= inserted;
                }
            } else {
                int amount = distributionBudget / destinations.size();
                for (RemoteDestination destination : destinations) {
                    if (carried.isEmpty() || amount <= 0 || distributionBudget <= 0) break;
                    int inserted = insertRemoteDragSlot(destination.handler(), destination.slot(), carried,
                            Math.min(amount, distributionBudget));
                    distributionBudget -= inserted;
                }
            }
        }
        setCarried(carried);
    }

    // Some mod handlers redirect insertItem; preserve the hovered index when directly modifiable.
    private int insertRemoteDragSlot(IItemHandler handler, int slot, ItemStack carried, int requested) {
        if (!(handler instanceof IItemHandlerModifiable modifiable)
                || handler instanceof InvWrapper
                || handler instanceof SidedInvWrapper
                || handler instanceof CombinedInvWrapper) {
            return insertRemoteSlot(handler, slot, carried, requested);
        }
        if (!canAcceptRemoteSlot(handler, slot, carried)) return 0;
        ItemStack existing = handler.getStackInSlot(slot);
        int currentCount = existing.isEmpty() ? 0 : existing.getCount();
        int limit = Math.min(carried.getMaxStackSize(), handler.getSlotLimit(slot));
        int offeredCount = Math.min(requested, Math.max(0, limit - currentCount));
        if (offeredCount <= 0) return 0;

        ItemStack simulated = handler.insertItem(slot, carried.copyWithCount(offeredCount), true);
        if (!simulated.isEmpty() && !ItemStack.isSameItemSameComponents(carried, simulated)) return 0;
        int inserted = offeredCount - simulated.getCount();
        if (inserted <= 0) return 0;

        ItemStack updated = existing.isEmpty()
                ? carried.copyWithCount(inserted)
                : existing.copyWithCount(currentCount + inserted);
        modifiable.setStackInSlot(slot, updated);
        carried.shrink(inserted);
        return inserted;
    }

    private void handleRemoteShiftDrag(ServerPlayer player, TerminalStorageClickPayload click) {
        Set<TerminalStorageClickPayload.SlotTarget> seen = new HashSet<>();
        for (TerminalStorageClickPayload.SlotTarget target : click.dragTargets()) {
            if (seen.size() >= 256 || !seen.add(target)
                    || target.section() < 0 || target.section() >= serverSections.size()) continue;
            ServerSection section = serverSections.get(target.section());
            if (target.slot() < 0 || target.slot() >= section.slotCount()) continue;
            TerminalConnection connection = section.connection();
            int handlerSlot = target.slot();
            if (!section.searchResults().isEmpty()) {
                SearchEntry entry = section.searchResults().get(target.slot());
                connection = entry.connection();
                handlerSlot = entry.slot();
            }
            if (connection == null) continue;
            InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
            if (resolution.status() == ConnectionStatus.AVAILABLE && resolution.adapter() != null
                    && handlerSlot < resolution.adapter().getSlots()) {
                moveRemoteToPlayer(player, resolution.adapter(), handlerSlot);
            }
        }
    }

    private void fillRemoteSlotCreative(IItemHandler handler, int slot, ItemStack stack) {
        if (stack.isEmpty() || !handler.isItemValid(slot, stack)) return;
        ItemStack existing = handler.getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return;
        int limit = Math.max(0, handler.getSlotLimit(slot));
        int currentCount = existing.isEmpty() ? 0 : existing.getCount();
        int count = Math.max(currentCount, Math.min(stack.getCount(), limit));
        if (count <= currentCount) return;
        ItemStack offered = stack.copyWithCount(count - currentCount);
        ItemStack remainder = handler.insertItem(slot, offered, true);
        if (!remainder.isEmpty()) return;
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack.copyWithCount(count));
        } else {
            handler.insertItem(slot, offered, false);
        }
    }

    private boolean canAcceptRemoteSlot(IItemHandler handler, int slot, ItemStack stack) {
        ItemStack existing = handler.getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return false;
        int currentCount = existing.isEmpty() ? 0 : existing.getCount();
        int limit = Math.max(0, handler.getSlotLimit(slot));
        int capacity = limit - currentCount;
        if (capacity <= 0 || !handler.isItemValid(slot, stack)) return false;
        ItemStack simulated = handler.insertItem(slot, stack.copyWithCount(1), true);
        int accepted = 1 - (simulated.isEmpty() ? 0 : simulated.getCount());
        return accepted == 1;
    }

    private int insertRemoteSlot(IItemHandler handler, int slot, ItemStack carried, int requested) {
        if (!canAcceptRemoteSlot(handler, slot, carried)) return 0;
        ItemStack existing = handler.getStackInSlot(slot);
        int currentCount = existing.isEmpty() ? 0 : existing.getCount();
        int capacity = Math.max(0, handler.getSlotLimit(slot) - currentCount);
        int offeredCount = Math.min(requested, capacity);
        if (offeredCount <= 0) return 0;

        ItemStack offered = carried.copyWithCount(offeredCount);
        ItemStack simulated = handler.insertItem(slot, offered, true);
        if (!simulated.isEmpty() && !ItemStack.isSameItemSameComponents(carried, simulated)) return 0;
        int accepted = offeredCount - simulated.getCount();
        if (accepted <= 0) return 0;

        ItemStack remainder = handler.insertItem(slot, offered.copyWithCount(accepted), false);
        if (!remainder.isEmpty() && !ItemStack.isSameItemSameComponents(carried, remainder)) return 0;
        int inserted = accepted - remainder.getCount();
        carried.shrink(inserted);
        return inserted;
    }

    private void moveRemoteToPlayer(ServerPlayer player, InventoryAdapter adapter, int slot) {
        ItemStack source = adapter.handler().getStackInSlot(slot).copy();
        if (source.isEmpty()) return;
        int availableSpace = playerInventorySpace(source);
        int wanted = Math.min(source.getCount(), availableSpace);
        if (wanted <= 0) return;
        ItemStack extracted = adapter.handler().extractItem(slot, wanted, false);
        ItemStack remainder = extracted.copy();
        moveItemStackTo(remainder, mainInventoryStart, hotbarStart + 9, false);
        int inserted = extracted.getCount() - remainder.getCount();
        if (inserted > 0) {
            recordExtraction(player, adapter.connection(), slot, extracted.copyWithCount(inserted));
        }
        if (!remainder.isEmpty()) adapter.handler().insertItem(slot, remainder, false);
    }

    private int playerInventorySpace(ItemStack stack) {
        int space = 0;
        for (int index = mainInventoryStart; index < hotbarStart + 9; index++) {
            Slot target = slots.get(index);
            if (isTerminalSlot(target.index) || !target.mayPlace(stack)) continue;
            ItemStack existing = target.getItem();
            if (existing.isEmpty()) {
                space += Math.min(stack.getMaxStackSize(), target.getMaxStackSize(stack));
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                space += Math.max(0, Math.min(stack.getMaxStackSize(), target.getMaxStackSize(stack))
                        - existing.getCount());
            }
        }
        return space;
    }

    public void dismissNotice(ServerPlayer player, TerminalNoticeDismissPayload payload) {
        UUID id = terminalId();
        if (id == null) return;
        ResourceKeyParser.dismiss(TerminalSavedData.get(player.serverLevel()), id, payload);
        refreshServerView();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != craftingGrid || world.isClientSide || !(entity instanceof ServerPlayer player)) return;

        CraftingInput input = craftingGrid.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        var recipe = input.isEmpty()
                ? java.util.Optional.<RecipeHolder<CraftingRecipe>>empty()
                : world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, world);
        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipe.get();
            if (craftingResult.setRecipeUsed(world, player, holder)) {
                ItemStack assembled = holder.value().assemble(input, world.registryAccess());
                if (assembled.isItemEnabled(world.enabledFeatures())) {
                    result = assembled;
                }
            }
        } else {
            craftingResult.setRecipeUsed((RecipeHolder<?>) null);
        }
        craftingResult.setItem(0, result);
    }

    @Override
    public void removed(Player player) {
        closed = true;
        passiveCheckTicks = 0;
        if (!world.isClientSide && player == entity) {
            returnCraftingItems(player, true);
        }
        super.removed(player);
    }

    private void returnCraftingItems(Player player, boolean dropRemainder) {
        UUID id = terminalId();
        List<TerminalConnection> connections = player instanceof ServerPlayer serverPlayer && id != null
                ? TerminalSavedData.get(serverPlayer.serverLevel()).connections(id) : List.of();
        boolean canReturnToConnections = connections.isEmpty()
                || player instanceof ServerPlayer serverPlayer && canUseConnectedInventories(serverPlayer, false);
        boolean ownershipWarningSent = false;
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            ItemStack stack = craftingGrid.removeItemNoUpdate(slot);
            if (stack.isEmpty()) continue;
            insertIntoPlayerInventory(stack);
            if (!stack.isEmpty() && canReturnToConnections) {
                stack = insertIntoConnectedInventories(stack, false);
            } else if (!stack.isEmpty() && !ownershipWarningSent
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner"));
                ownershipWarningSent = true;
            }
            if (!stack.isEmpty() && dropRemainder) {
                player.drop(stack, false);
            } else if (!stack.isEmpty()) {
                craftingGrid.setItem(slot, stack);
            }
        }
        slotsChanged(craftingGrid);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().setChanged();
        }
    }

    private void insertIntoPlayerInventory(ItemStack stack) {
        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && !stack.isEmpty(); pass++) {
            insertIntoPlayerRange(stack, mainInventoryStart, hotbarStart, pass);
            insertIntoPlayerRange(stack, hotbarStart, hotbarStart + 9, pass);
        }
    }

    private void insertIntoPlayerRange(ItemStack stack, int start, int end, int pass) {
        for (int index = start; index < end && !stack.isEmpty(); index++) {
            Slot target = slots.get(index);
            if (isTerminalSlot(target.index) || !target.mayPlace(stack)) continue;
            ItemStack existing = target.getItem();
            if (!acceptsInsertionSlot(existing, stack, pass)) continue;
            int limit = Math.min(stack.getMaxStackSize(), target.getMaxStackSize(stack));
            int amount = Math.min(stack.getCount(), limit - (existing.isEmpty() ? 0 : existing.getCount()));
            if (amount <= 0) continue;
            if (existing.isEmpty()) {
                target.set(stack.copyWithCount(amount));
            } else {
                existing.grow(amount);
                target.setChanged();
            }
            stack.shrink(amount);
        }
    }

    private ItemStack insertIntoConnectedInventories(ItemStack stack) {
        return insertIntoConnectedInventories(stack, true);
    }

    private ItemStack insertIntoConnectedInventories(ItemStack stack, boolean notify) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty()) return stack;
        UUID id = terminalId();
        if (id == null) return stack;
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        if (connections.isEmpty() || !canUseConnectedInventories(player, notify)) return stack;
        insertIntoRememberedOrigins(stack, stack.getCount());
        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && !stack.isEmpty(); pass++) {
            for (TerminalConnection connection : connections) {
                if (stack.isEmpty()) return ItemStack.EMPTY;
                InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
                if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) continue;
                IItemHandler handler = resolution.adapter().handler();
                for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                    ItemStack existing = handler.getStackInSlot(slot);
                    if (!acceptsInsertionSlot(existing, stack, pass)) continue;
                    if (!handler.isItemValid(slot, stack)) continue;
                    stack = handler.insertItem(slot, stack, false);
                }
            }
        }
        return stack;
    }

    private ItemStack insertIntoDisplayedConnections(ItemStack stack) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty() || !canUseConnectedInventories(player, true)) {
            return stack;
        }
        insertIntoRememberedOrigins(stack, stack.getCount());
        if (stack.isEmpty()) return ItemStack.EMPTY;

        List<RemoteDestination> destinations = new ArrayList<>();
        for (ServerSection section : serverSections) {
            if (!section.searchResults().isEmpty()) {
                for (SearchEntry entry : section.searchResults()) {
                    addDisplayedDestination(destinations, entry.connection(), entry.slot());
                }
                continue;
            }
            if (section.connection() == null) continue;
            InventoryAdapter.Resolution resolution = verifyConnection(section.connection(), true);
            InventoryAdapter adapter = resolution.adapter();
            if (resolution.status() != ConnectionStatus.AVAILABLE || adapter == null) continue;
            for (int slot = 0; slot < Math.min(section.slotCount(), adapter.getSlots()); slot++) {
                destinations.add(new RemoteDestination(adapter.handler(), slot));
            }
        }

        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && !stack.isEmpty(); pass++) {
            for (RemoteDestination destination : destinations) {
                if (stack.isEmpty()) break;
                IItemHandler handler = destination.handler();
                int slot = destination.slot();
                ItemStack existing = handler.getStackInSlot(slot);
                if (!acceptsInsertionSlot(existing, stack, pass)
                        || !handler.isItemValid(slot, stack)) continue;
                ItemStack simulated = handler.insertItem(slot, stack.copy(), true);
                if (simulated.getCount() == stack.getCount()) continue;
                stack = handler.insertItem(slot, stack, false);
            }
        }
        return stack;
    }

    private void addDisplayedDestination(List<RemoteDestination> destinations, TerminalConnection connection, int slot) {
        InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
        InventoryAdapter adapter = resolution.adapter();
        if (resolution.status() != ConnectionStatus.AVAILABLE || adapter == null || slot >= adapter.getSlots()) {
            return;
        }
        destinations.add(new RemoteDestination(adapter.handler(), slot));
    }

    private ItemStack simulateConnectedInsert(ItemStack stack) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty()) return stack;
        UUID id = terminalId();
        if (id == null) return stack;
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        if (connections.isEmpty() || !canUseConnectedInventories(player, true)) return stack;
        stack = simulateRememberedOriginInsert(stack, connections);
        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && !stack.isEmpty(); pass++) {
            for (TerminalConnection connection : connections) {
                if (stack.isEmpty()) return ItemStack.EMPTY;
                InventoryAdapter.Resolution resolution = resolve(connection);
                if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) continue;
                IItemHandler handler = resolution.adapter().handler();
                for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                    ItemStack existing = handler.getStackInSlot(slot);
                    if (!acceptsInsertionSlot(existing, stack, pass)
                            || !handler.isItemValid(slot, stack)) continue;
                    stack = handler.insertItem(slot, stack, true);
                }
            }
        }
        return stack;
    }

    private ItemStack simulateRememberedOriginInsert(ItemStack stack, List<TerminalConnection> connections) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty() || !inventoryLockEnabled(player)) return stack;
        UUID id = terminalId();
        if (id == null) return stack;
        ItemStack simulatedStack = stack.copy();
        for (TerminalOriginMemory.OriginRecord record : TerminalOriginMemory.records(player, id)) {
            if (simulatedStack.isEmpty()) break;
            if (!ItemStack.isSameItemSameComponents(record.item(), simulatedStack)) continue;
            TerminalConnection current = connections.stream()
                    .filter(connection -> sameOriginConnection(connection, record.connection()))
                    .findFirst().orElse(null);
            if (current == null) break;
            InventoryAdapter.Resolution resolution = verifyConnection(current, true);
            if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) break;
            int requested = Math.min(simulatedStack.getCount(), record.remaining());
            int inserted = simulateOriginInventory(resolution.adapter().handler(), simulatedStack, requested);
            simulatedStack.shrink(inserted);
            if (inserted < requested) break;
        }
        return simulatedStack;
    }

    private int simulateOriginInventory(IItemHandler handler, ItemStack stack, int requested) {
        int inserted = 0;
        for (int pass = 0; pass < TerminalInsertionRules.PASS_COUNT && inserted < requested; pass++) {
            for (int slot = 0; slot < handler.getSlots() && inserted < requested; slot++) {
                ItemStack existing = handler.getStackInSlot(slot);
                if (!acceptsInsertionSlot(existing, stack, pass)
                        || !handler.isItemValid(slot, stack)) continue;
                int count = Math.min(requested - inserted, stack.getCount() - inserted);
                if (count <= 0) break;
                ItemStack remainder = handler.insertItem(slot, stack.copyWithCount(count), true);
                if (!remainder.isEmpty() && !ItemStack.isSameItemSameComponents(stack, remainder)) continue;
                inserted += Math.max(0, Math.min(count, count - remainder.getCount()));
            }
        }
        return inserted;
    }

    private boolean canFitCraftedResult(ItemStack result) {
        ItemStack remainder = result.copy();
        int playerSpace = playerInventorySpace(remainder);
        remainder.shrink(Math.min(remainder.getCount(), playerSpace));
        return remainder.isEmpty() || simulateConnectedInsert(remainder).isEmpty();
    }

    private static boolean acceptsInsertionSlot(ItemStack existing, ItemStack incoming, int pass) {
        boolean empty = existing.isEmpty();
        boolean matches = !empty && ItemStack.isSameItemSameComponents(existing, incoming);
        return TerminalInsertionRules.acceptsSlot(empty, matches, pass);
    }

    private static int insertionCapacity(ItemStack incoming, ItemStack existing, int slotLimit) {
        return TerminalInsertionRules.capacity(incoming.getMaxStackSize(),
                existing.isEmpty() ? 0 : existing.getCount(), slotLimit);
    }

    private ItemStack quickMoveCraftingResult(Player player) {
        Slot resultSlot = slots.get(craftingResultIndex);
        if (!resultSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = resultSlot.getItem();
        ItemStack original = source.copy();
        if (world.getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING, craftingGrid.asCraftInput(), world).isEmpty()
                || !canFitCraftedResult(source)) return ItemStack.EMPTY;
        ItemStack remaining = source.copy();
        insertIntoPlayerInventory(remaining);
        if (!remaining.isEmpty()) remaining = insertIntoConnectedInventories(remaining);
        if (!remaining.isEmpty()) return ItemStack.EMPTY;
        resultSlot.onQuickCraft(remaining, original);
        resultSlot.setByPlayer(ItemStack.EMPTY);
        resultSlot.onTake(player, remaining);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && !player.isRemoved()
                && player.getItemInHand(hand).is(SimplestoragemanagerModItems.STORAGE_MANAGER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= upgradeStart && index < upgradeStart + 3) {
            return quickMoveUpgrade(player, index);
        }
        if (index == craftingResultIndex) {
            if (!slots.get(index).hasItem()) return ItemStack.EMPTY;
            return quickMoveCraftingResult(player);
        }
        if ((index >= craftingGridStart && index < craftingGridStart + 9)
                || (index >= armorStart && index <= offhandIndex)) {
            Slot source = slots.get(index);
            if (!source.hasItem()) return ItemStack.EMPTY;
            ItemStack original = source.getItem().copy();
            ItemStack moving = original.copy();
            insertIntoPlayerInventory(moving);
            if (!moving.isEmpty()) moving = insertIntoConnectedInventories(moving);
            int moved = original.getCount() - moving.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            source.set(moving);
            source.onTake(player, original.copyWithCount(moved));
            return original.copyWithCount(moved);
        }

        if (isTerminalSlot(index) || index < mainInventoryStart || index >= hotbarStart + 9) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack original = source.getItem().copy();
        ItemStack moving = original.copy();
        if (player instanceof ServerPlayer serverPlayer) {
            UUID id = terminalId();
            if (id != null && !TerminalSavedData.get(serverPlayer.serverLevel()).connections(id).isEmpty()
                    && !canUseConnectedInventories(serverPlayer, true)) {
                return ItemStack.EMPTY;
            }
        }
        moving = insertIntoDisplayedConnections(moving);
        int moved = original.getCount() - moving.getCount();
        if (moved <= 0) return ItemStack.EMPTY;
        source.set(moving);
        source.onTake(player, original.copyWithCount(moved));
        refreshServerView();
        return original.copyWithCount(moved);
    }

    private ItemStack quickMoveUpgrade(Player player, int index) {
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack existing = source.getItem();
        if (existing.isEmpty()) return ItemStack.EMPTY;

        Slot destination = findPlayerSlot(mainInventoryStart, hotbarStart, existing);
        if (destination == null) {
            destination = findPlayerSlot(hotbarStart, hotbarStart + 9, existing);
        }
        if (destination != null) {
            ItemStack moved = source.remove(existing.getCount());
            if (moved.isEmpty()) return ItemStack.EMPTY;
            ItemStack remainder = putInSlot(destination, moved);
            if (!remainder.isEmpty()) source.set(remainder);
            source.onTake(player, moved);
            broadcastChanges();
            return remainder.isEmpty() ? moved : ItemStack.EMPTY;
        }

        if (!(player instanceof ServerPlayer serverPlayer)
                || !canUseConnectedInventories(serverPlayer, true)) return ItemStack.EMPTY;
        ItemStack original = existing.copy();
        ItemStack moved = source.remove(existing.getCount());
        if (moved.isEmpty()) return ItemStack.EMPTY;
        int prioritized = insertIntoRememberedOrigins(moved, moved.getCount());
        int inserted = prioritized;
        if (!moved.isEmpty()) {
            ConnectedDestination connected = findConnectedSlot(serverPlayer, moved);
            if (connected == null) {
                source.set(moved);
                source.setChanged();
                return ItemStack.EMPTY;
            }
            ItemStack remainder = connected.handler().insertItem(connected.slot(), moved, false);
            inserted += moved.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) source.set(remainder);
        }
        source.onTake(player, original.copyWithCount(inserted));
        broadcastChanges();
        refreshServerView();
        return inserted == original.getCount() ? original : ItemStack.EMPTY;
    }

    private Slot findPlayerSlot(int start, int end, ItemStack stack) {
        for (int index = start; index < end; index++) {
            Slot candidate = slots.get(index);
            if (isTerminalSlot(candidate.index) || !candidate.mayPlace(stack)) continue;
            ItemStack present = candidate.getItem();
            boolean compatible = present.isEmpty() || ItemStack.isSameItemSameComponents(present, stack);
            int currentCount = present.isEmpty() ? 0 : present.getCount();
            int limit = Math.min(stack.getMaxStackSize(), candidate.getMaxStackSize(stack));
            if (compatible && limit - currentCount >= stack.getCount()) return candidate;
        }
        return null;
    }

    private ItemStack putInSlot(Slot destination, ItemStack stack) {
        ItemStack present = destination.getItem();
        if (present.isEmpty()) {
            destination.set(stack.copy());
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(present, stack)) return stack;
        int limit = Math.min(stack.getMaxStackSize(), destination.getMaxStackSize(stack));
        int moved = Math.min(stack.getCount(), limit - present.getCount());
        if (moved <= 0) return stack;
        present.grow(moved);
        destination.setChanged();
        return stack.copyWithCount(stack.getCount() - moved);
    }

    private ConnectedDestination findConnectedSlot(ServerPlayer player, ItemStack stack) {
        UUID id = terminalId();
        if (id == null) return null;
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        if (connections.isEmpty() || !canUseConnectedInventories(player, true)) return null;
        for (TerminalConnection connection : connections) {
            InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
            if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null) continue;
            IItemHandler handler = resolution.adapter().handler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (!handler.getStackInSlot(slot).isEmpty() || !handler.isItemValid(slot, stack)) continue;
                if (handler.insertItem(slot, stack.copy(), true).isEmpty()) {
                    return new ConnectedDestination(handler, slot);
                }
            }
        }
        return null;
    }

    private record ConnectedDestination(IItemHandler handler, int slot) { }
    private record RemoteDestination(IItemHandler handler, int slot) { }
    private record ShiftDragDestination(RemoteDestination destination, ItemStack item) { }

    private void depositIntoConnections(ItemStack stack, boolean storageOnly) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty()) return;
        UUID id = terminalId();
        if (id == null) return;
        List<TerminalConnection> connections = TerminalSavedData.get(player.serverLevel()).connections(id);
        if (connections.isEmpty() || !canUseConnectedInventories(player, false)) return;
        insertIntoRememberedOrigins(stack, stack.getCount());
        for (TerminalConnection connection : connections) {
            if (stack.isEmpty()) return;
            InventoryAdapter.Resolution resolution = verifyConnection(connection, true);
            if (resolution.status() != ConnectionStatus.AVAILABLE || resolution.adapter() == null
                    || resolution.adapter().isStorage() != storageOnly) continue;
            IItemHandler handler = resolution.adapter().handler();
            for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                if (!handler.isItemValid(slot, stack)) continue;
                ItemStack remainder = handler.insertItem(slot, stack.copy(), true);
                int accepted = stack.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack actualRemainder = handler.insertItem(slot, stack.copyWithCount(accepted), false);
                stack.shrink(accepted - actualRemainder.getCount());
            }
        }
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= mainInventoryStart && slot.index < hotbarStart + 9
                && !isTerminalSlot(slot.index)
                || slot.index >= craftingGridStart && slot.index < craftingGridStart + 9;
    }

    @Override
    public Map<Integer, Slot> getSlots() { return Collections.unmodifiableMap(customSlots); }
    @Override
    public Map<String, Object> getMenuState() { return menuState; }

    private record ServerSection(TerminalConnection connection, ConnectionStatus status, Component label,
                                 int slotCount, List<ItemStack> contents, List<Integer> slotLimits,
                                 List<SearchEntry> searchResults) {
        private ItemStack stackAt(int slot) {
            if (!searchResults.isEmpty()) return searchResults.get(slot).stack();
            return slot >= contents.size() ? ItemStack.EMPTY : contents.get(slot);
        }

        private int slotLimitAt(int slot, ItemStack stack) {
            if (!searchResults.isEmpty()) return searchResults.get(slot).slotLimit();
            return slot >= slotLimits.size() ? stack.getMaxStackSize() : slotLimits.get(slot);
        }
    }

    private record LayoutEntry(int sectionIndex, ServerSection section, TerminalSavedData.Notice notice) {
        private static LayoutEntry section(int index, ServerSection section) {
            return new LayoutEntry(index, section, null);
        }

        private static LayoutEntry notice(TerminalSavedData.Notice notice) {
            return new LayoutEntry(-1, null, notice);
        }
    }

    private record SearchEntry(TerminalConnection connection, int slot, ItemStack stack, int slotLimit) { }

    private static final class ComponentText {
        private static String statusLabel(ConnectionStatus status) {
            return net.minecraft.network.chat.Component.translatable(switch (status) {
                case CHUNK_UNLOADED -> "screen.simplestoragemanager.chunk_unloaded";
                case DIMENSION_MISSING -> "screen.simplestoragemanager.dimension_missing";
                case BLOCK_REMOVED -> "screen.simplestoragemanager.connection_lost";
                case BASIC_UNAVAILABLE -> "screen.simplestoragemanager.connection_lost";
                case AVAILABLE -> "screen.simplestoragemanager.search_results";
            }).getString();
        }
    }

    private static final class ResourceKeyParser {
        private static void dismiss(TerminalSavedData data, UUID id, TerminalNoticeDismissPayload payload) {
            data.dismissNotice(id, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(payload.dimension())), payload.pos());
        }
    }
}
