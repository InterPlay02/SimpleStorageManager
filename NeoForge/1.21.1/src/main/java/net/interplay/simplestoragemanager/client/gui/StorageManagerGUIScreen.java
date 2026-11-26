package net.interplay.simplestoragemanager.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import org.joml.Vector2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.interplay.simplestoragemanager.access.ConnectionStatus;
import net.interplay.simplestoragemanager.network.TerminalNoticeDismissPayload;
import net.interplay.simplestoragemanager.network.TerminalClearCraftingPayload;
import net.interplay.simplestoragemanager.network.TerminalStorageClickPayload;
import net.interplay.simplestoragemanager.network.TerminalShiftDragPreviewPayload;
import net.interplay.simplestoragemanager.network.TerminalShiftDragPreviewRequestPayload;
import net.interplay.simplestoragemanager.network.TerminalConnectionStatusPayload;
import net.interplay.simplestoragemanager.network.TerminalLockTogglePayload;
import net.interplay.simplestoragemanager.network.TerminalOrganizePayload;
import net.interplay.simplestoragemanager.network.TerminalRenamePayload;
import net.interplay.simplestoragemanager.network.TerminalViewPayload;
import net.interplay.simplestoragemanager.network.TerminalViewRequestPayload;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.logic.InventoryFilterLogic;
import net.interplay.simplestoragemanager.logic.TerminalScaleChoice;
import net.interplay.simplestoragemanager.logic.TerminalInsertionRules;
import net.interplay.simplestoragemanager.logic.TerminalTitleLogic;
import net.interplay.simplestoragemanager.logic.UpgradeSlotRules;
import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.interplay.simplestoragemanager.network.TerminalFilterSavePayload;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModScreens;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StorageManagerGUIScreen extends AbstractContainerScreen<StorageManagerGUIMenu>
        implements SimplestoragemanagerModScreens.ScreenAccessor {
    private static final ResourceLocation SMALL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/storage_manager_gui.png");
    private static final ResourceLocation BIG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/storage_manager_gui_big.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/slot.png");
    private static final ResourceLocation SCROLLER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/scroller.png");
    private static final ResourceLocation SCROLLER_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/scroller_disabled.png");
    private static final ResourceLocation CROSS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/cross_button.png");
    private static final ResourceLocation CROSS_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/cross_button_highlighted.png");
    private static final ResourceLocation LOCKED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/locked_button.png");
    private static final ResourceLocation LOCKED_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/locked_button_highlighted.png");
    private static final ResourceLocation UNLOCKED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/unlocked_button.png");
    private static final ResourceLocation UNLOCKED_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/unlocked_button_highlighted.png");
    private static final ResourceLocation REPORT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/report_button.png");
    private static final ResourceLocation REPORT_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/report_button_highlighted.png");
    private static final ResourceLocation ORGANIZER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/organizer.png");
    private static final ResourceLocation FILTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "simplestoragemanager", "textures/screens/filter.png");
    private static final int SLOT_SIZE = TerminalScreenLayout.SLOT_SIZE;
    private static final int SECTION_BUTTON_SIZE = 10;
    private static final int SECTION_BUTTON_NONE = 0;
    private static final int SECTION_BUTTON_ORGANIZE = 1;
    private static final int SECTION_BUTTON_FILTER = 2;
    private static final int SECTION_MENU_ORGANIZE_OPTION_COUNT = 4;
    private static final int FILTER_MENU_ITEM_COUNT = 2;
    private static final int SECTION_MENU_OFFSET_X = 10;
    private static final int SECTION_MENU_OFFSET_Y = 10;
    private static final String MENU_CHECK_MARK = "\u2714";
    private static final int MENU_TITLE_COLOR = 0xFFFFFFFF;
    private static final int MENU_OPTION_COLOR = 0xFFfb8f13;
    private static final int MENU_OPTION_COLOR_UNSELECTED = 0xFF808080;
    private static final String ADVANCED_MARK = " MkII";
    private static final float OWNER_NAME_SCALE = 0.5F;

    private final Player player;
    private TerminalScreenLayout.Variant variant = TerminalScreenLayout.SMALL;
    private EditBox searchBar;
    private EditBox titleEditBox;
    private int titleBoxHeight;
    private int editingSection = -1;
    private String editingBackupName = "";
    private final Map<Integer, TitleScrollAnimation> titleScrollAnimations = new HashMap<>();
    private int lastTitleClickSection = -1;
    private long lastTitleClickTime;
    private double lastTitleClickX;
    private double lastTitleClickY;
    private TerminalViewPayload view = new TerminalViewPayload(false, 0, 0, List.of(), List.of(), List.of());
    /** Cached layout entries built from the current view; rebuilt only when the view revision changes. */
    private List<ClientLayoutEntry> cachedLayoutEntries = null;
    private int cachedLayoutRevision = -1;
    /** Incremented whenever the terminal view data used for layout changes. */
    private int layoutRevision = 0;
    private final Long2ObjectOpenHashMap<ItemStack> remoteItems = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap remoteSlotLimits = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<UUID> remoteConnectionIds = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap remoteStorageSlots = new Long2IntOpenHashMap();
    private RemoteHit hoveredRemote;
    private boolean hoveredUnavailableIcon;
    private boolean hoveredNoticeDismiss;
    private int hoveredOrganizeSection = -1;
    private int hoveredFilterSection = -1;
    private boolean sectionMenuOpen;
    private int sectionMenuSourceButton = SECTION_BUTTON_NONE;
    private int sectionMenuSourceSection = -1;
    /** Selected organize option per section (section index -> option index). */
    private final Map<Integer, Integer> sectionSelectedOrganize = new HashMap<>();
    /** Sections currently in filter-edit mode (many may be edited at the same time). */
    private final Set<Integer> filterEditSections = new HashSet<>();
    /** Filter entries being edited for each section, transient and client side only. */
    private final Map<Integer, List<ItemStack>> filterEditItems = new HashMap<>();
    /** Filter mode being edited for each section (0 = allow, 1 = deny). */
    private final Map<Integer, Integer> filterSectionMode = new HashMap<>();
    private int sectionMenuX;
    private int sectionMenuY;
    private final ClientTooltipPositioner sectionMenuPositioner = new ClientTooltipPositioner() {
        @Override
        public Vector2i positionTooltip(int guiLeft, int guiTop, int mouseX, int mouseY, int width, int height) {
            int x = Math.max(4, Math.min(sectionMenuX, guiLeft - width - 4));
            int y = Math.max(4, Math.min(sectionMenuY, guiTop - height - 4));
            return new Vector2i(x, y);
        }
    };
    private boolean draggingScroller;
    private final List<TerminalStorageClickPayload.SlotTarget> remoteDragTargets = new ArrayList<>();
    private final Long2ObjectOpenHashMap<ItemStack> remotePreviewItems = new Long2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ItemStack> playerPreviewItems = new Int2ObjectOpenHashMap<>();
    private ItemStack remotePreviewCarried;
    private ItemStack remoteDragInitialCarried = ItemStack.EMPTY;
    private boolean remoteDragShift;
    private int remoteDragMode;
    private int itemDragButton = -1;
    private boolean itemDragShift;
    private boolean itemDragMoved;
    private boolean itemDragCreative;
    private int itemDragStartSlot = -1;
    private final Set<Integer> itemDragSlots = new LinkedHashSet<>();
    private long nextOperationSequence;
    private long pendingOperationSequence;
    private long lastAppliedOperationSequence;
    private long nextShiftPreviewRequestId;
    private long latestShiftPreviewRequestId;
    private long appliedShiftPreviewRequestId;
    private boolean shiftDragCommitPending;
    private final Int2ObjectOpenHashMap<ItemStack> visualPlayerPreviewItems = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ItemStack> visualRemotePreviewItems = new Long2ObjectOpenHashMap<>();
    private ItemStack visualRemotePreviewCarried;

    public StorageManagerGUIScreen(StorageManagerGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        player = inventory.player;
        imageWidth = TerminalScreenLayout.SMALL.imageWidth();
        imageHeight = TerminalScreenLayout.SMALL.imageHeight();
    }

    private int remoteSlotLimit(long slotKey, ItemStack stack) {
        return remoteSlotLimits.getOrDefault(slotKey, stack.getMaxStackSize());
    }

    private int measureTitleWidth(String text) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), Integer.MAX_VALUE);
        if (lines.isEmpty()) return 0;
        return font.width(lines.get(0));
    }

    private double animatedTitleOffset(int sectionIndex, int maxScroll) {
        if (maxScroll <= 0) {
            titleScrollAnimations.remove(sectionIndex);
            return 0.0;
        }
        TitleScrollAnimation animation = titleScrollAnimations.computeIfAbsent(sectionIndex,
                ignored -> new TitleScrollAnimation());
        if (animation.startedAtTick < 0L) {
            animation.startedAtTick = this.menu.world.getGameTime();
        }
        return TerminalTitleLogic.scrollOffset(this.menu.world.getGameTime(), animation.startedAtTick,
                TerminalTitleLogic.directionTicks(), TerminalTitleLogic.pauseTicks(), maxScroll);
    }

    private void drawClippedTitle(GuiGraphics graphics, Component label, int titleX, int titleY, double offset) {
        int boxTop = titleY - 1;
        graphics.enableScissor(titleX, boxTop, titleX + TerminalScreenLayout.TITLE_BOX_WIDTH,
                boxTop + titleBoxHeight);
        try {
            graphics.drawString(font, label, titleX + (int) Math.round(offset), titleY, 0xFFFFFFFF, false);
        } finally {
            graphics.disableScissor();
        }
    }

    private void updateTitleEditBoxRect(int sectionIndex) {
        if (view.sections().isEmpty()) return;
        if (sectionIndex < 0 || sectionIndex >= view.sections().size()) return;
        TerminalScreenLayout.Rect area = variant.contentArea();
        int cursor = area.top() + 2 - view.scrollOffset();
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                cursor += sectionHeight(entry.notice().slotCount());
                continue;
            }
            if (entry.sectionIndex() == sectionIndex) {
                titleEditBox.setX(leftPos + area.left() + 4);
                titleEditBox.setY(topPos + cursor + 2);
                titleEditBox.setWidth(TerminalScreenLayout.TITLE_BOX_WIDTH);
                titleEditBox.setHeight(titleBoxHeight);
                return;
            }
            TerminalViewPayload.Section section = entry.section();
            int slotY = cursor + 12;
            int bottom = slotY + Math.max(1, (section.slotCount() + 8) / 9) * SLOT_SIZE + 2;
            cursor = bottom + 4;
        }
    }

    private void startTitleEdit(int sectionIndex) {
        if (view.sections().isEmpty()) return;
        if (sectionIndex < 0 || sectionIndex >= view.sections().size()) return;
        editingSection = sectionIndex;
        editingBackupName = view.sections().get(sectionIndex).label().getString();
        if (titleEditBox == null) return;
        titleEditBox.setValue(editingBackupName);
        titleEditBox.setCursorPosition(editingBackupName.length());
        lastTitleClickSection = -1;
        titleEditBox.setVisible(true);
        this.setFocused(titleEditBox);
    }

    private void confirmTitleEdit() {
        if (editingSection < 0) return;
        int sectionIndex = editingSection;
        String newName = titleEditBox == null ? editingBackupName : titleEditBox.getValue();
        if (newName == null) newName = "";
        editingSection = -1;
        if (titleEditBox != null) {
            titleEditBox.setValue("");
            titleEditBox.setVisible(false);
        }
        this.setFocused(null);
        setSectionLabel(sectionIndex, Component.literal(newName));
        PacketDistributor.sendToServer(new TerminalRenamePayload(sectionIndex, newName));
    }

    private void cancelTitleEdit() {
        if (editingSection < 0) return;
        int sectionIndex = editingSection;
        editingSection = -1;
        if (titleEditBox != null) {
            titleEditBox.setValue("");
            titleEditBox.setVisible(false);
        }
        this.setFocused(null);
        setSectionLabel(sectionIndex, Component.literal(editingBackupName));
    }

    private void setSectionLabel(int sectionIndex, Component label) {
        if (view.sections().isEmpty()) return;
        if (sectionIndex < 0 || sectionIndex >= view.sections().size()) return;
        List<TerminalViewPayload.Section> sections = new ArrayList<>(view.sections());
        TerminalViewPayload.Section current = sections.get(sectionIndex);
        sections.set(sectionIndex, new TerminalViewPayload.Section(label, current.coordinate(), current.status(),
                current.slotCount(), current.filterItems(), current.filterMode()));
        view = new TerminalViewPayload(view.big(), view.scrollOffset(), view.maxScroll(), sections,
                view.visibleSlots(), view.notices(), view.locked(), view.originTargets(), view.operationSequence(),
                view.playerSlots(), view.canMutateInventories(), view.advanced(), view.ownerName());
        layoutRevision++;
    }

    private int titleSectionAt(double mouseX, double mouseY) {
        TerminalScreenLayout.Rect area = variant.contentArea();
        // Somente cliques dentro da área de conteúdo podem atingir caixas de título.
        // Impede que títulos escondidos fora do viewport bloqueiem cliques no resto da GUI
        // (ex.: inventário principal do player logo abaixo).
        if (!isInside(area, mouseX, mouseY)) return -1;
        int cursor = area.top() + 2 - view.scrollOffset();
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                cursor += sectionHeight(entry.notice().slotCount());
                continue;
            }
            TerminalViewPayload.Section section = entry.section();
            int top = cursor;
            int slotY = top + 12;
            int bottom = slotY + Math.max(1, (section.slotCount() + 8) / 9) * SLOT_SIZE + 2;
            int titleX = leftPos + area.left() + 4;
            int titleY = topPos + top + 2;
            // Ignore seções cujo título está completamente fora da área visível
            // (acima ou abaixo do viewport por causa do rolagem).
            if (titleY + titleBoxHeight <= topPos + area.top()
                    || titleY >= topPos + area.bottom()) {
                cursor = bottom + 4;
                continue;
            }
            if (mouseX >= titleX && mouseX < titleX + TerminalScreenLayout.TITLE_BOX_WIDTH
                    && mouseY >= titleY && mouseY < titleY + titleBoxHeight) {
                return entry.sectionIndex();
            }
            cursor = bottom + 4;
        }
        return -1;
    }

    private void handleTitleClick(int sectionIndex, double mouseX, double mouseY, int button) {
        if (button != 0) return;
        if (isFilterEditing(sectionIndex)) return;
        long now = System.currentTimeMillis();
        boolean doubleClick = sectionIndex == lastTitleClickSection
                && now - lastTitleClickTime < 500L
                && Math.abs(mouseX - lastTitleClickX) < 3.0
                && Math.abs(mouseY - lastTitleClickY) < 3.0;
        lastTitleClickSection = sectionIndex;
        lastTitleClickTime = now;
        lastTitleClickX = mouseX;
        lastTitleClickY = mouseY;
        if (doubleClick) startTitleEdit(sectionIndex);
    }

    private int[] sectionButtonRects(int top) {
        TerminalScreenLayout.Rect main = variant.mainArea();
        int buttonY = topPos + top + 2;
        int organizeButtonRight = leftPos + main.right() - 2;
        int organizeButtonLeft = organizeButtonRight - SECTION_BUTTON_SIZE;
        int filterButtonRight = organizeButtonLeft - 2;
        int filterButtonLeft = filterButtonRight - SECTION_BUTTON_SIZE;
        return new int[]{filterButtonLeft, filterButtonRight, organizeButtonLeft, organizeButtonRight, buttonY};
    }

    private SectionButtonHit sectionButtonHit(double mouseX, double mouseY) {
        TerminalScreenLayout.Rect area = variant.contentArea();
        if (!isInside(area, mouseX, mouseY)) return null;
        if (!view.advanced()) return null;
        int cursor = area.top() + 2 - view.scrollOffset();
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                cursor += sectionHeight(entry.notice().slotCount());
                continue;
            }
            TerminalViewPayload.Section section = entry.section();
            int rows = Math.max(1, (section.slotCount() + 8) / 9);
            int top = cursor;
            int slotY = top + 12;
            int bottom = slotY + rows * SLOT_SIZE + 2;
            // Os botões só existem quando o inventário está disponível.
            if (section.connectionStatus() == ConnectionStatus.AVAILABLE) {
                int[] rects = sectionButtonRects(top);
                int filterButtonLeft = rects[0], filterButtonRight = rects[1];
                int organizeButtonLeft = rects[2], organizeButtonRight = rects[3], buttonY = rects[4];

                boolean organizeHovered = mouseX >= organizeButtonLeft && mouseX < organizeButtonRight
                        && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;
                boolean filterHovered = mouseX >= filterButtonLeft && mouseX < filterButtonRight
                        && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;

                if (organizeHovered) return new SectionButtonHit(entry.sectionIndex(), SECTION_BUTTON_ORGANIZE);
                if (filterHovered) return new SectionButtonHit(entry.sectionIndex(), SECTION_BUTTON_FILTER);
            }
            cursor = bottom + 4;
        }
        return null;
    }

    private record SectionButtonHit(int section, int kind) { }

    private void handleSectionButtonClick(int button) {
        if (button != 0) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        // No additional functionality - just play the click sound
    }

    private void openSectionMenu(int sourceButton, int sourceSection, double mouseX, double mouseY) {
        sectionMenuOpen = true;
        sectionMenuSourceButton = sourceButton;
        sectionMenuSourceSection = sourceSection;
        sectionMenuX = (int) Math.round(mouseX) + SECTION_MENU_OFFSET_X;
        sectionMenuY = (int) Math.round(mouseY) + SECTION_MENU_OFFSET_Y;
    }

    private void closeSectionMenu() {
        sectionMenuOpen = false;
    }

    private void toggleFilterEditMode(int section) {
        if (section < 0 || section >= view.sections().size()) return;
        if (view.sections().get(section).connectionStatus() != ConnectionStatus.AVAILABLE) return;
        if (filterEditSections.contains(section)) {
            saveFilter(section);
        } else {
            startFilterEdit(section);
        }
    }

    private void startFilterEdit(int section) {
        TerminalViewPayload.Section sectionView = view.sections().get(section);
        filterEditSections.add(section);
        List<ItemStack> working = new ArrayList<>(sectionView.filterItems().size());
        for (ItemStack stack : sectionView.filterItems()) working.add(stack.copy());
        filterEditItems.put(section, working);
        filterSectionMode.putIfAbsent(section, sectionView.filterAllow() ? 0 : 1);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void saveFilter(int section) {
        TerminalViewPayload.Section sectionView = view.sections().get(section);
        List<ItemStack> saved = sectionView.filterItems();
        List<ItemStack> current = filterEditItems.getOrDefault(section, List.of());
        int savedMode = sectionView.filterAllow() ? 0 : 1;
        int currentMode = filterSectionMode.getOrDefault(section, savedMode);
        // Sem alterações nos itens nem na opção do dropdown: apenas desativa, nada a salvar.
        if (filterListsEqual(current, saved) && currentMode == savedMode) {
            filterEditSections.remove(section);
            filterEditItems.remove(section);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }
        PacketDistributor.sendToServer(new TerminalFilterSavePayload(section, currentMode,
                current.stream().map(ItemStack::copy).filter(stack -> !stack.isEmpty()).toList()));
        filterEditSections.remove(section);
        filterEditItems.remove(section);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean filterListsEqual(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ItemStack left = a.get(i);
            ItemStack right = b.get(i);
            if (left.isEmpty() && right.isEmpty()) continue;
            if (left.isEmpty() || right.isEmpty()) return false;
            if (left.getCount() != right.getCount()) return false;
            if (!InventoryFilterLogic.matchesFilterItem(left, right)) return false;
        }
        return true;
    }

    private void handleFilterSlotClick(int section, int slot, int button) {
        if (button != 0 && button != 1) return;
        if (!filterEditSections.contains(section)) return;
        ItemStack carried = menu.getCarried();
        List<ItemStack> items = new ArrayList<>(filterEditItems.getOrDefault(section, List.of()));
        while (items.size() <= slot) items.add(ItemStack.EMPTY);
        boolean slotHas = !items.get(slot).isEmpty();
        if (carried.isEmpty()) {
            if (slotHas) items.set(slot, ItemStack.EMPTY);
        } else {
            // Cria uma cópia de uma unidade sem consumir o cursor nem tocar no inventário físico.
            items.set(slot, InventoryFilterLogic.filterCopy(carried));
        }
        filterEditItems.put(section, items);
    }

    private void clearFilterSlot(int section, int slot) {
        if (!filterEditSections.contains(section)) return;
        List<ItemStack> items = filterEditItems.get(section);
        if (items == null) return;
        while (items.size() <= slot) items.add(ItemStack.EMPTY);
        items.set(slot, ItemStack.EMPTY);
        filterEditItems.put(section, items);
    }

    private boolean isFilterEditing(int section) {
        return filterEditSections.contains(section);
    }

    private ItemStack filterSlotStack(int section, int slot) {
        List<ItemStack> items = filterEditItems.get(section);
        if (items == null || slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        return items.get(slot);
    }

    private void selectSectionMenuOption(int sourceButton, int option) {
        if (sourceButton == SECTION_BUTTON_ORGANIZE) {
            sectionSelectedOrganize.put(sectionMenuSourceSection, option);
        } else if (sectionMenuSourceSection >= 0) {
            filterSectionMode.put(sectionMenuSourceSection, option);
        }
        sectionMenuOpen = false;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private int sectionMenuSelectedOrganizeFor(int section) {
        Integer selected = sectionSelectedOrganize.get(section);
        return selected == null ? 0 : selected;
    }

    private int sectionMenuSelectedFor(int sourceButton) {
        if (sourceButton == SECTION_BUTTON_ORGANIZE) {
            // Primeira opção é o padrão quando nada foi selecionado ainda.
            return sectionMenuSelectedOrganizeFor(sectionMenuSourceSection);
        }
        int section = sectionMenuSourceSection;
        if (section >= 0 && section < view.sections().size()) {
            Integer edited = filterSectionMode.get(section);
            if (edited != null) return edited;
            return view.sections().get(section).filterAllow() ? 0 : 1;
        }
        return 0;
    }

    private int sectionMenuLineCount(int sourceButton) {
        if (sourceButton == SECTION_BUTTON_ORGANIZE) {
            // A primeira linha é o título ("Organizar por:"), não selecionável; as demais são as opções.
            return SECTION_MENU_ORGANIZE_OPTION_COUNT + 1;
        }
        // A primeira linha é o título ("Tipo de Filtro:"), não selecionável; as demais são as opções.
        return FILTER_MENU_ITEM_COUNT + 1;
    }

    private boolean sectionMenuLineSelectable(int sourceButton, int line) {
        // A primeira linha (título) de qualquer menu não é selecionável; as demais são opções.
        return line > 0;
    }

    private MutableComponent sectionMenuLine(int sourceButton, int line) {
        if (sourceButton == SECTION_BUTTON_ORGANIZE && line == 0) {
            return Component.translatable("gui.simplestoragemanager.storage_manager_gui.organize_by")
                    .withStyle(Style.EMPTY.withColor(MENU_TITLE_COLOR));
        }
        if (sourceButton == SECTION_BUTTON_FILTER && line == 0) {
            return Component.translatable("gui.simplestoragemanager.storage_manager_gui.filter_type")
                    .withStyle(Style.EMPTY.withColor(MENU_TITLE_COLOR));
        }
        int option = line - 1;
        MutableComponent text = Component.translatable(sectionMenuTranslationKey(sourceButton, option));
        int spaceWidth = font.width(" ");
        int checkmarkWidth = font.width(Component.literal(MENU_CHECK_MARK));
        int reservedWidth = checkmarkWidth + spaceWidth;
        int selected = sectionMenuSelectedFor(sourceButton);
        boolean isSelected = option == selected;
        // Checkmark em negrito apenas na opção selecionada.
        String prefix = isSelected
                ? MENU_CHECK_MARK + " "
                : " ".repeat(Math.max(1, (int) Math.ceil((double) reservedWidth / spaceWidth)));
        MutableComponent prefixComponent = Component.literal(prefix);
        if (isSelected) {
            prefixComponent = prefixComponent.withStyle(Style.EMPTY.withBold(true));
        }
        MutableComponent lineComponent = prefixComponent.append(text);
        int color = isSelected ? MENU_OPTION_COLOR : MENU_OPTION_COLOR_UNSELECTED;
        return lineComponent.withStyle(Style.EMPTY.withColor(color));
    }

    private String sectionMenuTranslationKey(int sourceButton, int option) {
        if (sourceButton == SECTION_BUTTON_ORGANIZE) {
            return switch (option) {
                case 0 -> "gui.simplestoragemanager.storage_manager_gui.organize_by_name";
                case 1 -> "gui.simplestoragemanager.storage_manager_gui.organize_by_quantity";
                case 2 -> "gui.simplestoragemanager.storage_manager_gui.organize_by_item_type";
                case 3 -> "gui.simplestoragemanager.storage_manager_gui.organize_by_mod";
                default -> "";
            };
        }
        return switch (option) {
            case 0 -> "gui.simplestoragemanager.storage_manager_gui.filter_allow";
            case 1 -> "gui.simplestoragemanager.storage_manager_gui.filter_deny";
            default -> "";
        };
    }

    private int sectionMenuOptionAt(double mouseX, double mouseY, int sourceButton) {
        List<MutableComponent> lines = new ArrayList<>();
        int widest = 0;
        for (int line = 0; line < sectionMenuLineCount(sourceButton); line++) {
            if (!sectionMenuLineSelectable(sourceButton, line)) continue;
            MutableComponent menuLine = sectionMenuLine(sourceButton, line);
            lines.add(menuLine);
            widest = Math.max(widest, font.width(menuLine));
        }
        int menuWidth = Math.max(1, widest + 4);
        int lineHeight = font.lineHeight;
        for (int line = 0; line < sectionMenuLineCount(sourceButton); line++) {
            if (!sectionMenuLineSelectable(sourceButton, line)) continue;
            MutableComponent menuLine = sectionMenuLine(sourceButton, line);
            List<net.minecraft.util.FormattedCharSequence> split = font.split(menuLine, menuWidth);
            if (!split.isEmpty()) {
                int left = sectionMenuX;
                int top = sectionMenuY + line * lineHeight;
                int right = left + font.width(split.get(0));
                int bottom = top + lineHeight;
                // O último pixel vertical de cada linha ainda pertence a essa linha,
                // então o hit-test é inclusivo no eixo Y (mouseY <= bottom).
                if (mouseX >= left && mouseX < right && mouseY >= top && mouseY <= bottom) {
                    return line - 1;
                }
            }
        }
        return -1;
    }

    private void renderSectionMenu(GuiGraphics graphics, int mouseX, int mouseY, int sourceButton) {
        List<MutableComponent> lines = new ArrayList<>();
        int widest = 0;
        for (int line = 0; line < sectionMenuLineCount(sourceButton); line++) {
            MutableComponent menuLine = sectionMenuLine(sourceButton, line);
            lines.add(menuLine);
            widest = Math.max(widest, font.width(menuLine));
        }
        List<net.minecraft.util.FormattedCharSequence> rendered = new ArrayList<>();
        int menuWidth = Math.max(1, widest + 4);
        for (MutableComponent line : lines) {
            rendered.addAll(font.split(line, menuWidth));
        }
        graphics.renderTooltip(font, rendered, sectionMenuPositioner, mouseX, mouseY);
    }

    private boolean handleSectionMenuClick(double mouseX, double mouseY, int button, int sourceButton) {
        int option = sectionMenuOptionAt(mouseX, mouseY, sourceButton);
        if (option >= 0 && (button == 0 || button == 1)) {
            selectSectionMenuOption(sourceButton, option);
            return true;
        }
        closeSectionMenu();
        return true;
    }

    @Override
    protected void init() {
        int configuredScale = Minecraft.getInstance().options.guiScale().get();
        boolean useBig = TerminalScaleChoice.useBig(configuredScale);
        variant = useBig ? TerminalScreenLayout.BIG : TerminalScreenLayout.SMALL;
        imageWidth = variant.imageWidth();
        imageHeight = variant.imageHeight();
        titleLabelX = variant.mainArea().left();
        super.init();
        searchBar = new EditBox(font, leftPos + variant.searchArea().left(), topPos + variant.searchArea().top(),
                variant.searchArea().width(), variant.searchArea().height(),
                Component.translatable("gui.simplestoragemanager.storage_manager_gui.search_bar"));
        searchBar.setBordered(false);
        searchBar.setTextColor(0xFFFFFF);
        searchBar.setTextColorUneditable(0xFFFFFF);
        searchBar.setMaxLength(256);
        searchBar.setResponder(ignored -> requestView(0));
        addRenderableWidget(searchBar);
        titleBoxHeight = font.lineHeight + 2;
        titleEditBox = new EditBox(font, 0, 0, TerminalScreenLayout.TITLE_BOX_WIDTH, titleBoxHeight, Component.empty());
        titleEditBox.setBordered(false);
        titleEditBox.setTextColor(0xFFFFFF);
        titleEditBox.setTextColorUneditable(0xFFFFFF);
        titleEditBox.setMaxLength(256);
        titleEditBox.setResponder(ignored -> { });
        titleEditBox.setVisible(false);
        addRenderableWidget(titleEditBox);
        requestView(0);
    }

    private void requestView(int scroll) {
        int clamped = Math.max(0, Math.min(view.maxScroll(), scroll));
        PacketDistributor.sendToServer(new TerminalViewRequestPayload(searchBar == null ? "" : searchBar.getValue(),
                clamped, variant == TerminalScreenLayout.BIG));
    }

    public void applyView(TerminalViewPayload payload) {
        if (payload.operationSequence() < pendingOperationSequence
                || payload.operationSequence() < lastAppliedOperationSequence) return;
        if (pendingOperationSequence > 0 && payload.operationSequence() == pendingOperationSequence
                && payload.playerSlots().isEmpty()) return;
        if (itemDragShift || shiftDragCommitPending) cancelPendingShiftDrag();
        view = payload;
        lastAppliedOperationSequence = Math.max(lastAppliedOperationSequence, payload.operationSequence());
        layoutRevision++;
        remoteItems.clear();
        remoteSlotLimits.clear();
        remoteConnectionIds.clear();
        remoteStorageSlots.clear();
        for (TerminalViewPayload.VisibleSlot slot : payload.visibleSlots()) {
            long slotKey = key(slot.section(), slot.slot());
            remoteItems.put(slotKey, slot.stack());
            remoteSlotLimits.put(slotKey, slot.slotLimit());
            remoteConnectionIds.put(slotKey, slot.connectionId());
            remoteStorageSlots.put(slotKey, slot.storageSlot());
        }
        applyOriginSlotLimits(payload);
        if (pendingOperationSequence > 0 && payload.operationSequence() >= pendingOperationSequence
                && !payload.playerSlots().isEmpty()) {
            for (TerminalViewPayload.PlayerSlot slot : payload.playerSlots()) {
                if (slot.slot() >= 0 && slot.slot() < menu.slots.size()) {
                    menu.slots.get(slot.slot()).set(slot.stack());
                }
            }
            pendingOperationSequence = 0;
            clearRemotePreview();
        } else if (pendingOperationSequence == 0 && itemDragButton < 0) {
            clearRemotePreview();
        }
    }

    private void applyOriginSlotLimits(TerminalViewPayload payload) {
        for (TerminalViewPayload.OriginTarget target : payload.originTargets()) {
            if (target.section() >= 0 && target.slot() >= 0 && target.slotLimit() > 0) {
                remoteSlotLimits.put(key(target.section(), target.slot()), target.slotLimit());
            }
        }
    }

    public void applyConnectionStatus(TerminalConnectionStatusPayload payload) {
        if (itemDragShift || shiftDragCommitPending) cancelPendingShiftDrag();
        int sectionIndex = payload.section();
        if (sectionIndex < 0 || sectionIndex >= view.sections().size()) {
            if (payload.refreshView()) requestView(view.scrollOffset());
            return;
        }
        List<TerminalViewPayload.Section> sections = new ArrayList<>(view.sections());
        TerminalViewPayload.Section current = sections.get(sectionIndex);
        sections.set(sectionIndex, new TerminalViewPayload.Section(payload.label(), current.coordinate(),
                payload.status(), current.slotCount(), current.filterItems(), current.filterMode()));
        List<TerminalViewPayload.VisibleSlot> visibleSlots = new ArrayList<>();
        for (TerminalViewPayload.VisibleSlot slot : view.visibleSlots()) {
            if (slot.section() != sectionIndex) visibleSlots.add(slot);
        }
        visibleSlots.addAll(payload.visibleSlots());
        view = new TerminalViewPayload(view.big(), view.scrollOffset(), view.maxScroll(),
                sections, visibleSlots, view.notices(), view.locked(), view.originTargets(),
                view.operationSequence(), view.playerSlots(), view.canMutateInventories(),
                view.advanced(), view.ownerName());
        layoutRevision++;
        remoteItems.keySet().removeIf(key -> (int) (key >> 32) == sectionIndex);
        remoteSlotLimits.keySet().removeIf(key -> (int) (key >> 32) == sectionIndex);
        remoteConnectionIds.keySet().removeIf(key -> (int) (key >> 32) == sectionIndex);
        remoteStorageSlots.keySet().removeIf(key -> (int) (key >> 32) == sectionIndex);
        for (TerminalViewPayload.VisibleSlot slot : payload.visibleSlots()) {
            long slotKey = key(slot.section(), slot.slot());
            remoteItems.put(slotKey, slot.stack());
            remoteSlotLimits.put(slotKey, slot.slotLimit());
            remoteConnectionIds.put(slotKey, slot.connectionId());
            remoteStorageSlots.put(slotKey, slot.storageSlot());
        }
        applyOriginSlotLimits(view);
        if (payload.refreshView()) requestView(view.scrollOffset());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(variant == TerminalScreenLayout.BIG ? BIG_TEXTURE : SMALL_TEXTURE,
                leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        renderPlayer(graphics, mouseX, mouseY);
        TerminalScreenLayout.Rect area = variant.contentArea();
        int clipLeft = leftPos + area.left();
        int clipTop = topPos + area.top();
        int clipRight = leftPos + area.right() + 1;
        int clipBottom = topPos + area.bottom() + 1;
        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        drawRemoteSections(graphics, mouseX, mouseY);
        graphics.disableScissor();

        boolean enabled = view.maxScroll() > 0 && !view.sections().isEmpty();
        ResourceLocation scroller = enabled ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
        int travel = variant.scrollTrack().height() - 15;
        int scrollerY = variant.scrollTrack().top();
        if (enabled && view.maxScroll() > 0) {
            scrollerY += (int) ((double) view.scrollOffset() / view.maxScroll() * travel);
        }
        graphics.blit(scroller, leftPos + variant.scrollTrack().left(), topPos + scrollerY,
                0, 0, 12, 15, 12, 15);
        renderStaticItems(graphics);
        renderCrafting(graphics);
        ResourceLocation cross = isPointInside(variant.cross(), TerminalScreenLayout.BUTTON_SIZE, mouseX, mouseY)
                ? CROSS_HIGHLIGHTED_TEXTURE : CROSS_TEXTURE;
        graphics.blit(cross, leftPos + variant.cross().x(), topPos + variant.cross().y(),
                0, 0, TerminalScreenLayout.BUTTON_SIZE, TerminalScreenLayout.BUTTON_SIZE,
                TerminalScreenLayout.BUTTON_SIZE, TerminalScreenLayout.BUTTON_SIZE);
        boolean lockHovered = isPointInside(variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE,
                mouseX, mouseY);
        ResourceLocation lockTexture = view.locked()
                ? (lockHovered ? LOCKED_HIGHLIGHTED_TEXTURE : LOCKED_TEXTURE)
                : (lockHovered ? UNLOCKED_HIGHLIGHTED_TEXTURE : UNLOCKED_TEXTURE);
        graphics.blit(lockTexture, leftPos + variant.lockButton().x(), topPos + variant.lockButton().y(),
                TerminalScreenLayout.LOCK_BUTTON_SIZE, TerminalScreenLayout.LOCK_BUTTON_SIZE,
                0, 0, 20, 20, 20, 20);
    }

    private void renderPlayer(GuiGraphics graphics, int mouseX, int mouseY) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                leftPos + variant.playerPortrait().left(), topPos + variant.playerPortrait().top(),
                leftPos + variant.playerPortrait().right(), topPos + variant.playerPortrait().bottom(),
                30, 0.0625F, mouseX, mouseY, player);
    }

    private void drawRemoteSections(GuiGraphics graphics, int mouseX, int mouseY) {
        TerminalScreenLayout.Rect area = variant.contentArea();
        int cursor = area.top() + 2 - view.scrollOffset();
        int viewportTop = area.top();
        int viewportBottom = area.bottom();
        hoveredRemote = null;
        hoveredUnavailableIcon = false;
        hoveredNoticeDismiss = false;
        hoveredOrganizeSection = -1;
        hoveredFilterSection = -1;
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                int bottom = cursor + sectionHeight(entry.notice().slotCount()) - 4;
                if (bottom > viewportTop && cursor < viewportBottom) {
                    drawReplacementNotice(graphics, entry.notice(), area.left(), cursor, area.right(), bottom,
                            mouseX, mouseY);
                }
                cursor = bottom + 4;
                continue;
            }
            int index = entry.sectionIndex();
            TerminalViewPayload.Section section = entry.section();
            int rows = Math.max(1, (section.slotCount() + 8) / 9);
            int top = cursor;
            int slotY = top + 12;
            int bottom = slotY + rows * SLOT_SIZE + 2;
            if (bottom <= viewportTop || top >= viewportBottom) {
                // Etapa 3: section completely outside the visible content area -> visual culling.
                cursor = bottom + 4;
                continue;
            }
            boolean unavailable = section.connectionStatus() != ConnectionStatus.AVAILABLE;
            drawDottedFrame(graphics, area.left(), top, area.right(), bottom,
                    unavailable ? 0xFFFF9B45 : 0xFFD0D0D0);
            boolean filterEditing = isFilterEditing(index);
            if (filterEditing) {
                // O título fica travado como "Editor de Filtro de Itens" enquanto edita o filtro.
                drawClippedTitle(graphics,
                        Component.translatable("gui.simplestoragemanager.storage_manager_gui.filter_editor_title"),
                        leftPos + area.left() + 4, topPos + top + 2, 0.0);
            } else if (editingSection == index) {
                updateTitleEditBoxRect(index);
            } else {
                String titleText = section.label().getString();
                int textWidth = measureTitleWidth(titleText);
                int maxScroll = TerminalTitleLogic.scrollRange(textWidth, TerminalScreenLayout.TITLE_BOX_WIDTH);
                double offset = animatedTitleOffset(index, maxScroll);
                int titleX = leftPos + area.left() + 4;
                int titleY = topPos + top + 2;
                drawClippedTitle(graphics, section.label(), titleX, titleY, offset);
            }

            for (int slot = 0; slot < section.slotCount(); slot++) {
                int x = area.left() + (slot % 9) * SLOT_SIZE;
                int y = slotY + (slot / 9) * SLOT_SIZE;
                if (y + SLOT_SIZE > viewportTop && y < viewportBottom) {
                    graphics.blit(SLOT_TEXTURE, leftPos + x, topPos + y, 0, 0, 18, 18, 18, 18);
                    ItemStack stack;
                    if (filterEditing) {
                        stack = filterSlotStack(index, slot);
                    } else {
                        stack = visualRemotePreviewItems.getOrDefault(key(index, slot),
                                remoteItems.getOrDefault(key(index, slot), ItemStack.EMPTY));
                    }
                    if (!stack.isEmpty()) {
                        int itemX = leftPos + x + TerminalScreenLayout.ITEM_OFFSET;
                        int itemY = topPos + y + TerminalScreenLayout.ITEM_OFFSET;
                        graphics.renderItem(stack, itemX, itemY);
                        renderItemDecorations(graphics, stack, itemX, itemY);
                    }
                    if (isInside(area, mouseX, mouseY)
                            && mouseX >= leftPos + x && mouseX < leftPos + x + 18
                            && mouseY >= topPos + y && mouseY < topPos + y + 18) {
                        hoveredRemote = new RemoteHit(index, slot, stack);
                    }
                }
            }
            if (unavailable) {
                drawUnavailable(graphics, section, area.left(), top, area.right(), bottom, mouseX, mouseY);
            }

            if (!unavailable && view.advanced()) {
                // Render section buttons (Organize and Filter) on the same height as the title,
                // at the top-right of the section, 2px from the main area's right edge.
                // Hidden entirely (and not clickable) when the inventory is unavailable
                // or the terminal is in basic mode.
                int[] rects = sectionButtonRects(top);
                int filterButtonLeft = rects[0], filterButtonRight = rects[1];
                int organizeButtonLeft = rects[2], organizeButtonRight = rects[3], buttonY = rects[4];

                boolean organizeHovered = mouseX >= organizeButtonLeft && mouseX < organizeButtonRight
                        && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;
                boolean filterHovered = mouseX >= filterButtonLeft && mouseX < filterButtonRight
                        && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;

                if (organizeHovered) hoveredOrganizeSection = index;
                if (filterHovered) hoveredFilterSection = index;

                // Scale the whole 18x18 source texture to 10x10 (same pattern as the lock/alert buttons).
                graphics.blit(ORGANIZER_TEXTURE, organizeButtonLeft, buttonY,
                        SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, 0, 0, 18, 18, 18, 18);
                graphics.blit(FILTER_TEXTURE, filterButtonLeft, buttonY,
                        SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, 0, 0, 18, 18, 18, 18);
            }
            cursor = bottom + 4;
        }
        if (view.sections().isEmpty() && view.notices().isEmpty()) {
            Component message = Component.translatable("screen.simplestoragemanager.no_inventories");
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(message, Math.max(1, area.width() - 12));
            int lineHeight = font.lineHeight;
            int firstY = area.top() + Math.max(0, (area.height() - lines.size() * lineHeight) / 2);
            int centerX = leftPos + (area.left() + area.right()) / 2;
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(font, lines.get(i), centerX, topPos + firstY + i * lineHeight, 0xFFD0D0D0);
            }
        }
    }

    private void drawUnavailable(GuiGraphics graphics, TerminalViewPayload.Section section,
                                 int left, int top, int right, int bottom, int mouseX, int mouseY) {
        int sectionWidth = right - left + 1;
        int sectionHeight = bottom - top + 1;
        int background = 0xE015120F;
        graphics.fill(leftPos + left, topPos + top, leftPos + right + 1, topPos + bottom + 1, background);
        graphics.enableScissor(leftPos + left, topPos + top, leftPos + right + 1, topPos + bottom + 1);
        for (int stripe = -sectionHeight / 3; stripe < sectionWidth; stripe += 10) {
            for (int y = top; y <= bottom; y += 3) {
                int x = left + stripe + (y - top) / 3;
                graphics.fill(leftPos + x, topPos + y, leftPos + x + 3,
                        topPos + Math.min(y + 3, bottom + 1), 0xFFFF9B45);
            }
        }
        graphics.disableScissor();

        String key = switch (section.connectionStatus()) {
            case BLOCK_REMOVED -> "screen.simplestoragemanager.connection_lost";
            case CHUNK_UNLOADED -> "screen.simplestoragemanager.chunk_unloaded";
            case DIMENSION_MISSING -> "screen.simplestoragemanager.dimension_missing";
            case BASIC_UNAVAILABLE -> "screen.simplestoragemanager.connection_lost";
            case AVAILABLE -> "screen.simplestoragemanager.connection_lost";
        };
        Component message = Component.translatable(key, section.coordinate());
        int maxWidth = Math.max(1, right - left - 12);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(message, maxWidth);
        int lineHeight = 9;
        int firstY = top + Math.max(0, ((bottom - top + 1) - lines.size() * lineHeight) / 2);
        int textWidth = 0;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int textLeft = left + (sectionWidth - Math.min(sectionWidth, textWidth + 8)) / 2;
        int textRight = textLeft + Math.min(sectionWidth, textWidth + 8);
        graphics.fill(leftPos + textLeft, topPos + firstY - 2, leftPos + textRight,
                topPos + firstY + lines.size() * lineHeight + 1, background);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(font, lines.get(i), leftPos + (left + right) / 2,
                    topPos + firstY + i * lineHeight, 0xFFFF9B45);
        }
        drawDottedFrame(graphics, left, top, right, bottom, 0xFFFF9B45);
        int iconSize = TerminalScreenLayout.ALERT_ICON_SIZE;
        int iconX = right - iconSize - 1;
        int iconY = top + 1;
        boolean hovered = isPointInside(new TerminalScreenLayout.Point(iconX, iconY), iconSize, mouseX, mouseY);
        hoveredUnavailableIcon |= hovered;
        ResourceLocation icon = hovered ? REPORT_HIGHLIGHTED_TEXTURE : REPORT_TEXTURE;
        graphics.blit(icon, leftPos + iconX, topPos + iconY, iconSize, iconSize,
                0, 0, 20, 20, 20, 20);
    }

    private void drawReplacementNotice(GuiGraphics graphics, TerminalViewPayload.Notice notice,
                                       int left, int top, int right, int bottom, int mouseX, int mouseY) {
        graphics.fill(leftPos + left + 2, topPos + top, leftPos + right,
                topPos + bottom + 1, 0xE01B4D27);
        drawDottedFrame(graphics, left, top, right, bottom, 0xFFB5FFBC);
        Component message = Component.translatable("screen.simplestoragemanager.replacement_notice",
                notice.pos().getX(), notice.pos().getY(), notice.pos().getZ());
        int maxWidth = Math.max(1, right - left - 16);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(message, maxWidth);
        int lineHeight = 9;
        int firstY = top + Math.max(0, ((bottom - top + 1) - lines.size() * lineHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(font, lines.get(i), leftPos + (left + right) / 2,
                    topPos + firstY + i * lineHeight, 0xFFB5FFBC);
        }
        int crossX = right - 9;
        int crossY = top + 2;
        boolean hovered = mouseX >= leftPos + crossX && mouseX < leftPos + crossX + 7
                && mouseY >= topPos + crossY && mouseY < topPos + crossY + 7;
        hoveredNoticeDismiss |= hovered;
        ResourceLocation cross = hovered
                ? CROSS_HIGHLIGHTED_TEXTURE : CROSS_TEXTURE;
        graphics.blit(cross, leftPos + crossX, topPos + crossY, 7, 7,
                0, 0, 14, 14, 14, 14);
    }

    private void renderStaticItems(GuiGraphics graphics) {
        for (int i = 0; i < 4; i++) renderSlotItem(graphics, menu.slots.get(menu.armorStart + i), variant.armor()[i]);
        renderSlotItem(graphics, menu.slots.get(menu.offhandIndex), variant.offhand());
        renderSlotItem(graphics, menu.slots.get(menu.upgradeStart), variant.upgrade3());
        renderSlotItem(graphics, menu.slots.get(menu.upgradeStart + 1), variant.upgrade2());
        renderSlotItem(graphics, menu.slots.get(menu.upgradeStart + 2), variant.upgrade1());
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            renderSlotItem(graphics, menu.slots.get(menu.mainInventoryStart + row * 9 + column),
                    new TerminalScreenLayout.Point(variant.inventoryOrigin().x() + column * 18,
                            variant.inventoryOrigin().y() + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            renderSlotItem(graphics, menu.slots.get(menu.hotbarStart + column),
                    new TerminalScreenLayout.Point(variant.hotbarOrigin().x() + column * 18, variant.hotbarOrigin().y()));
        }
    }

    private void renderCrafting(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                renderSlotItem(graphics, menu.slots.get(menu.craftingGridStart + row * 3 + column),
                        new TerminalScreenLayout.Point(variant.craftOrigin().x() + column * SLOT_SIZE,
                                variant.craftOrigin().y() + row * SLOT_SIZE));
            }
        }
        renderSlotItem(graphics, menu.slots.get(menu.craftingResultIndex), variant.result());
    }

    private void renderSlotItem(GuiGraphics graphics, Slot slot, TerminalScreenLayout.Point point) {
        ItemStack stack = visualPlayerPreviewItems.getOrDefault(slot.index, slot.getItem());
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, leftPos + point.x() + 1, topPos + point.y() + 1);
        renderItemDecorations(graphics, stack, leftPos + point.x() + 1, topPos + point.y() + 1);
    }

    private void renderItemDecorations(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.getCount() <= 999) {
            graphics.renderItemDecorations(font, stack, x, y);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + 19, y + 9, 0);
        graphics.pose().scale(0.65F, 0.65F, 1.0F);
        graphics.renderItemDecorations(font, stack, -19, -9);
        graphics.pose().popPose();
    }

    private void drawDottedFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        for (int x = left; x <= right; x += 4) {
            graphics.fill(leftPos + x, topPos + top, leftPos + Math.min(x + 2, right + 1), topPos + top + 1, color);
            graphics.fill(leftPos + x, topPos + bottom, leftPos + Math.min(x + 2, right + 1), topPos + bottom + 1, color);
        }
        for (int y = top; y <= bottom; y += 4) {
            graphics.fill(leftPos + left, topPos + y, leftPos + left + 1, topPos + Math.min(y + 2, bottom + 1), color);
            graphics.fill(leftPos + right, topPos + y, leftPos + right + 1, topPos + Math.min(y + 2, bottom + 1), color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        if (view.advanced()) {
            graphics.drawString(font, Component.literal(ADVANCED_MARK)
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    titleLabelX + font.width(title), titleLabelY, 0x404040, false);
        }
        if (!view.ownerName().getString().isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(titleLabelX, titleLabelY + font.lineHeight + 3, 0);
            graphics.pose().scale(OWNER_NAME_SCALE, OWNER_NAME_SCALE, 1.0F);
            graphics.drawString(font, Component.literal(view.ownerName().getString())
                            .withStyle(Style.EMPTY.withBold(true).withItalic(true)),
                    0, 0, 0x404040, false);
            graphics.pose().popPose();
        }
    }

    // Slot tooltips are rendered by this screen because BIG relocates static slots.
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ItemStack carried = menu.getCarried();
        if (visualRemotePreviewCarried != null) menu.setCarried(visualRemotePreviewCarried.copy());
        if (editingSection >= 0 && titleEditBox != null) {
            updateTitleEditBoxRect(editingSection);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (visualRemotePreviewCarried != null) menu.setCarried(carried);
        if (sectionMenuOpen) {
            renderSectionMenu(graphics, mouseX, mouseY, sectionMenuSourceButton);
        }
        if (!sectionMenuOpen && isPointInside(variant.cross(), TerminalScreenLayout.BUTTON_SIZE, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.simplestoragemanager.storage_manager_gui.clear_crafting"),
                    mouseX, mouseY);
        } else if (isPointInside(variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE, mouseX, mouseY)) {
            List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
            String stateKey = view.locked() ? "screen.simplestoragemanager.inventory_lock.disable"
                    : "screen.simplestoragemanager.inventory_lock.enable";
            lines.addAll(font.split(Component.translatable(stateKey).withStyle(ChatFormatting.WHITE), 180));
            if (hasShiftDown()) {
                String descriptionKey = view.locked() ? "screen.simplestoragemanager.inventory_lock.disabled_description"
                        : "screen.simplestoragemanager.inventory_lock.enabled_description";
                lines.addAll(font.split(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY), 180));
            } else {
                lines.addAll(font.split(Component.translatable("screen.simplestoragemanager.inventory_lock.shift_hint")
                        .withStyle(ChatFormatting.GRAY), 180));
            }
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        } else if (!sectionMenuOpen && hoveredOrganizeSection >= 0 && view.advanced()) {
            graphics.renderTooltip(font,
                    Component.translatable("gui.simplestoragemanager.storage_manager_gui.organize_inventory"),
                    mouseX, mouseY);
        } else if (!sectionMenuOpen && hoveredFilterSection >= 0 && view.advanced()) {
            if (isFilterEditing(hoveredFilterSection)) {
                graphics.renderTooltip(font,
                        Component.translatable("gui.simplestoragemanager.storage_manager_gui.close_inventory_filter"),
                        mouseX, mouseY);
            } else {
                graphics.renderTooltip(font,
                        Component.translatable("gui.simplestoragemanager.storage_manager_gui.open_inventory_filter"),
                        mouseX, mouseY);
            }
        } else if (hoveredNoticeDismiss) {
            graphics.renderTooltip(font, Component.translatable("screen.simplestoragemanager.dismiss_notice"),
                    mouseX, mouseY);
        } else if (hoveredUnavailableIcon) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(
                    Component.translatable("screen.simplestoragemanager.unavailable_tooltip"), 180);
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        } else if (hoveredRemote != null && !hoveredRemote.stack().isEmpty()) {
            graphics.renderTooltip(font, hoveredRemote.stack(), mouseX, mouseY);
        } else {
            Slot hoveredSlot = staticSlotAt(mouseX, mouseY);
            if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
                graphics.renderTooltip(font, hoveredSlot.getItem(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sectionMenuOpen) {
            handleSectionMenuClick(mouseX, mouseY, button, sectionMenuSourceButton);
            return true;
        }
        if (!isInside(variant.searchArea(), mouseX, mouseY) && searchBar != null && searchBar.isFocused()) {
            setFocused(null);
        }
        if (isInside(variant.searchArea(), mouseX, mouseY)) {
            if (button == 1 && searchBar != null) {
                searchBar.setValue("");
                setFocused(searchBar);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (editingSection >= 0 && titleEditBox != null && titleEditBox.isFocused()) {
            boolean insideBox = mouseX >= titleEditBox.getX() && mouseX < titleEditBox.getX() + titleEditBox.getWidth()
                    && mouseY >= titleEditBox.getY() && mouseY < titleEditBox.getY() + titleEditBox.getHeight();
            if (insideBox) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            cancelTitleEdit();
            return true;
        }
        if (pendingOperationSequence > 0 && button == 0 && hasShiftDown()) return true;
        if (isPointInside(variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE, mouseX, mouseY)) {
            if (button == 0) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                PacketDistributor.sendToServer(new TerminalLockTogglePayload());
            }
            return true;
        }
        if (isPointInside(variant.cross(), TerminalScreenLayout.BUTTON_SIZE, mouseX, mouseY)) {
            if (button == 0) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            PacketDistributor.sendToServer(new TerminalClearCraftingPayload());
            return true;
        }
        SectionButtonHit sectionButton = sectionButtonHit(mouseX, mouseY);
        if (sectionButton != null) {
            int sourceSection = sectionButton.section();
            if (button == 1) {
                // Clique direito abre o menu dropdown (Tipo de Filtro ou Organizar por).
                openSectionMenu(sectionButton.kind(), sourceSection, mouseX, mouseY);
            } else if (sectionButton.kind() == SECTION_BUTTON_FILTER) {
                // Clique esquerdo no botão de filtro alterna o modo de edição daquele inventário.
                toggleFilterEditMode(sourceSection);
            } else if (sectionButton.kind() == SECTION_BUTTON_ORGANIZE) {
                // Clique esquerdo no botão de organização executa a auto-organização naquele inventário,
                // usando a opção atualmente selecionada no menu dropdown.
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                int option = sectionMenuSelectedOrganizeFor(sourceSection);
                PacketDistributor.sendToServer(new TerminalOrganizePayload(sourceSection, option));
            }
            handleSectionButtonClick(button);
            return true;
        }
        int clickedTitleSection = titleSectionAt(mouseX, mouseY);
        if (clickedTitleSection >= 0) {
            handleTitleClick(clickedTitleSection, mouseX, mouseY, button);
            return true;
        }
        if (clickNotice(mouseX, mouseY)) return true;
        RemoteHit remoteHit = remoteHitAt(mouseX, mouseY);
        if (remoteHit != null && isFilterEditing(remoteHit.section())) {
            // Enquanto edita o filtro, cliques na área da seção afetam apenas os slots do filtro.
            handleFilterSlotClick(remoteHit.section(), remoteHit.slot(), button);
            return true;
        }
        if (remoteHit != null) {
            if (!view.canMutateInventories()) return true;
            if (isCreativePickButton(button)) {
                if (!menu.getCarried().isEmpty()) {
                    beginRemoteDrag(remoteHit, button, false, true);
                    return true;
                }
                if (!remoteHit.stack().isEmpty()) {
                    PacketDistributor.sendToServer(new TerminalStorageClickPayload(remoteHit.section(),
                            remoteHit.slot(), 2, false));
                }
                return true;
            }
            if (button != 0 && button != 1) return true;
            int remoteButton = button == 1 ? 1 : 0;
            boolean shiftRemoval = button == 0 && hasShiftDown() && menu.getCarried().isEmpty();
            if (shiftRemoval || (!hasShiftDown() && !menu.getCarried().isEmpty())
                    || (button == 0 && !menu.getCarried().isEmpty())) {
                beginRemoteDrag(remoteHit, button, shiftRemoval, false);
                return true;
            }
            PacketDistributor.sendToServer(new TerminalStorageClickPayload(remoteHit.section(), remoteHit.slot(),
                    remoteButton, shiftRemoval));
            return true;
        }
        Slot staticSlot = staticSlotAt(mouseX, mouseY);
        if (staticSlot != null) {
            if (menu.isTerminalSlot(staticSlot.index)) return true;
            if (isCreativePickButton(button)) {
                if (menu.getCarried().isEmpty()) {
                    if (staticSlot.index < menu.upgradeStart || staticSlot.index >= menu.upgradeStart + 3
                            && staticSlot.index != menu.craftingResultIndex) {
                        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, staticSlot.index, button,
                                net.minecraft.world.inventory.ClickType.CLONE, player);
                    }
                } else if (menu.canDragTo(staticSlot)) {
                    beginStaticDrag(staticSlot, button, false, true);
                }
                return true;
            }
            if (button != 0 && button != 1) return true;
            if (button == 0 && hasShiftDown() && menu.getCarried().isEmpty()) {
                if (!view.canMutateInventories()) return true;
                beginStaticDrag(staticSlot, button, true, false);
                return true;
            }
            if (!hasShiftDown() && !menu.getCarried().isEmpty() && menu.canDragTo(staticSlot)) {
                beginStaticDrag(staticSlot, button, false, false);
                return true;
            }
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, staticSlot.index,
                    button == 1 ? 1 : 0,
                    hasShiftDown() && button == 0 ? net.minecraft.world.inventory.ClickType.QUICK_MOVE
                            : net.minecraft.world.inventory.ClickType.PICKUP,
                    player);
            return true;
        }
        if (editingSection >= 0) return true;
        if (isInside(variant.scrollTrack(), mouseX, mouseY)) {
            draggingScroller = view.maxScroll() > 0;
            updateScroll(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isCreativePickButton(int button) {
        return player.getAbilities().instabuild
                && minecraft.options.keyPickItem.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    // Locked terminals: a manual drag without shift may only start inside the origin inventory of
    // the cursor stack or in an authorized inventory slot; starts elsewhere are cancelled before
    // the drag begins.
    private boolean isManualDragStartAllowed(ItemStack carried, boolean startInOriginInventory, boolean startInPlayerInventory) {
        if (carried.isEmpty() || !view.locked()) return true;
        boolean carriedHasOrigin = false;
        for (TerminalViewPayload.OriginTarget origin : view.originTargets()) {
            if (ItemStack.isSameItemSameComponents(origin.item(), carried)) {
                carriedHasOrigin = true;
                break;
            }
        }
        if (!carriedHasOrigin) return true;
        return ManualInsertionRules.isManualDragStartAllowed(view.locked(), carriedHasOrigin, startInOriginInventory, startInPlayerInventory);
    }

    private boolean isPlayerInventoryDragStartSlot(int slotIndex) {
        return slotIndex >= menu.mainInventoryStart && slotIndex < menu.hotbarStart + 9;
    }

    private boolean isCraftingDragStartSlot(int slotIndex) {
        return slotIndex >= menu.craftingGridStart && slotIndex < menu.craftingGridStart + 9;
    }

    private boolean isRemoteDragStartInOrigin(RemoteHit hit) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || !view.locked()) return true;
        List<TerminalViewPayload.OriginTarget> origins = view.originTargets();
        boolean carriedHasOrigin = false;
        for (TerminalViewPayload.OriginTarget origin : origins) {
            if (ItemStack.isSameItemSameComponents(origin.item(), carried)) {
                carriedHasOrigin = true;
                break;
            }
        }
        if (!carriedHasOrigin) return true;
        UUID hitConnectionId = remoteConnectionIds.get(key(hit.section(), hit.slot()));
        UUID originConnectionId = null;
        for (TerminalViewPayload.OriginTarget origin : origins) {
            if (!ItemStack.isSameItemSameComponents(origin.item(), carried)) continue;
            UUID candidate = remoteConnectionIds.get(key(origin.section(), origin.slot()));
            if (candidate != null) {
                originConnectionId = candidate;
                break;
            }
        }
        return originConnectionId != null && hitConnectionId != null && hitConnectionId.equals(originConnectionId);
    }

    private void clearItemDragState() {
        itemDragButton = -1;
        itemDragShift = false;
        itemDragMoved = false;
        itemDragCreative = false;
        itemDragStartSlot = -1;
        itemDragSlots.clear();
        remoteDragMode = 0;
        remoteDragShift = false;
        remoteDragTargets.clear();
        remoteDragInitialCarried = ItemStack.EMPTY;
    }

    private void beginRemoteDrag(RemoteHit hit, int button, boolean shift, boolean creative) {
        itemDragButton = button;
        itemDragShift = shift;
        itemDragCreative = creative;
        itemDragMoved = false;
        itemDragStartSlot = -1;
        itemDragSlots.clear();
        remoteDragMode = creative ? 2 : button == 1 ? 1 : 0;
        remoteDragShift = shift;
        remoteDragInitialCarried = menu.getCarried().copy();
        remoteDragTargets.clear();
        addRemoteDragTarget(hit);
        updateCombinedPreview();
    }

    private void beginStaticDrag(Slot slot, int button, boolean shift, boolean creative) {
        itemDragButton = button;
        itemDragShift = shift;
        itemDragCreative = creative;
        itemDragMoved = false;
        itemDragStartSlot = slot.index;
        itemDragSlots.clear();
        itemDragSlots.add(slot.index);
        remoteDragMode = creative ? 2 : button == 1 ? 1 : 0;
        remoteDragShift = shift;
        remoteDragInitialCarried = menu.getCarried().copy();
        remoteDragTargets.clear();
        updateCombinedPreview();
    }

    private void addStaticDragTarget(Slot slot) {
        if (itemDragShift && itemDragStartSlot < 0) return;
        if (slot == null || itemDragSlots.size() >= 256 || !itemDragSlots.add(slot.index)) return;
        if (itemDragSlots.size() > 1 || !remoteDragTargets.isEmpty()) itemDragMoved = true;
        updateCombinedPreview();
        finishDragWhenCarriedIsEmpty();
    }

    private void updateCombinedPreview() {
        if (!itemDragSlots.isEmpty()) updateStaticPreview();
        else remotePreviewCarried = remoteDragInitialCarried.copy();
        if (!remoteDragTargets.isEmpty()) updateRemotePreview();
    }

    private void updateStaticPreview() {
        playerPreviewItems.clear();
        ItemStack remaining = menu.getCarried().copy();
        if (itemDragShift || remaining.isEmpty()) {
            if (itemDragShift) updateShiftPreview();
            remotePreviewCarried = remaining;
            return;
        }
        Int2ObjectOpenHashMap<ItemStack> working = new Int2ObjectOpenHashMap<>();
        List<Integer> eligible = new ArrayList<>();
        for (int index : itemDragSlots) {
            Slot slot = menu.slots.get(index);
            ItemStack existing = slot.getItem().copy();
            if (!menu.canDragTo(slot) || !slot.mayPlace(remaining)
                    || !existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            if (!itemDragCreative && remaining.getCount() <= eligible.size()) continue;
            int limit = Math.min(remaining.getMaxStackSize(), slot.getMaxStackSize(remaining));
            if (limit > (existing.isEmpty() ? 0 : existing.getCount())) {
                eligible.add(index);
                working.put(index, existing);
            }
        }
        if (itemDragCreative) {
            if (player.getAbilities().instabuild) {
                for (int index : eligible) {
                    Slot slot = menu.slots.get(index);
                    int count = Math.min(remaining.getCount(), slot.getMaxStackSize(remaining));
                    ItemStack existing = working.get(index);
                    working.put(index, existing.isEmpty() ? remaining.copyWithCount(count)
                            : existing.copyWithCount(Math.max(existing.getCount(), count)));
                }
            }
        } else {
            int amount = itemDragButton == 1 ? 1 : eligible.isEmpty() ? 0 : remaining.getCount() / eligible.size();
            for (int index : eligible) {
                if (remaining.isEmpty() || amount <= 0) break;
                Slot slot = menu.slots.get(index);
                ItemStack existing = working.get(index);
                int currentCount = existing.isEmpty() ? 0 : existing.getCount();
                int limit = Math.min(remaining.getMaxStackSize(), slot.getMaxStackSize(remaining));
                int inserted = Math.min(amount, limit - currentCount);
                if (inserted <= 0) continue;
                working.put(index, existing.isEmpty() ? remaining.copyWithCount(inserted)
                        : existing.copyWithCount(currentCount + inserted));
                remaining.shrink(inserted);
            }
        }
        playerPreviewItems.putAll(working);
        remotePreviewCarried = itemDragCreative ? remoteDragInitialCarried.copy() : remaining;
        publishVisualPreview();
    }

    private void updateShiftPreview() {
        if (isUpgradeExtractionShiftDrag()) {
            playerPreviewItems.clear();
            remotePreviewItems.clear();
            updateUpgradeExtractionPreview();
            publishVisualPreview();
        }
        latestShiftPreviewRequestId = ++nextShiftPreviewRequestId;
        PacketDistributor.sendToServer(new TerminalShiftDragPreviewRequestPayload(
                latestShiftPreviewRequestId, List.copyOf(itemDragSlots)));
    }

    private boolean isUpgradeExtractionShiftDrag() {
        return itemDragShift && itemDragStartSlot >= menu.upgradeStart
                && itemDragStartSlot < menu.upgradeStart + 3
                && !itemDragSlots.isEmpty()
                && itemDragSlots.stream().allMatch(index ->
                index >= menu.upgradeStart && index < menu.upgradeStart + 3);
    }

    private void updateUpgradeExtractionPreview() {
        boolean hasLevel2 = !menu.slots.get(menu.upgradeStart + UpgradeSlotRules.MIDDLE_SLOT)
                .getItem().isEmpty();
        boolean hasLevel3 = !menu.slots.get(menu.upgradeStart + UpgradeSlotRules.TOP_SLOT)
                .getItem().isEmpty();
        Int2ObjectOpenHashMap<ItemStack> playerStacks = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<ItemStack> originalPlayerStacks = new Int2ObjectOpenHashMap<>();
        for (int index = menu.mainInventoryStart; index < menu.hotbarStart + 9; index++) {
            if (menu.isTerminalSlot(index)) continue;
            ItemStack stack = menu.slots.get(index).getItem().copy();
            playerStacks.put(index, stack);
            originalPlayerStacks.put(index, stack.copy());
        }

        for (int sourceIndex : itemDragSlots) {
            int upgradeSlot = sourceIndex - menu.upgradeStart;
            if (!UpgradeSlotRules.canRemove(upgradeSlot, hasLevel2, hasLevel3)) continue;
            ItemStack source = menu.slots.get(sourceIndex).getItem().copy();
            if (source.isEmpty()) continue;
            ItemStack moving = source.copy();
            for (int pass = 0; pass < 2 && !moving.isEmpty(); pass++) {
                for (int targetIndex = menu.mainInventoryStart;
                     targetIndex < menu.hotbarStart + 9 && !moving.isEmpty(); targetIndex++) {
                    Slot target = menu.slots.get(targetIndex);
                    if (menu.isTerminalSlot(targetIndex) || !target.mayPlace(moving)) continue;
                    ItemStack existing = playerStacks.get(targetIndex);
                    if (pass == 0 && (existing.isEmpty()
                            || !ItemStack.isSameItemSameComponents(existing, moving))) continue;
                    if (pass == 1 && !existing.isEmpty()) continue;
                    int currentCount = existing.isEmpty() ? 0 : existing.getCount();
                    int limit = Math.min(moving.getMaxStackSize(), target.getMaxStackSize(moving));
                    int accepted = Math.min(moving.getCount(), Math.max(0, limit - currentCount));
                    if (accepted <= 0) continue;
                    playerStacks.put(targetIndex, existing.isEmpty()
                            ? moving.copyWithCount(accepted)
                            : existing.copyWithCount(currentCount + accepted));
                    moving.shrink(accepted);
                }
            }
            if (moving.getCount() != source.getCount()) {
                playerPreviewItems.put(sourceIndex, moving);
            }
            if (moving.isEmpty()) {
                if (upgradeSlot == UpgradeSlotRules.TOP_SLOT) hasLevel3 = false;
                if (upgradeSlot == UpgradeSlotRules.MIDDLE_SLOT) hasLevel2 = false;
            }
        }
        for (Map.Entry<Integer, ItemStack> entry : playerStacks.entrySet()) {
            if (!ItemStack.matches(originalPlayerStacks.get(entry.getKey()), entry.getValue())) {
                playerPreviewItems.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void applyShiftDragPreview(TerminalShiftDragPreviewPayload payload) {
        if ((!itemDragShift && !shiftDragCommitPending)
                || payload.requestId() != latestShiftPreviewRequestId) return;
        appliedShiftPreviewRequestId = payload.requestId();
        // While a shift-drag is still being planned or applied, an incomplete response must not wipe
        // the current preview: the drag keeps moving and the display has to hold a consistent state
        // until the plan for the current drag is known.
        boolean applies = payload.complete() || !(itemDragShift || shiftDragCommitPending);
        if (applies) {
            playerPreviewItems.clear();
            remotePreviewItems.clear();
            visualPlayerPreviewItems.clear();
            visualRemotePreviewItems.clear();
            visualRemotePreviewCarried = null;
        }
        if (payload.complete()) {
            for (TerminalShiftDragPreviewPayload.RemoteResult destination : payload.destinations()) {
                long slotKey = key(destination.section(), destination.slot());
                if (!destination.connectionId().equals(remoteConnectionIds.get(slotKey))
                        || destination.storageSlot() != remoteStorageSlots.getOrDefault(slotKey, -1)) {
                    cancelPendingShiftDrag();
                    requestView(view.scrollOffset());
                    return;
                }
            }
            for (TerminalShiftDragPreviewPayload.SourceResult source : payload.sources()) {
                if (source.menuSlot() >= 0 && source.menuSlot() < menu.slots.size()) {
                    playerPreviewItems.put(source.menuSlot(), source.remaining());
                }
            }
            for (TerminalShiftDragPreviewPayload.PlayerResult destination : payload.playerDestinations()) {
                if (destination.menuSlot() >= 0 && destination.menuSlot() < menu.slots.size()) {
                    playerPreviewItems.put(destination.menuSlot(), destination.stack());
                }
            }
            for (TerminalShiftDragPreviewPayload.RemoteResult destination : payload.destinations()) {
                remotePreviewItems.put(key(destination.section(), destination.slot()), destination.stack());
            }
            publishVisualPreview();
        }
        if (shiftDragCommitPending) {
            sendShiftDragCommit();
        }
    }

    private void finishItemDrag() {
        boolean shiftOperation = itemDragShift
                && (!itemDragSlots.isEmpty() || !remoteDragTargets.isEmpty());
        if (shiftOperation && pendingOperationSequence == 0) {
            pendingOperationSequence = ++nextOperationSequence;
        }
        if (shiftOperation && !(isUpgradeExtractionShiftDrag() && itemDragMoved) && !itemDragSlots.isEmpty()
                && appliedShiftPreviewRequestId != latestShiftPreviewRequestId) {
            shiftDragCommitPending = true;
            itemDragButton = -1;
            return;
        }
        if (!itemDragSlots.isEmpty() && itemDragShift) {
            sendShiftDragCommit();
        } else if (!itemDragSlots.isEmpty() && !itemDragCreative && itemDragMoved) {
            int type = remoteDragMode;
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, -999,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(0, type),
                    net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
            for (int slotIndex : itemDragSlots) {
                minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex,
                        net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(1, type),
                        net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
            }
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, -999,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(2, type),
                    net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
        } else if (!itemDragSlots.isEmpty() && !itemDragMoved && itemDragStartSlot >= 0) {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, itemDragStartSlot,
                    itemDragCreative ? itemDragButton : itemDragButton == 1 ? 1 : 0,
                    itemDragShift ? net.minecraft.world.inventory.ClickType.QUICK_MOVE
                            : itemDragCreative ? net.minecraft.world.inventory.ClickType.CLONE
                            : net.minecraft.world.inventory.ClickType.PICKUP,
                    player);
        }
        boolean hasRemoteTargets = !remoteDragTargets.isEmpty();
        if (itemDragCreative && (hasRemoteTargets || !itemDragSlots.isEmpty())) {
            if (itemDragMoved) {
                TerminalStorageClickPayload.SlotTarget first = hasRemoteTargets
                        ? remoteDragTargets.get(0) : new TerminalStorageClickPayload.SlotTarget(-1, -1);
                PacketDistributor.sendToServer(new TerminalStorageClickPayload(first.section(), first.slot(), 2,
                        false, List.copyOf(remoteDragTargets), List.copyOf(itemDragSlots)));
            } else if (hasRemoteTargets) {
                TerminalStorageClickPayload.SlotTarget target = remoteDragTargets.get(0);
                PacketDistributor.sendToServer(new TerminalStorageClickPayload(
                        target.section(), target.slot(), 2, false));
            }
        } else if (hasRemoteTargets) {
            TerminalStorageClickPayload.SlotTarget first = remoteDragTargets.get(0);
            boolean sendBatch = itemDragMoved || remoteDragShift;
            PacketDistributor.sendToServer(sendBatch
                    ? new TerminalStorageClickPayload(first.section(), first.slot(), remoteDragMode,
                            remoteDragShift, List.copyOf(remoteDragTargets), List.of(),
                            remoteDragShift ? pendingOperationSequence : 0)
                    : new TerminalStorageClickPayload(first.section(), first.slot(), remoteDragMode, false));
        }
        itemDragButton = -1;
        itemDragShift = false;
        itemDragMoved = false;
        itemDragCreative = false;
        itemDragStartSlot = -1;
        itemDragSlots.clear();
        remoteDragMode = 0;
        remoteDragShift = false;
        remoteDragTargets.clear();
        remoteDragInitialCarried = ItemStack.EMPTY;
        if (!hasRemoteTargets && !shiftOperation) clearRemotePreview();
    }

    private void sendShiftDragCommit() {
        PacketDistributor.sendToServer(new TerminalStorageClickPayload(-1, -1, 0, true,
                List.of(), List.copyOf(itemDragSlots), pendingOperationSequence,
                isUpgradeExtractionShiftDrag() && itemDragMoved ? 0 : latestShiftPreviewRequestId));
        shiftDragCommitPending = false;
        itemDragButton = -1;
        itemDragShift = false;
        itemDragMoved = false;
        itemDragCreative = false;
        itemDragStartSlot = -1;
        itemDragSlots.clear();
        remoteDragMode = 0;
        remoteDragShift = false;
        remoteDragTargets.clear();
        remoteDragInitialCarried = ItemStack.EMPTY;
    }

    private void cancelPendingShiftDrag() {
        shiftDragCommitPending = false;
        pendingOperationSequence = 0;
        itemDragButton = -1;
        itemDragShift = false;
        itemDragMoved = false;
        itemDragCreative = false;
        itemDragStartSlot = -1;
        itemDragSlots.clear();
        remoteDragMode = 0;
        remoteDragShift = false;
        remoteDragTargets.clear();
        remoteDragInitialCarried = ItemStack.EMPTY;
        clearRemotePreview();
    }

    private void addRemoteDragTarget(RemoteHit hit) {
        if (itemDragShift && itemDragStartSlot >= 0) return;
        if (hit == null || remoteDragTargets.size() >= 256) return;
        TerminalStorageClickPayload.SlotTarget target =
                new TerminalStorageClickPayload.SlotTarget(hit.section(), hit.slot());
        if (!remoteDragTargets.contains(target)) {
            if (!remoteDragTargets.isEmpty() || !itemDragSlots.isEmpty()) itemDragMoved = true;
            remoteDragTargets.add(target);
            updateCombinedPreview();
            finishDragWhenCarriedIsEmpty();
        }
    }

    private void finishDragWhenCarriedIsEmpty() {
        if (!itemDragMoved || itemDragShift || itemDragCreative
                || remotePreviewCarried == null || !remotePreviewCarried.isEmpty()) return;
        if (itemDragButton == 1 || itemDragButton == 0
                && eligibleDragSlotCount() >= remoteDragInitialCarried.getCount()) finishItemDrag();
    }

    private int eligibleDragSlotCount() {
        int eligible = 0;
        ItemStack carried = remoteDragInitialCarried;
        for (int index : itemDragSlots) {
            Slot slot = menu.slots.get(index);
            ItemStack existing = slot.getItem();
            if (menu.canDragTo(slot) && slot.mayPlace(carried)
                    && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, carried))
                    && slot.getMaxStackSize(carried) > (existing.isEmpty() ? 0 : existing.getCount())) {
                eligible++;
            }
        }
        for (TerminalStorageClickPayload.SlotTarget target : remoteDragTargets) {
            long slotKey = key(target.section(), target.slot());
            ItemStack existing = remoteItems.getOrDefault(slotKey, ItemStack.EMPTY);
            if ((existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, carried))
                    && remoteSlotLimit(slotKey, carried) > (existing.isEmpty() ? 0 : existing.getCount())) {
                eligible++;
            }
        }
        return eligible;
    }

    private void updateRemotePreview() {
        remotePreviewItems.clear();
        ItemStack remaining = itemDragSlots.isEmpty()
                ? remoteDragInitialCarried.copy()
                : remotePreviewCarried == null ? remoteDragInitialCarried.copy() : remotePreviewCarried.copy();
        if (remoteDragShift) {
            playerPreviewItems.clear();
            for (int index = menu.mainInventoryStart; index < menu.hotbarStart + 9; index++) {
                playerPreviewItems.put(index, menu.slots.get(index).getItem().copy());
            }
            for (TerminalStorageClickPayload.SlotTarget target : remoteDragTargets) {
                long slotKey = key(target.section(), target.slot());
                ItemStack source = remoteItems.getOrDefault(slotKey, ItemStack.EMPTY).copy();
                if (source.isEmpty()) continue;
                int requested = TerminalInsertionRules.shiftExtractionCount(source.getCount(),
                        source.getMaxStackSize());
                ItemStack moving = source.copyWithCount(requested);
                insertPreviewIntoPlayerInventory(moving);
                source.shrink(requested - moving.getCount());
                remotePreviewItems.put(slotKey, source);
            }
            remotePreviewCarried = ItemStack.EMPTY;
            publishVisualPreview();
            return;
        }
        List<TerminalStorageClickPayload.SlotTarget> eligible = new ArrayList<>();
        Long2ObjectOpenHashMap<ItemStack> working = new Long2ObjectOpenHashMap<>();
        for (TerminalStorageClickPayload.SlotTarget target : remoteDragTargets) {
            long slotKey = key(target.section(), target.slot());
            ItemStack existing = remoteItems.getOrDefault(slotKey, ItemStack.EMPTY).copy();
            if (!existing.isEmpty() && (remaining.isEmpty()
                    || !ItemStack.isSameItemSameComponents(existing, remaining))) continue;
            if (!remaining.isEmpty() && existing.getCount() >= remoteSlotLimit(slotKey, remaining)) continue;
            eligible.add(target);
            working.put(slotKey, existing);
        }

        if (remaining.isEmpty()) return;
        if (remoteDragMode == 2 && player.getAbilities().instabuild) {
            for (TerminalStorageClickPayload.SlotTarget target : eligible) {
                long slotKey = key(target.section(), target.slot());
                ItemStack existing = working.get(slotKey);
                int limit = remoteSlotLimit(slotKey, remaining);
                int count = Math.min(remaining.getCount(), limit);
                if (count <= 0) continue;
                if (existing.isEmpty()) {
                    working.put(slotKey, remaining.copyWithCount(count));
                } else {
                    working.put(slotKey, existing.copyWithCount(Math.max(existing.getCount(), count)));
                }
            }
        } else {
            int amount = remoteDragMode == 1 ? 1 : eligible.isEmpty() ? 0 : remaining.getCount() / eligible.size();
            for (TerminalStorageClickPayload.SlotTarget target : eligible) {
                if (remaining.isEmpty() || amount <= 0) break;
                long slotKey = key(target.section(), target.slot());
                ItemStack existing = working.get(slotKey);
                int currentCount = existing.isEmpty() ? 0 : existing.getCount();
                int inserted = Math.min(amount, remoteSlotLimit(slotKey, remaining) - currentCount);
                if (inserted <= 0) continue;
                working.put(slotKey, existing.isEmpty() ? remaining.copyWithCount(inserted)
                        : existing.copyWithCount(currentCount + inserted));
                remaining.shrink(inserted);
            }
        }
        remotePreviewItems.putAll(working);
        remotePreviewCarried = remoteDragMode == 2 ? remoteDragInitialCarried.copy() : remaining;
        publishVisualPreview();
    }

    private void insertPreviewIntoPlayerInventory(ItemStack moving) {
        for (int pass = 0; pass < 2 && !moving.isEmpty(); pass++) {
            for (int index = menu.mainInventoryStart; index < menu.hotbarStart + 9 && !moving.isEmpty(); index++) {
                Slot slot = menu.slots.get(index);
                if (menu.isTerminalSlot(index) || !slot.mayPlace(moving)) continue;
                ItemStack current = playerPreviewItems.get(index);
                if (pass == 0 && (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, moving))) continue;
                if (pass == 1 && !current.isEmpty()) continue;
                int limit = Math.min(moving.getMaxStackSize(), slot.getMaxStackSize(moving));
                int available = limit - (current.isEmpty() ? 0 : current.getCount());
                int count = Math.min(moving.getCount(), available);
                if (count <= 0) continue;
                playerPreviewItems.put(index, current.isEmpty() ? moving.copyWithCount(count)
                        : current.copyWithCount(current.getCount() + count));
                moving.shrink(count);
            }
        }
    }

    private void clearRemotePreview() {
        remotePreviewItems.clear();
        playerPreviewItems.clear();
        remotePreviewCarried = null;
        clearVisualPreview();
    }

    private void publishVisualPreview() {
        visualPlayerPreviewItems.clear();
        visualPlayerPreviewItems.putAll(playerPreviewItems);
        visualRemotePreviewItems.clear();
        visualRemotePreviewItems.putAll(remotePreviewItems);
        visualRemotePreviewCarried = remotePreviewCarried;
    }

    private void clearVisualPreview() {
        visualPlayerPreviewItems.clear();
        visualRemotePreviewItems.clear();
        visualRemotePreviewCarried = null;
    }

    private RemoteHit remoteHitAt(double mouseX, double mouseY) {
        TerminalScreenLayout.Rect area = variant.contentArea();
        if (!isInside(area, mouseX, mouseY)) return null;
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        int cursor = area.top() + 2 - view.scrollOffset();
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                cursor += sectionHeight(entry.notice().slotCount());
                continue;
            }
            TerminalViewPayload.Section section = entry.section();
            int slotY = cursor + 12;
            for (int slot = 0; slot < section.slotCount(); slot++) {
                int slotX = area.left() + (slot % 9) * SLOT_SIZE;
                int currentY = slotY + (slot / 9) * SLOT_SIZE;
                if (x >= slotX && x < slotX + SLOT_SIZE && y >= currentY && y < currentY + SLOT_SIZE) {
                    return new RemoteHit(entry.sectionIndex(), slot,
                            visualRemotePreviewItems.getOrDefault(key(entry.sectionIndex(), slot),
                                    remoteItems.getOrDefault(key(entry.sectionIndex(), slot), ItemStack.EMPTY)));
                }
            }
            cursor += sectionHeight(section.slotCount());
        }
        return null;
    }

    private boolean clickNotice(double mouseX, double mouseY) {
        int cursor = variant.contentArea().top() + 2 - view.scrollOffset();
        for (ClientLayoutEntry entry : layoutEntries()) {
            if (entry.notice() != null) {
                int bottom = cursor + sectionHeight(entry.notice().slotCount()) - 4;
                int crossX = variant.contentArea().right() - 9;
                int crossY = cursor + 2;
                if (mouseX >= leftPos + crossX && mouseX < leftPos + crossX + 7
                        && mouseY >= topPos + crossY && mouseY < topPos + crossY + 7) {
                    TerminalViewPayload.Notice notice = entry.notice();
                PacketDistributor.sendToServer(new TerminalNoticeDismissPayload(notice.dimension(), notice.pos()));
                return true;
                }
                cursor = bottom + 4;
                continue;
            }
            cursor += sectionHeight(entry.section().slotCount());
        }
        return false;
    }

    private Slot staticSlotAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (pointHit(variant.result(), x, y)) return menu.slots.get(menu.craftingResultIndex);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(
                        variant.craftOrigin().x() + column * SLOT_SIZE,
                        variant.craftOrigin().y() + row * SLOT_SIZE);
                if (pointHit(point, x, y)) {
                    return menu.slots.get(menu.craftingGridStart + row * 3 + column);
                }
            }
        }
        for (int i = 0; i < 4; i++) if (pointHit(variant.armor()[i], x, y)) return menu.slots.get(menu.armorStart + i);
        if (pointHit(variant.offhand(), x, y)) return menu.slots.get(menu.offhandIndex);
        if (pointHit(variant.upgrade3(), x, y)) return menu.slots.get(menu.upgradeStart);
        if (pointHit(variant.upgrade2(), x, y)) return menu.slots.get(menu.upgradeStart + 1);
        if (pointHit(variant.upgrade1(), x, y)) return menu.slots.get(menu.upgradeStart + 2);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(variant.inventoryOrigin().x() + column * 18,
                    variant.inventoryOrigin().y() + row * 18);
            if (pointHit(point, x, y)) return menu.slots.get(menu.mainInventoryStart + row * 9 + column);
        }
        for (int column = 0; column < 9; column++) {
            TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(variant.hotbarOrigin().x() + column * 18,
                    variant.hotbarOrigin().y());
            if (pointHit(point, x, y)) return menu.slots.get(menu.hotbarStart + column);
        }
        return null;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!filterEditSections.isEmpty() && button == 0 && menu.getCarried().isEmpty()) {
            RemoteHit hit = remoteHitAt(mouseX, mouseY);
            if (hit != null && isFilterEditing(hit.section())) {
                clearFilterSlot(hit.section(), hit.slot());
                return true;
            }
        }
        if (itemDragButton >= 0 && button == itemDragButton) {
            if (!itemDragMoved && !itemDragShift && !itemDragCreative && !menu.getCarried().isEmpty()) {
                // A manual drag without shift may only start inside the origin inventory of the
                // cursor stack or in a slot of the player's own inventory; an invalid start cancels
                // the operation before execution.
                if (itemDragStartSlot < 0) {
                    if (!remoteDragTargets.isEmpty()) {
                        TerminalStorageClickPayload.SlotTarget startTarget =
                                remoteDragTargets.get(0);
                        RemoteHit startHit = new RemoteHit(startTarget.section(), startTarget.slot(),
                                ItemStack.EMPTY);
                        if (!isManualDragStartAllowed(menu.getCarried(),
                                isRemoteDragStartInOrigin(startHit), false)) {
                            clearItemDragState();
                            return true;
                        }
                    }
                } else if (!isManualDragStartAllowed(menu.getCarried(), false,
                        isPlayerInventoryDragStartSlot(itemDragStartSlot) || isCraftingDragStartSlot(itemDragStartSlot))) {
                    clearItemDragState();
                    return true;
                }
            }
            addStaticDragTarget(staticSlotAt(mouseX, mouseY));
            if (itemDragButton < 0) return true;
            addRemoteDragTarget(remoteHitAt(mouseX, mouseY));
            return true;
        }
        if (draggingScroller) {
            updateScroll(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (itemDragButton >= 0 && button == itemDragButton) {
            addStaticDragTarget(staticSlotAt(mouseX, mouseY));
            if (itemDragButton < 0) return true;
            addRemoteDragTarget(remoteHitAt(mouseX, mouseY));
            if (itemDragButton < 0) return true;
            finishItemDrag();
            return true;
        }
        if (draggingScroller) {
            draggingScroller = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScroll(double mouseY) {
        if (editingSection >= 0) return;
        int travel = variant.scrollTrack().height() - 15;
        if (travel <= 0 || view.maxScroll() <= 0) return;
        int next = (int) (((mouseY - topPos - variant.scrollTrack().top() - 7.5D) / travel) * view.maxScroll());
        next = Math.max(0, Math.min(view.maxScroll(), next));
        requestView(next);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editingSection >= 0) return true;
        if (isInside(variant.contentArea(), mouseX, mouseY) || isInside(variant.scrollTrack(), mouseX, mouseY)) {
            int speed = SimplestoragemanagerConfig.SCROLL_SPEED.get();
            requestView(view.scrollOffset() - (int) Math.signum(scrollY) * speed);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar != null && searchBar.isFocused()
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (editingSection >= 0 && titleEditBox != null && titleEditBox.isFocused()) {
            if (keyCode == InputConstants.KEY_RETURN) {
                confirmTitleEdit();
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                cancelTitleEdit();
                return true;
            }
            return titleEditBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (sectionMenuOpen && keyCode == InputConstants.KEY_ESCAPE) {
            closeSectionMenu();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingSection >= 0 && titleEditBox != null && titleEditBox.isFocused()) {
            return titleEditBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String value = searchBar == null ? "" : searchBar.getValue();
        super.resize(minecraft, width, height);
        if (editingSection >= 0) cancelTitleEdit();
        if (searchBar != null) searchBar.setValue(value);
    }

    @Override
    public void updateMenuState(int elementType, String name, Object state) { }

    private boolean isInside(TerminalScreenLayout.Rect rect, double mouseX, double mouseY) {
        return mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.right() + 1
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.bottom() + 1;
    }

    /**
     * Returns the menu slot at the given screen-relative coordinates, checking only the crafting
     * grid and the player's personal inventory (main + hotbar). Remote inventory sections are not
     * real menu slots and are intentionally excluded. Used by EMI's stack provider so that
     * shortcut keys (R/U) can resolve the hovered item without requiring a prior click.
     *
     * @param x screen-relative X coordinate (mouseX - guiLeft)
     * @param y screen-relative Y coordinate (mouseY - guiTop)
     * @return the slot at the position, or null if none matches
     */
    public Slot getSlotAtRelative(double x, double y) {
        // Crafting grid: 3x3
        TerminalScreenLayout.Point craftOrigin = variant.craftOrigin();
        if (x >= craftOrigin.x() && x < craftOrigin.x() + 54
                && y >= craftOrigin.y() && y < craftOrigin.y() + 54) {
            int column = (int) ((x - craftOrigin.x()) / 18);
            int row = (int) ((y - craftOrigin.y()) / 18);
            if (column >= 0 && column < 3 && row >= 0 && row < 3) {
                return menu.slots.get(menu.craftingGridStart + row * 3 + column);
            }
        }
        // Player main inventory: 3 rows x 9 columns
        TerminalScreenLayout.Point invOrigin = variant.inventoryOrigin();
        if (x >= invOrigin.x() && x < invOrigin.x() + 162
                && y >= invOrigin.y() && y < invOrigin.y() + 54) {
            int column = (int) ((x - invOrigin.x()) / 18);
            int row = (int) ((y - invOrigin.y()) / 18);
            if (column >= 0 && column < 9 && row >= 0 && row < 3) {
                return menu.slots.get(menu.mainInventoryStart + row * 9 + column);
            }
        }
        // Hotbar: 1 row x 9 columns
        TerminalScreenLayout.Point hotbarOrigin = variant.hotbarOrigin();
        if (x >= hotbarOrigin.x() && x < hotbarOrigin.x() + 162
                && y >= hotbarOrigin.y() && y < hotbarOrigin.y() + 18) {
            int column = (int) ((x - hotbarOrigin.x()) / 18);
            if (column >= 0 && column < 9) {
                return menu.slots.get(menu.hotbarStart + column);
            }
        }
        return null;
    }

    /**
     * Item sob o mouse em coordenadas absolutas da tela, incluindo os inventários conectados
     * (snapshots). A lógica fica em {@link TerminalHoverHit}, fora desta classe já extensa.
     */
    public ItemStack getHoveredItem(double mouseX, double mouseY) {
        return TerminalHoverHit.hovered(hoverContext(), mouseX, mouseY).stack();
    }

    /** Retângulo do slot sob o mouse, usado pelo JEI para registrar o ingrediente clicável. */
    public Rect2i getHoveredSlotRect(double mouseX, double mouseY) {
        return TerminalHoverHit.hovered(hoverContext(), mouseX, mouseY).slotRect();
    }

    private TerminalHoverHit.Context hoverContext() {
        return new TerminalHoverHit.Context(variant, view, visualRemotePreviewItems, remoteItems,
                menu, leftPos, topPos);
    }

    private boolean pointHit(TerminalScreenLayout.Point point, double x, double y) {
        return x >= point.x() && x < point.x() + 18 && y >= point.y() && y < point.y() + 18;
    }

    private boolean isPointInside(TerminalScreenLayout.Point point, int size, double mouseX, double mouseY) {
        return mouseX >= leftPos + point.x() && mouseX < leftPos + point.x() + size
                && mouseY >= topPos + point.y() && mouseY < topPos + point.y() + size;
    }

    private long key(int section, int slot) { return ((long) section << 32) | (slot & 0xFFFFFFFFL); }

    private List<ClientLayoutEntry> layoutEntries() {
        if (cachedLayoutEntries != null && cachedLayoutRevision == layoutRevision) {
            return cachedLayoutEntries;
        }
        List<TerminalViewPayload.Notice> notices = new ArrayList<>(view.notices());
        notices.sort(Comparator.comparingInt(TerminalViewPayload.Notice::sectionIndex));
        List<ClientLayoutEntry> entries = new ArrayList<>();
        int noticeIndex = 0;
        for (int sectionIndex = 0; sectionIndex < view.sections().size(); sectionIndex++) {
            while (noticeIndex < notices.size()
                    && notices.get(noticeIndex).sectionIndex() <= sectionIndex) {
                entries.add(ClientLayoutEntry.notice(notices.get(noticeIndex++)));
            }
            entries.add(ClientLayoutEntry.section(sectionIndex, view.sections().get(sectionIndex)));
        }
        while (noticeIndex < notices.size()) {
            entries.add(ClientLayoutEntry.notice(notices.get(noticeIndex++)));
        }
        cachedLayoutEntries = entries;
        cachedLayoutRevision = layoutRevision;
        return entries;
    }

    private int sectionHeight(int slotCount) {
        int rows = Math.max(1, (slotCount + 8) / 9);
        return 12 + rows * SLOT_SIZE + 2 + 4;
    }

    private static final class TitleScrollAnimation {
        private long startedAtTick = -1L;
    }

    private record RemoteHit(int section, int slot, ItemStack stack) { }

    private record ClientLayoutEntry(int sectionIndex, TerminalViewPayload.Section section,
                                     TerminalViewPayload.Notice notice) {
        private static ClientLayoutEntry section(int index, TerminalViewPayload.Section section) {
            return new ClientLayoutEntry(index, section, null);
        }

        private static ClientLayoutEntry notice(TerminalViewPayload.Notice notice) {
            return new ClientLayoutEntry(-1, null, notice);
        }
    }
}
