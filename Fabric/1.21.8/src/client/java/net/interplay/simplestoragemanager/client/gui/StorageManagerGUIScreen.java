package net.interplay.simplestoragemanager.client.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.interplay.simplestoragemanager.access.ConnectionStatus;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModScreens;
import net.interplay.simplestoragemanager.logic.InventoryFilterLogic;
import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.interplay.simplestoragemanager.logic.TerminalInsertionRules;
import net.interplay.simplestoragemanager.logic.TerminalScaleChoice;
import net.interplay.simplestoragemanager.logic.TerminalTitleLogic;
import net.interplay.simplestoragemanager.logic.UpgradeSlotRules;
import net.interplay.simplestoragemanager.network.TerminalClearCraftingPayload;
import net.interplay.simplestoragemanager.network.TerminalConnectionStatusPayload;
import net.interplay.simplestoragemanager.network.TerminalFilterSavePayload;
import net.interplay.simplestoragemanager.network.TerminalLockTogglePayload;
import net.interplay.simplestoragemanager.network.TerminalNoticeDismissPayload;
import net.interplay.simplestoragemanager.network.TerminalOrganizePayload;
import net.interplay.simplestoragemanager.network.TerminalRenamePayload;
import net.interplay.simplestoragemanager.network.TerminalShiftDragPreviewPayload;
import net.interplay.simplestoragemanager.network.TerminalShiftDragPreviewRequestPayload;
import net.interplay.simplestoragemanager.network.TerminalStorageClickPayload;
import net.interplay.simplestoragemanager.network.TerminalViewPayload;
import net.interplay.simplestoragemanager.network.TerminalViewRequestPayload;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2i;

public class StorageManagerGUIScreen extends AbstractContainerScreen<StorageManagerGUIMenu> implements SimplestoragemanagerModScreens.ScreenAccessor {
   private static final ResourceLocation SMALL_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/storage_manager_gui.png");
   private static final ResourceLocation BIG_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/storage_manager_gui_big.png");
   private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/slot.png");
   private static final ResourceLocation SCROLLER_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/scroller.png");
   private static final ResourceLocation SCROLLER_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/scroller_disabled.png");
   private static final ResourceLocation CROSS_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/cross_button.png");
   private static final ResourceLocation CROSS_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/cross_button_highlighted.png");
   private static final ResourceLocation LOCKED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/locked_button.png");
   private static final ResourceLocation LOCKED_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/locked_button_highlighted.png");
   private static final ResourceLocation UNLOCKED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/unlocked_button.png");
   private static final ResourceLocation UNLOCKED_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/unlocked_button_highlighted.png");
   private static final ResourceLocation REPORT_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/report_button.png");
   private static final ResourceLocation REPORT_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/report_button_highlighted.png");
   private static final ResourceLocation ORGANIZER_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/organizer.png");
   private static final ResourceLocation FILTER_TEXTURE = ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/filter.png");
   private static final int SECTION_BUTTON_SIZE = 10;
   private static final int SECTION_BUTTON_ORGANIZE = 1;
   private static final int SECTION_BUTTON_FILTER = 2;
   private static final int SECTION_MENU_ORGANIZE_OPTION_COUNT = 4;
   private static final int FILTER_MENU_ITEM_COUNT = 2;
   private static final int SECTION_MENU_OFFSET_X = 10;
   private static final int SECTION_MENU_OFFSET_Y = 10;
   private static final String MENU_CHECK_MARK = "✔";
   private static final int MENU_TITLE_COLOR = -1;
   // 0xFF444444: cor padrão usada por AbstractContainerScreen.renderLabels() na vanilla.
   private static final int VANILLA_TITLE_COLOR = -12566464;
   private static final int MENU_OPTION_COLOR = -291053;
   private static final int MENU_OPTION_COLOR_UNSELECTED = -8355712;
   private static final String ADVANCED_MARK = " MkII";
   private static final float OWNER_NAME_SCALE = 0.5F;
   // Title coordinates are relative to the GUI texture: renderLabels() is called after
   // GuiGraphics is translated by (leftPos, topPos), so leftPos/topPos must NOT be added here.
   private static final int MAIN_TITLE_LABEL_Y = 7;
   private final Player player;
   private TerminalScreenLayout.Variant variant;
   private EditBox searchBar;
   private EditBox titleEditBox;
   private int titleBoxHeight;
   private int editingSection;
   private String editingBackupName;
   private final Map<Integer, TitleScrollAnimation> titleScrollAnimations;
   private int lastTitleClickSection;
   private long lastTitleClickTime;
   private double lastTitleClickX;
   private double lastTitleClickY;
   private TerminalViewPayload view;
   private List<ClientLayoutEntry> cachedLayoutEntries;
   private int cachedLayoutRevision;
   private int layoutRevision;
   private final Long2ObjectOpenHashMap<ItemStack> remoteItems;
   private final Long2IntOpenHashMap remoteSlotLimits;
   private final Long2ObjectOpenHashMap<UUID> remoteConnectionIds;
   private final Long2IntOpenHashMap remoteStorageSlots;
   private RemoteHit hoveredRemote;
   private boolean hoveredUnavailableIcon;
   private boolean hoveredNoticeDismiss;
   private int hoveredOrganizeSection;
   private int hoveredFilterSection;
   private boolean sectionMenuOpen;
   private int sectionMenuSourceButton;
   private int sectionMenuSourceSection;
   private final Map<Integer, Integer> sectionSelectedOrganize;
   private final Set<Integer> filterEditSections;
   private final Map<Integer, List<ItemStack>> filterEditItems;
   private final Map<Integer, Integer> filterSectionMode;
   private int sectionMenuX;
   private int sectionMenuY;
   private final ClientTooltipPositioner sectionMenuPositioner;
   private boolean draggingScroller;
   private final List<TerminalStorageClickPayload.SlotTarget> remoteDragTargets;
   private final Long2ObjectOpenHashMap<ItemStack> remotePreviewItems;
   private final Int2ObjectOpenHashMap<ItemStack> playerPreviewItems;
   private ItemStack remotePreviewCarried;
   private ItemStack remoteDragInitialCarried;
   private boolean remoteDragShift;
   private int remoteDragMode;
   private int itemDragButton;
   private boolean itemDragShift;
   private boolean itemDragMoved;
   private boolean itemDragCreative;
   private int itemDragStartSlot;
   private final Set<Integer> itemDragSlots;
   private long nextOperationSequence;
   private long pendingOperationSequence;
   private long lastAppliedOperationSequence;
   private long nextShiftPreviewRequestId;
   private long latestShiftPreviewRequestId;
   private long appliedShiftPreviewRequestId;
   private boolean shiftDragCommitPending;
   private final Int2ObjectOpenHashMap<ItemStack> visualPlayerPreviewItems;
   private final Long2ObjectOpenHashMap<ItemStack> visualRemotePreviewItems;
   private ItemStack visualRemotePreviewCarried;

   public StorageManagerGUIScreen(StorageManagerGUIMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.variant = TerminalScreenLayout.SMALL;
      this.editingSection = -1;
      this.editingBackupName = "";
      this.titleScrollAnimations = new HashMap<>();
      this.lastTitleClickSection = -1;
      this.view = new TerminalViewPayload(false, 0, 0, List.of(), List.of(), List.of());
      this.cachedLayoutEntries = null;
      this.cachedLayoutRevision = -1;
      this.layoutRevision = 0;
      this.remoteItems = new Long2ObjectOpenHashMap<>();
      this.remoteSlotLimits = new Long2IntOpenHashMap();
      this.remoteConnectionIds = new Long2ObjectOpenHashMap<>();
      this.remoteStorageSlots = new Long2IntOpenHashMap();
      this.hoveredOrganizeSection = -1;
      this.hoveredFilterSection = -1;
      this.sectionMenuSourceButton = 0;
      this.sectionMenuSourceSection = -1;
      this.sectionSelectedOrganize = new HashMap<>();
      this.filterEditSections = new HashSet<>();
      this.filterEditItems = new HashMap<>();
      this.filterSectionMode = new HashMap<>();
      this.sectionMenuPositioner = new ClientTooltipPositioner() {
         public Vector2i positionTooltip(int guiLeft, int guiTop, int mouseX, int mouseY, int width, int height) {
            int x = Math.max(4, Math.min(StorageManagerGUIScreen.this.sectionMenuX, guiLeft - width - 4));
            int y = Math.max(4, Math.min(StorageManagerGUIScreen.this.sectionMenuY, guiTop - height - 4));
            return new Vector2i(x, y);
         }
      };
      this.remoteDragTargets = new ArrayList<>();
      this.remotePreviewItems = new Long2ObjectOpenHashMap<>();
      this.playerPreviewItems = new Int2ObjectOpenHashMap<>();
      this.remoteDragInitialCarried = ItemStack.EMPTY;
      this.itemDragButton = -1;
      this.itemDragStartSlot = -1;
      this.itemDragSlots = new LinkedHashSet<>();
      this.visualPlayerPreviewItems = new Int2ObjectOpenHashMap<>();
      this.visualRemotePreviewItems = new Long2ObjectOpenHashMap<>();
      this.player = inventory.player;
      this.imageWidth = TerminalScreenLayout.SMALL.imageWidth();
      this.imageHeight = TerminalScreenLayout.SMALL.imageHeight();
   }

   private int remoteSlotLimit(long slotKey, ItemStack stack) {
      return this.remoteSlotLimits.getOrDefault(slotKey, stack.getMaxStackSize());
   }

   private int measureTitleWidth(String text) {
      List<FormattedCharSequence> lines = this.font.split(Component.literal(text), Integer.MAX_VALUE);
      return lines.isEmpty() ? 0 : this.font.width(lines.get(0));
   }

   private double animatedTitleOffset(int sectionIndex, int maxScroll) {
      if (maxScroll <= 0) {
         this.titleScrollAnimations.remove(sectionIndex);
         return (double)0.0F;
      } else {
         TitleScrollAnimation animation = this.titleScrollAnimations.computeIfAbsent(sectionIndex, (ignored) -> new TitleScrollAnimation());
         if (animation.startedAtTick < 0L) {
            animation.startedAtTick = this.menu.world.getGameTime();
         }

         return TerminalTitleLogic.scrollOffset(this.menu.world.getGameTime(), animation.startedAtTick, TerminalTitleLogic.directionTicks(), TerminalTitleLogic.pauseTicks(), (double)maxScroll);
      }
   }

   private void drawClippedTitle(GuiGraphics graphics, Component label, int titleX, int titleY, double offset) {
      int boxTop = titleY - 1;
      graphics.enableScissor(titleX, boxTop, titleX + TerminalScreenLayout.TITLE_BOX_WIDTH, boxTop + this.titleBoxHeight);

      try {
         graphics.drawString(this.font, label, titleX + (int)Math.round(offset), titleY, MENU_TITLE_COLOR, false);
      } finally {
         graphics.disableScissor();
      }

   }

   private void updateTitleEditBoxRect(int sectionIndex) {
      if (!this.view.sections().isEmpty()) {
         if (sectionIndex >= 0 && sectionIndex < this.view.sections().size()) {
            TerminalScreenLayout.Rect area = this.variant.contentArea();
            int cursor = area.top() + 2 - this.view.scrollOffset();

            for(ClientLayoutEntry entry : this.layoutEntries()) {
               if (entry.notice() != null) {
                  cursor += this.sectionHeight(entry.notice().slotCount());
               } else {
                  if (entry.sectionIndex() == sectionIndex) {
                     this.titleEditBox.setX(this.leftPos + area.left() + 4);
                     this.titleEditBox.setY(this.topPos + cursor + 2);
                     this.titleEditBox.setWidth(TerminalScreenLayout.TITLE_BOX_WIDTH);
                     this.titleEditBox.setHeight(this.titleBoxHeight);
                     return;
                  }

                  TerminalViewPayload.Section section = entry.section();
                  int slotY = cursor + 12;
                  int bottom = slotY + Math.max(1, (section.slotCount() + TerminalScreenLayout.MAIN_COLUMNS - 1) / TerminalScreenLayout.MAIN_COLUMNS) * TerminalScreenLayout.SLOT_SIZE + 2;
                  cursor = bottom + 4;
               }
            }

         }
      }
   }

   private void startTitleEdit(int sectionIndex) {
      if (!this.view.sections().isEmpty()) {
         if (sectionIndex >= 0 && sectionIndex < this.view.sections().size()) {
            this.editingSection = sectionIndex;
            this.editingBackupName = (this.view.sections().get(sectionIndex)).label().getString();
            if (this.titleEditBox != null) {
               this.titleEditBox.setValue(this.editingBackupName);
               this.titleEditBox.setCursorPosition(this.editingBackupName.length());
               this.lastTitleClickSection = -1;
               this.titleEditBox.setVisible(true);
               this.setFocused(this.titleEditBox);
            }
         }
      }
   }

   private void confirmTitleEdit() {
      if (this.editingSection >= 0) {
         int sectionIndex = this.editingSection;
         String newName = this.titleEditBox == null ? this.editingBackupName : this.titleEditBox.getValue();
         if (newName == null) {
            newName = "";
         }

         this.editingSection = -1;
         if (this.titleEditBox != null) {
            this.titleEditBox.setValue("");
            this.titleEditBox.setVisible(false);
         }

         this.setFocused(null);
         this.setSectionLabel(sectionIndex, Component.literal(newName));
         ClientPlayNetworking.send(new TerminalRenamePayload(sectionIndex, newName));
      }
   }

   private void cancelTitleEdit() {
      if (this.editingSection >= 0) {
         int sectionIndex = this.editingSection;
         this.editingSection = -1;
         if (this.titleEditBox != null) {
            this.titleEditBox.setValue("");
            this.titleEditBox.setVisible(false);
         }

         this.setFocused(null);
         this.setSectionLabel(sectionIndex, Component.literal(this.editingBackupName));
      }
   }

   private void setSectionLabel(int sectionIndex, Component label) {
      if (!this.view.sections().isEmpty()) {
         if (sectionIndex >= 0 && sectionIndex < this.view.sections().size()) {
            List<TerminalViewPayload.Section> sections = new ArrayList<>(this.view.sections());
            TerminalViewPayload.Section current = sections.get(sectionIndex);
            sections.set(sectionIndex, new TerminalViewPayload.Section(label, current.coordinate(), current.status(), current.slotCount(), current.filterItems(), current.filterMode()));
            this.view = new TerminalViewPayload(this.view.big(), this.view.scrollOffset(), this.view.maxScroll(), sections, this.view.visibleSlots(), this.view.notices(), this.view.locked(), this.view.originTargets(), this.view.operationSequence(), this.view.playerSlots(), this.view.canMutateInventories(), this.view.advanced(), this.view.ownerName());
            ++this.layoutRevision;
         }
      }
   }

   private int titleSectionAt(double mouseX, double mouseY) {
      TerminalScreenLayout.Rect area = this.variant.contentArea();
      if (!this.isInside(area, mouseX, mouseY)) {
         return -1;
      } else {
         int cursor = area.top() + 2 - this.view.scrollOffset();

         for(ClientLayoutEntry entry : this.layoutEntries()) {
            if (entry.notice() != null) {
               cursor += this.sectionHeight(entry.notice().slotCount());
            } else {
               TerminalViewPayload.Section section = entry.section();
               int slotY = cursor + 12;
               int bottom = slotY + Math.max(1, (section.slotCount() + TerminalScreenLayout.MAIN_COLUMNS - 1) / TerminalScreenLayout.MAIN_COLUMNS) * TerminalScreenLayout.SLOT_SIZE + 2;
               int titleX = this.leftPos + area.left() + 4;
               int titleY = this.topPos + cursor + 2;
               if (titleY + this.titleBoxHeight > this.topPos + area.top() && titleY < this.topPos + area.bottom()) {
                  if (mouseX >= (double)titleX && mouseX < (double)(titleX + TerminalScreenLayout.TITLE_BOX_WIDTH) && mouseY >= (double)titleY && mouseY < (double)(titleY + this.titleBoxHeight)) {
                     return entry.sectionIndex();
                  }

                  cursor = bottom + 4;
               } else {
                  cursor = bottom + 4;
               }
            }
         }

         return -1;
      }
   }

   private void handleTitleClick(int sectionIndex, double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (!this.isFilterEditing(sectionIndex)) {
            long now = System.currentTimeMillis();
            boolean doubleClick = sectionIndex == this.lastTitleClickSection && now - this.lastTitleClickTime < 500L && Math.abs(mouseX - this.lastTitleClickX) < (double)3.0F && Math.abs(mouseY - this.lastTitleClickY) < (double)3.0F;
            this.lastTitleClickSection = sectionIndex;
            this.lastTitleClickTime = now;
            this.lastTitleClickX = mouseX;
            this.lastTitleClickY = mouseY;
            if (doubleClick) {
               this.startTitleEdit(sectionIndex);
            }

         }
      }
   }

   private int[] sectionButtonRects(int top) {
      TerminalScreenLayout.Rect main = this.variant.mainArea();
      int buttonY = this.topPos + top + 2;
      int organizeButtonRight = this.leftPos + main.right() - 2;
      int organizeButtonLeft = organizeButtonRight - SECTION_BUTTON_SIZE;
      int filterButtonRight = organizeButtonLeft - 2;
      int filterButtonLeft = filterButtonRight - SECTION_BUTTON_SIZE;
      return new int[]{filterButtonLeft, filterButtonRight, organizeButtonLeft, organizeButtonRight, buttonY};
   }

   private SectionButtonHit sectionButtonHit(double mouseX, double mouseY) {
      TerminalScreenLayout.Rect area = this.variant.contentArea();
      if (!this.isInside(area, mouseX, mouseY)) {
         return null;
      } else if (!this.view.advanced()) {
         return null;
      } else {
         int cursor = area.top() + 2 - this.view.scrollOffset();

         for(ClientLayoutEntry entry : this.layoutEntries()) {
            if (entry.notice() != null) {
               cursor += this.sectionHeight(entry.notice().slotCount());
            } else {
               TerminalViewPayload.Section section = entry.section();
               int rows = Math.max(1, (section.slotCount() + TerminalScreenLayout.MAIN_COLUMNS - 1) / TerminalScreenLayout.MAIN_COLUMNS);
               int slotY = cursor + 12;
               int bottom = slotY + rows * TerminalScreenLayout.SLOT_SIZE + 2;
               if (section.connectionStatus() == ConnectionStatus.AVAILABLE) {
                  int[] rects = this.sectionButtonRects(cursor);
                  int filterButtonLeft = rects[0];
                  int filterButtonRight = rects[1];
                  int organizeButtonLeft = rects[2];
                  int organizeButtonRight = rects[3];
                  int buttonY = rects[4];
                  boolean organizeHovered = mouseX >= (double)organizeButtonLeft && mouseX < (double)organizeButtonRight && mouseY >= (double)buttonY && mouseY < (double)(buttonY + SECTION_BUTTON_SIZE);
                  boolean filterHovered = mouseX >= (double)filterButtonLeft && mouseX < (double)filterButtonRight && mouseY >= (double)buttonY && mouseY < (double)(buttonY + SECTION_BUTTON_SIZE);
                  if (organizeHovered) {
                     return new SectionButtonHit(entry.sectionIndex(), SECTION_BUTTON_ORGANIZE);
                  }

                  if (filterHovered) {
                     return new SectionButtonHit(entry.sectionIndex(), SECTION_BUTTON_FILTER);
                  }
               }

               cursor = bottom + 4;
            }
         }

         return null;
      }
   }

   private void handleSectionButtonClick(int button) {
      if (button == 0) {
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   private void openSectionMenu(int sourceButton, int sourceSection, double mouseX, double mouseY) {
      this.sectionMenuOpen = true;
      this.sectionMenuSourceButton = sourceButton;
      this.sectionMenuSourceSection = sourceSection;
      this.sectionMenuX = (int)Math.round(mouseX) + SECTION_MENU_OFFSET_X;
      this.sectionMenuY = (int)Math.round(mouseY) + SECTION_MENU_OFFSET_Y;
   }

   private void closeSectionMenu() {
      this.sectionMenuOpen = false;
   }

   private void toggleFilterEditMode(int section) {
      if (section >= 0 && section < this.view.sections().size()) {
         if ((this.view.sections().get(section)).connectionStatus() == ConnectionStatus.AVAILABLE) {
            if (this.filterEditSections.contains(section)) {
               this.saveFilter(section);
            } else {
               this.startFilterEdit(section);
            }

         }
      }
   }

   private void startFilterEdit(int section) {
      TerminalViewPayload.Section sectionView = this.view.sections().get(section);
      this.filterEditSections.add(section);
      List<ItemStack> working = new ArrayList<>(sectionView.filterItems().size());

      for(ItemStack stack : sectionView.filterItems()) {
         working.add(stack.copy());
      }

      this.filterEditItems.put(section, working);
      this.filterSectionMode.putIfAbsent(section, sectionView.filterAllow() ? 0 : 1);
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private void saveFilter(int section) {
      TerminalViewPayload.Section sectionView = this.view.sections().get(section);
      List<ItemStack> saved = sectionView.filterItems();
      List<ItemStack> current = this.filterEditItems.getOrDefault(section, List.of());
      int savedMode = sectionView.filterAllow() ? 0 : 1;
      int currentMode = this.filterSectionMode.getOrDefault(section, savedMode);
      if (filterListsEqual(current, saved) && currentMode == savedMode) {
         this.filterEditSections.remove(section);
         this.filterEditItems.remove(section);
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      } else {
         ClientPlayNetworking.send(new TerminalFilterSavePayload(section, currentMode, current.stream().map(ItemStack::copy).filter((stack) -> !stack.isEmpty()).toList()));
         this.filterEditSections.remove(section);
         this.filterEditItems.remove(section);
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   private static boolean filterListsEqual(List<ItemStack> a, List<ItemStack> b) {
      if (a.size() != b.size()) {
         return false;
      } else {
         for(int i = 0; i < a.size(); ++i) {
            ItemStack left = a.get(i);
            ItemStack right = b.get(i);
            if (!left.isEmpty() || !right.isEmpty()) {
               if (left.isEmpty() || right.isEmpty()) {
                  return false;
               }

               if (left.getCount() != right.getCount()) {
                  return false;
               }

               if (!InventoryFilterLogic.matchesFilterItem(left, right)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void handleFilterSlotClick(int section, int slot, int button) {
      if (button == 0 || button == 1) {
         if (this.filterEditSections.contains(section)) {
            ItemStack carried = this.menu.getCarried();
            List<ItemStack> items = new ArrayList<>(this.filterEditItems.getOrDefault(section, List.of()));

            while(items.size() <= slot) {
               items.add(ItemStack.EMPTY);
            }

            boolean slotHas = !(items.get(slot)).isEmpty();
            if (carried.isEmpty()) {
               if (slotHas) {
                  items.set(slot, ItemStack.EMPTY);
               }
            } else {
               items.set(slot, InventoryFilterLogic.filterCopy(carried));
            }

            this.filterEditItems.put(section, items);
         }
      }
   }

   private void clearFilterSlot(int section, int slot) {
      if (this.filterEditSections.contains(section)) {
         List<ItemStack> items = this.filterEditItems.get(section);
         if (items != null) {
            while(items.size() <= slot) {
               items.add(ItemStack.EMPTY);
            }

            items.set(slot, ItemStack.EMPTY);
            this.filterEditItems.put(section, items);
         }
      }
   }

   private boolean isFilterEditing(int section) {
      return this.filterEditSections.contains(section);
   }

   private ItemStack filterSlotStack(int section, int slot) {
      List<ItemStack> items = this.filterEditItems.get(section);
      return items != null && slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
   }

   private void selectSectionMenuOption(int sourceButton, int option) {
      if (sourceButton == SECTION_BUTTON_ORGANIZE) {
         this.sectionSelectedOrganize.put(this.sectionMenuSourceSection, option);
      } else if (this.sectionMenuSourceSection >= 0) {
         this.filterSectionMode.put(this.sectionMenuSourceSection, option);
      }

      this.sectionMenuOpen = false;
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private int sectionMenuSelectedOrganizeFor(int section) {
      Integer selected = this.sectionSelectedOrganize.get(section);
      return selected == null ? 0 : selected;
   }

   private int sectionMenuSelectedFor(int sourceButton) {
      if (sourceButton == SECTION_BUTTON_ORGANIZE) {
         return this.sectionMenuSelectedOrganizeFor(this.sectionMenuSourceSection);
      } else {
         int section = this.sectionMenuSourceSection;
         if (section >= 0 && section < this.view.sections().size()) {
            Integer edited = this.filterSectionMode.get(section);
            if (edited != null) {
               return edited;
            } else {
               return (this.view.sections().get(section)).filterAllow() ? 0 : 1;
            }
         } else {
            return 0;
         }
      }
   }

   private int sectionMenuLineCount(int sourceButton) {
      return sourceButton == SECTION_BUTTON_ORGANIZE ? 1 + SECTION_MENU_ORGANIZE_OPTION_COUNT : 1 + FILTER_MENU_ITEM_COUNT;
   }

   private boolean sectionMenuLineSelectable(int sourceButton, int line) {
      return line > 0;
   }

   private MutableComponent sectionMenuLine(int sourceButton, int line) {
      if (sourceButton == SECTION_BUTTON_ORGANIZE && line == 0) {
         return Component.translatable("gui.simplestoragemanager.storage_manager_gui.organize_by").withStyle(Style.EMPTY.withColor(MENU_TITLE_COLOR));
      } else if (sourceButton == SECTION_BUTTON_FILTER && line == 0) {
         return Component.translatable("gui.simplestoragemanager.storage_manager_gui.filter_type").withStyle(Style.EMPTY.withColor(MENU_TITLE_COLOR));
      } else {
         int option = line - 1;
         MutableComponent text = Component.translatable(this.sectionMenuTranslationKey(sourceButton, option));
         int spaceWidth = this.font.width(" ");
         int checkmarkWidth = this.font.width(Component.literal(MENU_CHECK_MARK));
         int reservedWidth = checkmarkWidth + spaceWidth;
         int selected = this.sectionMenuSelectedFor(sourceButton);
         boolean isSelected = option == selected;
         String prefix = isSelected ? MENU_CHECK_MARK + " " : " ".repeat(Math.max(1, (int)Math.ceil((double)reservedWidth / (double)spaceWidth)));
         MutableComponent prefixComponent = Component.literal(prefix);
         if (isSelected) {
            prefixComponent = prefixComponent.withStyle(Style.EMPTY.withBold(true));
         }

         MutableComponent lineComponent = prefixComponent.append(text);
         int color = isSelected ? MENU_OPTION_COLOR : MENU_OPTION_COLOR_UNSELECTED;
         return lineComponent.withStyle(Style.EMPTY.withColor(color));
      }
   }

   private String sectionMenuTranslationKey(int sourceButton, int option) {
      if (sourceButton == SECTION_BUTTON_ORGANIZE) {
         String var3;
         switch (option) {
            case 0 -> var3 = "gui.simplestoragemanager.storage_manager_gui.organize_by_name";
            case 1 -> var3 = "gui.simplestoragemanager.storage_manager_gui.organize_by_quantity";
            case 2 -> var3 = "gui.simplestoragemanager.storage_manager_gui.organize_by_item_type";
            case 3 -> var3 = "gui.simplestoragemanager.storage_manager_gui.organize_by_mod";
            default -> var3 = "";
         }

         return var3;
      } else {
         String var10000;
         switch (option) {
            case 0 -> var10000 = "gui.simplestoragemanager.storage_manager_gui.filter_allow";
            case 1 -> var10000 = "gui.simplestoragemanager.storage_manager_gui.filter_deny";
            default -> var10000 = "";
         }

         return var10000;
      }
   }

   private int sectionMenuOptionAt(double mouseX, double mouseY, int sourceButton) {
      List<MutableComponent> lines = new ArrayList<>();
      int widest = 0;

      for(int line = 0; line < this.sectionMenuLineCount(sourceButton); ++line) {
         if (this.sectionMenuLineSelectable(sourceButton, line)) {
            MutableComponent menuLine = this.sectionMenuLine(sourceButton, line);
            lines.add(menuLine);
            widest = Math.max(widest, this.font.width(menuLine));
         }
      }

      int menuWidth = Math.max(1, widest + 4);
      Objects.requireNonNull(this.font);
      int lineHeight = 9;

      for(int line = 0; line < this.sectionMenuLineCount(sourceButton); ++line) {
         if (this.sectionMenuLineSelectable(sourceButton, line)) {
            MutableComponent menuLine = this.sectionMenuLine(sourceButton, line);
            List<FormattedCharSequence> split = this.font.split(menuLine, menuWidth);
            if (!split.isEmpty()) {
               int left = this.sectionMenuX;
               int top = this.sectionMenuY + line * lineHeight;
               int right = left + this.font.width(split.get(0));
               int bottom = top + lineHeight;
               if (mouseX >= (double)left && mouseX < (double)right && mouseY >= (double)top && mouseY <= (double)bottom) {
                  return line - 1;
               }
            }
         }
      }

      return -1;
   }

   private void renderSectionMenu(GuiGraphics graphics, int mouseX, int mouseY, int sourceButton) {
      List<MutableComponent> lines = new ArrayList<>();
      int widest = 0;

      for(int line = 0; line < this.sectionMenuLineCount(sourceButton); ++line) {
         MutableComponent menuLine = this.sectionMenuLine(sourceButton, line);
         lines.add(menuLine);
         widest = Math.max(widest, this.font.width(menuLine));
      }

      List<FormattedCharSequence> rendered = new ArrayList<>();
      int menuWidth = Math.max(1, widest + 4);

      for(MutableComponent line : lines) {
         rendered.addAll(this.font.split(line, menuWidth));
      }

      graphics.setTooltipForNextFrame(this.font, rendered, this.sectionMenuPositioner, mouseX, mouseY, false);
   }

   private boolean handleSectionMenuClick(double mouseX, double mouseY, int button, int sourceButton) {
      int option = this.sectionMenuOptionAt(mouseX, mouseY, sourceButton);
      if (option < 0 || button != 0 && button != 1) {
         this.closeSectionMenu();
         return true;
      } else {
         this.selectSectionMenuOption(sourceButton, option);
         return true;
      }
   }

   protected void init() {
      int configuredScale = Minecraft.getInstance().options.guiScale().get();
      boolean useBig = TerminalScaleChoice.useBig(configuredScale);
      this.variant = useBig ? TerminalScreenLayout.BIG : TerminalScreenLayout.SMALL;
      this.imageWidth = this.variant.imageWidth();
      this.imageHeight = this.variant.imageHeight();
      super.init();
      this.titleLabelX = this.variant.mainArea().left();
      this.titleLabelY = MAIN_TITLE_LABEL_Y;
      this.searchBar = new EditBox(this.font, this.leftPos + this.variant.searchArea().left(), this.topPos + this.variant.searchArea().top(), this.variant.searchArea().width(), this.variant.searchArea().height(), Component.translatable("gui.simplestoragemanager.storage_manager_gui.search_bar"));
      this.searchBar.setBordered(false);
      this.searchBar.setTextColor(0xFFFFFFFF);
      this.searchBar.setTextColorUneditable(0xFFFFFFFF);
      this.searchBar.setMaxLength(256);
      this.searchBar.setResponder((ignored) -> this.requestView(0));
      this.addRenderableWidget(this.searchBar);
      Objects.requireNonNull(this.font);
      this.titleBoxHeight = 9 + 2;
      this.titleEditBox = new EditBox(this.font, 0, 0, TerminalScreenLayout.TITLE_BOX_WIDTH, this.titleBoxHeight, Component.empty());
      this.titleEditBox.setBordered(false);
      this.titleEditBox.setTextColor(0xFFFFFFFF);
      this.titleEditBox.setTextColorUneditable(0xFFFFFFFF);
      this.titleEditBox.setMaxLength(256);
      this.titleEditBox.setResponder((ignored) -> {
      });
      this.titleEditBox.setVisible(false);
      this.addRenderableWidget(this.titleEditBox);
      this.requestView(0);
   }

   private void requestView(int scroll) {
      int clamped = Math.max(0, Math.min(this.view.maxScroll(), scroll));
      ClientPlayNetworking.send(new TerminalViewRequestPayload(this.searchBar == null ? "" : this.searchBar.getValue(), clamped, this.variant == TerminalScreenLayout.BIG));
   }

   public void applyView(TerminalViewPayload payload) {
      if (payload.operationSequence() >= this.pendingOperationSequence && payload.operationSequence() >= this.lastAppliedOperationSequence) {
         if (this.pendingOperationSequence <= 0L || payload.operationSequence() != this.pendingOperationSequence || !payload.playerSlots().isEmpty()) {
            if (this.itemDragShift || this.shiftDragCommitPending) {
               this.cancelPendingShiftDrag();
            }

            this.view = payload;
            this.lastAppliedOperationSequence = Math.max(this.lastAppliedOperationSequence, payload.operationSequence());
            ++this.layoutRevision;
            this.remoteItems.clear();
            this.remoteSlotLimits.clear();
            this.remoteConnectionIds.clear();
            this.remoteStorageSlots.clear();

            for(TerminalViewPayload.VisibleSlot slot : payload.visibleSlots()) {
               long slotKey = this.key(slot.section(), slot.slot());
               this.remoteItems.put(slotKey, slot.stack());
               this.remoteSlotLimits.put(slotKey, slot.slotLimit());
               this.remoteConnectionIds.put(slotKey, slot.connectionId());
               this.remoteStorageSlots.put(slotKey, slot.storageSlot());
            }

            this.applyOriginSlotLimits(payload);
            if (this.pendingOperationSequence > 0L && payload.operationSequence() >= this.pendingOperationSequence && !payload.playerSlots().isEmpty()) {
               for(TerminalViewPayload.PlayerSlot slot : payload.playerSlots()) {
                  if (slot.slot() >= 0 && slot.slot() < this.menu.slots.size()) {
                     (this.menu.slots.get(slot.slot())).set(slot.stack());
                  }
               }

               this.pendingOperationSequence = 0L;
               this.clearRemotePreview();
            } else if (this.pendingOperationSequence == 0L && this.itemDragButton < 0) {
               this.clearRemotePreview();
            }

         }
      }
   }

   private void applyOriginSlotLimits(TerminalViewPayload payload) {
      for(TerminalViewPayload.OriginTarget target : payload.originTargets()) {
         if (target.section() >= 0 && target.slot() >= 0 && target.slotLimit() > 0) {
            this.remoteSlotLimits.put(this.key(target.section(), target.slot()), target.slotLimit());
         }
      }

   }

   public void applyConnectionStatus(TerminalConnectionStatusPayload payload) {
      if (this.itemDragShift || this.shiftDragCommitPending) {
         this.cancelPendingShiftDrag();
      }

      int sectionIndex = payload.section();
      if (sectionIndex >= 0 && sectionIndex < this.view.sections().size()) {
         List<TerminalViewPayload.Section> sections = new ArrayList<>(this.view.sections());
         TerminalViewPayload.Section current = sections.get(sectionIndex);
         sections.set(sectionIndex, new TerminalViewPayload.Section(payload.label(), current.coordinate(), payload.status(), current.slotCount(), current.filterItems(), current.filterMode()));
         List<TerminalViewPayload.VisibleSlot> visibleSlots = new ArrayList<>();

         for(TerminalViewPayload.VisibleSlot slot : this.view.visibleSlots()) {
            if (slot.section() != sectionIndex) {
               visibleSlots.add(slot);
            }
         }

         visibleSlots.addAll(payload.visibleSlots());
         this.view = new TerminalViewPayload(this.view.big(), this.view.scrollOffset(), this.view.maxScroll(), sections, visibleSlots, this.view.notices(), this.view.locked(), this.view.originTargets(), this.view.operationSequence(), this.view.playerSlots(), this.view.canMutateInventories(), this.view.advanced(), this.view.ownerName());
         ++this.layoutRevision;
         this.remoteItems.keySet().removeIf((key) -> (int)(key >> 32) == sectionIndex);
         this.remoteSlotLimits.keySet().removeIf((key) -> (int)(key >> 32) == sectionIndex);
         this.remoteConnectionIds.keySet().removeIf((key) -> (int)(key >> 32) == sectionIndex);
         this.remoteStorageSlots.keySet().removeIf((key) -> (int)(key >> 32) == sectionIndex);

         for(TerminalViewPayload.VisibleSlot slot : payload.visibleSlots()) {
            long slotKey = this.key(slot.section(), slot.slot());
            this.remoteItems.put(slotKey, slot.stack());
            this.remoteSlotLimits.put(slotKey, slot.slotLimit());
            this.remoteConnectionIds.put(slotKey, slot.connectionId());
            this.remoteStorageSlots.put(slotKey, slot.storageSlot());
         }

         this.applyOriginSlotLimits(this.view);
         if (payload.refreshView()) {
            this.requestView(this.view.scrollOffset());
         }

      } else {
         if (payload.refreshView()) {
            this.requestView(this.view.scrollOffset());
         }

      }
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, this.variant == TerminalScreenLayout.BIG ? BIG_TEXTURE : SMALL_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
      this.renderPlayer(graphics, mouseX, mouseY);
      TerminalScreenLayout.Rect area = this.variant.contentArea();
      int clipLeft = this.leftPos + area.left();
      int clipTop = this.topPos + area.top();
      int clipRight = this.leftPos + area.right() + 1;
      int clipBottom = this.topPos + area.bottom() + 1;
      graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
      this.drawRemoteSections(graphics, mouseX, mouseY);
      graphics.disableScissor();
      boolean enabled = this.view.maxScroll() > 0 && !this.view.sections().isEmpty();
      ResourceLocation scroller = enabled ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
      int travel = this.variant.scrollTrack().height() - 15;
      int scrollerY = this.variant.scrollTrack().top();
      if (enabled && this.view.maxScroll() > 0) {
         scrollerY += (int)((double)this.view.scrollOffset() / (double)this.view.maxScroll() * (double)travel);
      }

      graphics.blit(RenderPipelines.GUI_TEXTURED, scroller, this.leftPos + this.variant.scrollTrack().left(), this.topPos + scrollerY, 0.0F, 0.0F, 12, 15, 12, 15);
      this.renderStaticItems(graphics);
      this.renderCrafting(graphics);
      ResourceLocation cross = this.isPointInside(this.variant.cross(), TerminalScreenLayout.BUTTON_SIZE, (double)mouseX, (double)mouseY) ? CROSS_HIGHLIGHTED_TEXTURE : CROSS_TEXTURE;
      graphics.blit(RenderPipelines.GUI_TEXTURED, cross, this.leftPos + this.variant.cross().x(), this.topPos + this.variant.cross().y(), 0.0F, 0.0F, TerminalScreenLayout.BUTTON_SIZE, TerminalScreenLayout.BUTTON_SIZE, TerminalScreenLayout.BUTTON_SIZE, TerminalScreenLayout.BUTTON_SIZE);
      boolean lockHovered = this.isPointInside(this.variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE, (double)mouseX, (double)mouseY);
      ResourceLocation lockTexture = this.view.locked() ? (lockHovered ? LOCKED_HIGHLIGHTED_TEXTURE : LOCKED_TEXTURE) : (lockHovered ? UNLOCKED_HIGHLIGHTED_TEXTURE : UNLOCKED_TEXTURE);
      graphics.blit(RenderPipelines.GUI_TEXTURED, lockTexture, this.leftPos + this.variant.lockButton().x(), this.topPos + this.variant.lockButton().y(), 0.0F, 0.0F, TerminalScreenLayout.LOCK_BUTTON_SIZE, TerminalScreenLayout.LOCK_BUTTON_SIZE, TerminalScreenLayout.LOCK_BUTTON_SIZE, TerminalScreenLayout.LOCK_BUTTON_SIZE);
   }

   private void renderPlayer(GuiGraphics graphics, int mouseX, int mouseY) {
      InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, this.leftPos + this.variant.playerPortrait().left(), this.topPos + this.variant.playerPortrait().top(), this.leftPos + this.variant.playerPortrait().right(), this.topPos + this.variant.playerPortrait().bottom(), 30, 0.0625F, (float)mouseX, (float)mouseY, this.player);
   }

   private void drawRemoteSections(GuiGraphics graphics, int mouseX, int mouseY) {
      TerminalScreenLayout.Rect area = this.variant.contentArea();
      int cursor = area.top() + 2 - this.view.scrollOffset();
      int viewportTop = area.top();
      int viewportBottom = area.bottom();
      this.hoveredRemote = null;
      this.hoveredUnavailableIcon = false;
      this.hoveredNoticeDismiss = false;
      this.hoveredOrganizeSection = -1;
      this.hoveredFilterSection = -1;

      for(ClientLayoutEntry entry : this.layoutEntries()) {
         if (entry.notice() != null) {
            int bottom = cursor + this.sectionHeight(entry.notice().slotCount()) - 4;
            if (bottom > viewportTop && cursor < viewportBottom) {
               this.drawReplacementNotice(graphics, entry.notice(), area.left(), cursor, area.right(), bottom, mouseX, mouseY);
            }

            cursor = bottom + 4;
         } else {
            int index = entry.sectionIndex();
            TerminalViewPayload.Section section = entry.section();
            int rows = Math.max(1, (section.slotCount() + TerminalScreenLayout.MAIN_COLUMNS - 1) / TerminalScreenLayout.MAIN_COLUMNS);
            int slotY = cursor + 12;
            int bottom = slotY + rows * TerminalScreenLayout.SLOT_SIZE + 2;
            if (bottom > viewportTop && cursor < viewportBottom) {
               boolean unavailable = section.connectionStatus() != ConnectionStatus.AVAILABLE;
               this.drawDottedFrame(graphics, area.left(), cursor, area.right(), bottom, unavailable ? -25787 : -3092272);
               boolean filterEditing = this.isFilterEditing(index);
               if (filterEditing) {
                  this.drawClippedTitle(graphics, Component.translatable("gui.simplestoragemanager.storage_manager_gui.filter_editor_title"), this.leftPos + area.left() + 4, this.topPos + cursor + 2, (double)0.0F);
               } else if (this.editingSection == index) {
                  this.updateTitleEditBoxRect(index);
               } else {
                  String titleText = section.label().getString();
                  int textWidth = this.measureTitleWidth(titleText);
                  int maxScroll = TerminalTitleLogic.scrollRange(textWidth, TerminalScreenLayout.TITLE_BOX_WIDTH);
                  double offset = this.animatedTitleOffset(index, maxScroll);
                  int titleX = this.leftPos + area.left() + 4;
                  int titleY = this.topPos + cursor + 2;
                  this.drawClippedTitle(graphics, section.label(), titleX, titleY, offset);
               }

               for(int slot = 0; slot < section.slotCount(); ++slot) {
                  int x = area.left() + slot % TerminalScreenLayout.MAIN_COLUMNS * TerminalScreenLayout.SLOT_SIZE;
                  int y = slotY + slot / TerminalScreenLayout.MAIN_COLUMNS * TerminalScreenLayout.SLOT_SIZE;
                  if (y + TerminalScreenLayout.SLOT_SIZE > viewportTop && y < viewportBottom) {
                     graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, this.leftPos + x, this.topPos + y, 0.0F, 0.0F, TerminalScreenLayout.SLOT_SIZE, TerminalScreenLayout.SLOT_SIZE, TerminalScreenLayout.SLOT_SIZE, TerminalScreenLayout.SLOT_SIZE);
                     ItemStack stack;
                     if (filterEditing) {
                        stack = this.filterSlotStack(index, slot);
                     } else {
                        stack = this.visualRemotePreviewItems.getOrDefault(this.key(index, slot), this.remoteItems.getOrDefault(this.key(index, slot), ItemStack.EMPTY));
                     }

                     if (!stack.isEmpty()) {
                        int itemX = this.leftPos + x + TerminalScreenLayout.ITEM_OFFSET;
                        int itemY = this.topPos + y + TerminalScreenLayout.ITEM_OFFSET;
                        graphics.renderItem(stack, itemX, itemY);
                        this.renderItemDecorations(graphics, stack, itemX, itemY);
                     }

                     if (this.isInside(area, (double)mouseX, (double)mouseY) && mouseX >= this.leftPos + x && mouseX < this.leftPos + x + TerminalScreenLayout.SLOT_SIZE && mouseY >= this.topPos + y && mouseY < this.topPos + y + TerminalScreenLayout.SLOT_SIZE) {
                        this.hoveredRemote = new RemoteHit(index, slot, stack);
                     }
                  }
               }

               if (unavailable) {
                  this.drawUnavailable(graphics, section, area.left(), cursor, area.right(), bottom, mouseX, mouseY);
               }

               if (!unavailable && this.view.advanced()) {
                  int[] rects = this.sectionButtonRects(cursor);
                  int filterButtonLeft = rects[0];
                  int filterButtonRight = rects[1];
                  int organizeButtonLeft = rects[2];
                  int organizeButtonRight = rects[3];
                  int buttonY = rects[4];
                  boolean organizeHovered = mouseX >= organizeButtonLeft && mouseX < organizeButtonRight && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;
                  boolean filterHovered = mouseX >= filterButtonLeft && mouseX < filterButtonRight && mouseY >= buttonY && mouseY < buttonY + SECTION_BUTTON_SIZE;
                  if (organizeHovered) {
                     this.hoveredOrganizeSection = index;
                  }

                  if (filterHovered) {
                     this.hoveredFilterSection = index;
                  }

                  graphics.blit(RenderPipelines.GUI_TEXTURED, ORGANIZER_TEXTURE, organizeButtonLeft, buttonY, 0.0F, 0.0F, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE);
                  graphics.blit(RenderPipelines.GUI_TEXTURED, FILTER_TEXTURE, filterButtonLeft, buttonY, 0.0F, 0.0F, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE, SECTION_BUTTON_SIZE);
               }

               cursor = bottom + 4;
            } else {
               cursor = bottom + 4;
            }
         }
      }

      if (this.view.sections().isEmpty() && this.view.notices().isEmpty()) {
         Component message = Component.translatable("screen.simplestoragemanager.no_inventories");
         List<FormattedCharSequence> lines = this.font.split(message, Math.max(1, area.width() - 12));
         Objects.requireNonNull(this.font);
         int lineHeight = 9;
         int firstY = area.top() + Math.max(0, (area.height() - lines.size() * lineHeight) / 2);
         int centerX = this.leftPos + (area.left() + area.right()) / 2;

         for(int i = 0; i < lines.size(); ++i) {
            graphics.drawCenteredString(this.font, lines.get(i), centerX, this.topPos + firstY + i * lineHeight, -3092272);
         }
      }

   }

   private void drawUnavailable(GuiGraphics graphics, TerminalViewPayload.Section section, int left, int top, int right, int bottom, int mouseX, int mouseY) {
      int sectionWidth = right - left + 1;
      int sectionHeight = bottom - top + 1;
      int background = -535490033;
      graphics.fill(this.leftPos + left, this.topPos + top, this.leftPos + right + 1, this.topPos + bottom + 1, background);
      graphics.enableScissor(this.leftPos + left, this.topPos + top, this.leftPos + right + 1, this.topPos + bottom + 1);

      for(int stripe = -sectionHeight / 3; stripe < sectionWidth; stripe += 10) {
         for(int y = top; y <= bottom; y += 3) {
            int x = left + stripe + (y - top) / 3;
            graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + 3, this.topPos + Math.min(y + 3, bottom + 1), -25787);
         }
      }

      graphics.disableScissor();
      String var10000;
      switch (section.connectionStatus()) {
         case BLOCK_REMOVED -> var10000 = "screen.simplestoragemanager.connection_lost";
         case CHUNK_UNLOADED -> var10000 = "screen.simplestoragemanager.chunk_unloaded";
         case DIMENSION_MISSING -> var10000 = "screen.simplestoragemanager.dimension_missing";
         case BASIC_UNAVAILABLE -> var10000 = "screen.simplestoragemanager.connection_lost";
         case AVAILABLE -> var10000 = "screen.simplestoragemanager.connection_lost";
         default -> throw new MatchException(null, null);
      }

      String key = var10000;
      Component message = Component.translatable(key, new Object[]{section.coordinate()});
      int maxWidth = Math.max(1, right - left - 12);
      List<FormattedCharSequence> lines = this.font.split(message, maxWidth);
      int lineHeight = 9;
      int firstY = top + Math.max(0, (bottom - top + 1 - lines.size() * lineHeight) / 2);
      int textWidth = 0;

      for(FormattedCharSequence line : lines) {
         textWidth = Math.max(textWidth, this.font.width(line));
      }

      int textLeft = left + (sectionWidth - Math.min(sectionWidth, textWidth + 8)) / 2;
      int textRight = textLeft + Math.min(sectionWidth, textWidth + 8);
      graphics.fill(this.leftPos + textLeft, this.topPos + firstY - 2, this.leftPos + textRight, this.topPos + firstY + lines.size() * lineHeight + 1, background);

      for(int i = 0; i < lines.size(); ++i) {
         graphics.drawCenteredString(this.font, lines.get(i), this.leftPos + (left + right) / 2, this.topPos + firstY + i * lineHeight, -25787);
      }

      this.drawDottedFrame(graphics, left, top, right, bottom, -25787);
      int iconSize = TerminalScreenLayout.ALERT_ICON_SIZE;
      int iconX = right - iconSize - 1;
      int iconY = top + 1;
      boolean hovered = this.isPointInside(new TerminalScreenLayout.Point(iconX, iconY), iconSize, (double)mouseX, (double)mouseY);
      this.hoveredUnavailableIcon |= hovered;
      ResourceLocation icon = hovered ? REPORT_HIGHLIGHTED_TEXTURE : REPORT_TEXTURE;
      graphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.leftPos + iconX, this.topPos + iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
   }

   private void drawReplacementNotice(GuiGraphics graphics, TerminalViewPayload.Notice notice, int left, int top, int right, int bottom, int mouseX, int mouseY) {
      graphics.fill(this.leftPos + left + 2, this.topPos + top, this.leftPos + right, this.topPos + bottom + 1, -535081689);
      this.drawDottedFrame(graphics, left, top, right, bottom, -4849732);
      Component message = Component.translatable("screen.simplestoragemanager.replacement_notice", new Object[]{notice.pos().getX(), notice.pos().getY(), notice.pos().getZ()});
      int maxWidth = Math.max(1, right - left - 16);
      List<FormattedCharSequence> lines = this.font.split(message, maxWidth);
      int lineHeight = 9;
      int firstY = top + Math.max(0, (bottom - top + 1 - lines.size() * lineHeight) / 2);

      for(int i = 0; i < lines.size(); ++i) {
         graphics.drawCenteredString(this.font, lines.get(i), this.leftPos + (left + right) / 2, this.topPos + firstY + i * lineHeight, -4849732);
      }

      int crossX = right - 9;
      int crossY = top + 2;
      boolean hovered = mouseX >= this.leftPos + crossX && mouseX < this.leftPos + crossX + 7 && mouseY >= this.topPos + crossY && mouseY < this.topPos + crossY + 7;
      this.hoveredNoticeDismiss |= hovered;
      ResourceLocation cross = hovered ? CROSS_HIGHLIGHTED_TEXTURE : CROSS_TEXTURE;
      graphics.blit(RenderPipelines.GUI_TEXTURED, cross, this.leftPos + crossX, this.topPos + crossY, 0.0F, 0.0F, 7, 7, 7, 7);
   }

   private void renderStaticItems(GuiGraphics graphics) {
      for(int i = 0; i < 4; ++i) {
         this.renderSlotItem(graphics, this.menu.slots.get(this.menu.armorStart + i), this.variant.armor()[i]);
      }

      this.renderSlotItem(graphics, this.menu.slots.get(this.menu.offhandIndex), this.variant.offhand());
      this.renderSlotItem(graphics, this.menu.slots.get(this.menu.upgradeStart), this.variant.upgrade3());
      this.renderSlotItem(graphics, this.menu.slots.get(this.menu.upgradeStart + 1), this.variant.upgrade2());
      this.renderSlotItem(graphics, this.menu.slots.get(this.menu.upgradeStart + 2), this.variant.upgrade1());

      for(int row = 0; row < 3; ++row) {
         for(int column = 0; column < TerminalScreenLayout.MAIN_COLUMNS; ++column) {
            this.renderSlotItem(graphics, this.menu.slots.get(this.menu.mainInventoryStart + row * TerminalScreenLayout.MAIN_COLUMNS + column), new TerminalScreenLayout.Point(this.variant.inventoryOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.inventoryOrigin().y() + row * TerminalScreenLayout.SLOT_SIZE));
         }
      }

      for(int column = 0; column < TerminalScreenLayout.MAIN_COLUMNS; ++column) {
         this.renderSlotItem(graphics, this.menu.slots.get(this.menu.hotbarStart + column), new TerminalScreenLayout.Point(this.variant.hotbarOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.hotbarOrigin().y()));
      }

   }

   private void renderCrafting(GuiGraphics graphics) {
      for(int row = 0; row < 3; ++row) {
         for(int column = 0; column < 3; ++column) {
            this.renderSlotItem(graphics, this.menu.slots.get(this.menu.craftingGridStart + row * 3 + column), new TerminalScreenLayout.Point(this.variant.craftOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.craftOrigin().y() + row * TerminalScreenLayout.SLOT_SIZE));
         }
      }

      this.renderSlotItem(graphics, this.menu.slots.get(this.menu.craftingResultIndex), this.variant.result());
   }

   private void renderSlotItem(GuiGraphics graphics, Slot slot, TerminalScreenLayout.Point point) {
      ItemStack stack = this.visualPlayerPreviewItems.getOrDefault(slot.index, slot.getItem());
      if (!stack.isEmpty()) {
         graphics.renderItem(stack, this.leftPos + point.x() + TerminalScreenLayout.ITEM_OFFSET, this.topPos + point.y() + TerminalScreenLayout.ITEM_OFFSET);
         this.renderItemDecorations(graphics, stack, this.leftPos + point.x() + TerminalScreenLayout.ITEM_OFFSET, this.topPos + point.y() + TerminalScreenLayout.ITEM_OFFSET);
      }
   }

   private void renderItemDecorations(GuiGraphics graphics, ItemStack stack, int x, int y) {
      if (stack.getCount() <= 999) {
         graphics.renderItemDecorations(this.font, stack, x, y);
      } else {
         graphics.pose().pushMatrix();
         graphics.pose().translate((float)(x + 19), (float)(y + 9));
         graphics.pose().scale(0.65F, 0.65F);
         graphics.renderItemDecorations(this.font, stack, -19, -9);
         graphics.pose().popMatrix();
      }
   }

   private void drawDottedFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
      for(int x = left; x <= right; x += 4) {
         graphics.fill(this.leftPos + x, this.topPos + top, this.leftPos + Math.min(x + 2, right + 1), this.topPos + top + 1, color);
         graphics.fill(this.leftPos + x, this.topPos + bottom, this.leftPos + Math.min(x + 2, right + 1), this.topPos + bottom + 1, color);
      }

      for(int y = top; y <= bottom; y += 4) {
         graphics.fill(this.leftPos + left, this.topPos + y, this.leftPos + left + 1, this.topPos + Math.min(y + 2, bottom + 1), color);
         graphics.fill(this.leftPos + right, this.topPos + y, this.leftPos + right + 1, this.topPos + Math.min(y + 2, bottom + 1), color);
      }

   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, VANILLA_TITLE_COLOR, false);
      if (this.view.advanced()) {
         graphics.drawString(this.font, Component.literal(ADVANCED_MARK).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), this.titleLabelX + this.font.width(this.title), this.titleLabelY, MENU_TITLE_COLOR, false);
      }

      if (!this.view.ownerName().getString().isEmpty()) {
         graphics.pose().pushMatrix();
         Matrix3x2fStack var10000 = graphics.pose();
         float var10001 = (float)this.titleLabelX;
         int var4 = this.titleLabelY;
         Objects.requireNonNull(this.font);
         var10000.translate(var10001, (float)(var4 + 9 + 2));
         graphics.pose().scale(OWNER_NAME_SCALE, OWNER_NAME_SCALE);
         graphics.drawString(this.font, Component.literal(this.view.ownerName().getString()).withStyle(Style.EMPTY.withBold(true).withItalic(true)), 0, 0, 0xFF404040, false);
         graphics.pose().popMatrix();
      }

   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ItemStack carried = this.menu.getCarried();
      if (this.visualRemotePreviewCarried != null) {
         this.menu.setCarried(this.visualRemotePreviewCarried.copy());
      }

      if (this.editingSection >= 0 && this.titleEditBox != null) {
         this.updateTitleEditBoxRect(this.editingSection);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
      if (this.visualRemotePreviewCarried != null) {
         this.menu.setCarried(carried);
      }

      if (this.sectionMenuOpen) {
         this.renderSectionMenu(graphics, mouseX, mouseY, this.sectionMenuSourceButton);
      }

      if (!this.sectionMenuOpen && this.isPointInside(this.variant.cross(), TerminalScreenLayout.BUTTON_SIZE, (double)mouseX, (double)mouseY)) {
         graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.simplestoragemanager.storage_manager_gui.clear_crafting"), mouseX, mouseY);
      } else if (this.isPointInside(this.variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE, (double)mouseX, (double)mouseY)) {
         List<FormattedCharSequence> lines = new ArrayList<>();
         String stateKey = this.view.locked() ? "screen.simplestoragemanager.inventory_lock.disable" : "screen.simplestoragemanager.inventory_lock.enable";
         lines.addAll(this.font.split(Component.translatable(stateKey).withStyle(ChatFormatting.WHITE), 180));
         if (hasShiftDown()) {
            String descriptionKey = this.view.locked() ? "screen.simplestoragemanager.inventory_lock.disabled_description" : "screen.simplestoragemanager.inventory_lock.enabled_description";
            lines.addAll(this.font.split(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY), 180));
         } else {
            lines.addAll(this.font.split(Component.translatable("screen.simplestoragemanager.inventory_lock.shift_hint").withStyle(ChatFormatting.GRAY), 180));
         }

         graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
      } else if (!this.sectionMenuOpen && this.hoveredOrganizeSection >= 0 && this.view.advanced()) {
         graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.simplestoragemanager.storage_manager_gui.organize_inventory"), mouseX, mouseY);
      } else if (!this.sectionMenuOpen && this.hoveredFilterSection >= 0 && this.view.advanced()) {
         if (this.isFilterEditing(this.hoveredFilterSection)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.simplestoragemanager.storage_manager_gui.close_inventory_filter"), mouseX, mouseY);
         } else {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.simplestoragemanager.storage_manager_gui.open_inventory_filter"), mouseX, mouseY);
         }
      } else if (this.hoveredNoticeDismiss) {
         graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.simplestoragemanager.dismiss_notice"), mouseX, mouseY);
      } else if (this.hoveredUnavailableIcon) {
         List<FormattedCharSequence> lines = this.font.split(Component.translatable("screen.simplestoragemanager.unavailable_tooltip"), 180);
         graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
      } else if (this.hoveredRemote != null && !this.hoveredRemote.stack().isEmpty()) {
         graphics.setTooltipForNextFrame(this.font, this.hoveredRemote.stack(), mouseX, mouseY);
      } else {
         Slot hoveredSlot = this.staticSlotAt((double)mouseX, (double)mouseY);
         if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hoveredSlot.getItem(), mouseX, mouseY);
         }
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.sectionMenuOpen) {
         this.handleSectionMenuClick(mouseX, mouseY, button, this.sectionMenuSourceButton);
         return true;
      } else {
         if (!this.isInside(this.variant.searchArea(), mouseX, mouseY) && this.searchBar != null && this.searchBar.isFocused()) {
            this.setFocused(null);
         }

         if (this.isInside(this.variant.searchArea(), mouseX, mouseY)) {
            if (button == 1 && this.searchBar != null) {
               this.searchBar.setValue("");
               this.setFocused(this.searchBar);
               return true;
            } else {
               return super.mouseClicked(mouseX, mouseY, button);
            }
         } else if (this.editingSection >= 0 && this.titleEditBox != null && this.titleEditBox.isFocused()) {
            boolean insideBox = mouseX >= (double)this.titleEditBox.getX() && mouseX < (double)(this.titleEditBox.getX() + this.titleEditBox.getWidth()) && mouseY >= (double)this.titleEditBox.getY() && mouseY < (double)(this.titleEditBox.getY() + this.titleEditBox.getHeight());
            if (insideBox) {
               return super.mouseClicked(mouseX, mouseY, button);
            } else {
               this.cancelTitleEdit();
               return true;
            }
         } else if (this.pendingOperationSequence > 0L && button == 0 && hasShiftDown()) {
            return true;
         } else if (this.isPointInside(this.variant.lockButton(), TerminalScreenLayout.LOCK_BUTTON_SIZE, mouseX, mouseY)) {
            if (button == 0) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               ClientPlayNetworking.send(new TerminalLockTogglePayload());
            }

            return true;
         } else if (this.isPointInside(this.variant.cross(), TerminalScreenLayout.BUTTON_SIZE, mouseX, mouseY)) {
            if (button == 0) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            ClientPlayNetworking.send(new TerminalClearCraftingPayload());
            return true;
         } else {
            SectionButtonHit sectionButton = this.sectionButtonHit(mouseX, mouseY);
            if (sectionButton != null) {
               int sourceSection = sectionButton.section();
               if (button == 1) {
                  this.openSectionMenu(sectionButton.kind(), sourceSection, mouseX, mouseY);
               } else if (sectionButton.kind() == SECTION_BUTTON_FILTER) {
                  this.toggleFilterEditMode(sourceSection);
               } else if (sectionButton.kind() == SECTION_BUTTON_ORGANIZE) {
                  this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                  int option = this.sectionMenuSelectedOrganizeFor(sourceSection);
                  ClientPlayNetworking.send(new TerminalOrganizePayload(sourceSection, option));
               }

               this.handleSectionButtonClick(button);
               return true;
            } else {
               int clickedTitleSection = this.titleSectionAt(mouseX, mouseY);
               if (clickedTitleSection >= 0) {
                  this.handleTitleClick(clickedTitleSection, mouseX, mouseY, button);
                  return true;
               } else if (this.clickNotice(mouseX, mouseY)) {
                  return true;
               } else {
                  RemoteHit remoteHit = this.remoteHitAt(mouseX, mouseY);
                  if (remoteHit != null && this.isFilterEditing(remoteHit.section())) {
                     this.handleFilterSlotClick(remoteHit.section(), remoteHit.slot(), button);
                     return true;
                  } else if (remoteHit != null) {
                     if (!this.view.canMutateInventories()) {
                        return true;
                     } else if (this.isCreativePickButton(button)) {
                        if (!this.menu.getCarried().isEmpty()) {
                           this.beginRemoteDrag(remoteHit, button, false, true);
                           return true;
                        } else {
                           if (!remoteHit.stack().isEmpty()) {
                              ClientPlayNetworking.send(new TerminalStorageClickPayload(remoteHit.section(), remoteHit.slot(), 2, false));
                           }

                           return true;
                        }
                     } else if (button != 0 && button != 1) {
                        return true;
                     } else {
                        int remoteButton = button == 1 ? 1 : 0;
                        boolean shiftRemoval = button == 0 && hasShiftDown() && this.menu.getCarried().isEmpty();
                        if (!shiftRemoval && (hasShiftDown() || this.menu.getCarried().isEmpty()) && (button != 0 || this.menu.getCarried().isEmpty())) {
                           ClientPlayNetworking.send(new TerminalStorageClickPayload(remoteHit.section(), remoteHit.slot(), remoteButton, shiftRemoval));
                           return true;
                        } else {
                           this.beginRemoteDrag(remoteHit, button, shiftRemoval, false);
                           return true;
                        }
                     }
                  } else {
                     Slot staticSlot = this.staticSlotAt(mouseX, mouseY);
                     if (staticSlot != null) {
                        if (this.menu.isTerminalSlot(staticSlot.index)) {
                           return true;
                        } else if (this.isCreativePickButton(button)) {
                           if (this.menu.getCarried().isEmpty()) {
                              if (staticSlot.index < this.menu.upgradeStart || staticSlot.index >= this.menu.upgradeStart + 3 && staticSlot.index != this.menu.craftingResultIndex) {
                                 this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, staticSlot.index, button, ClickType.CLONE, this.player);
                              }
                           } else if (this.menu.canDragTo(staticSlot)) {
                              this.beginStaticDrag(staticSlot, button, false, true);
                           }

                           return true;
                        } else if (button != 0 && button != 1) {
                           return true;
                        } else if (button == 0 && hasShiftDown() && this.menu.getCarried().isEmpty()) {
                           if (!this.view.canMutateInventories()) {
                              return true;
                           } else {
                              this.beginStaticDrag(staticSlot, button, true, false);
                              return true;
                           }
                        } else if (!hasShiftDown() && !this.menu.getCarried().isEmpty() && this.menu.canDragTo(staticSlot)) {
                           this.beginStaticDrag(staticSlot, button, false, false);
                           return true;
                        } else {
                           this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, staticSlot.index, button == 1 ? 1 : 0, hasShiftDown() && button == 0 ? ClickType.QUICK_MOVE : ClickType.PICKUP, this.player);
                           return true;
                        }
                     } else if (this.editingSection >= 0) {
                        return true;
                     } else if (this.isInside(this.variant.scrollTrack(), mouseX, mouseY)) {
                        this.draggingScroller = this.view.maxScroll() > 0;
                        this.updateScroll(mouseY);
                        return true;
                     } else {
                        return super.mouseClicked(mouseX, mouseY, button);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isCreativePickButton(int button) {
      return this.player.getAbilities().instabuild && this.minecraft.options.keyPickItem.matchesMouse(button);
   }

   private boolean isManualDragStartAllowed(ItemStack carried, boolean startInOriginInventory, boolean startInPlayerInventory) {
      if (!carried.isEmpty() && this.view.locked()) {
         boolean carriedHasOrigin = false;

         for(TerminalViewPayload.OriginTarget origin : this.view.originTargets()) {
            if (ItemStack.isSameItemSameComponents(origin.item(), carried)) {
               carriedHasOrigin = true;
               break;
            }
         }

         return !carriedHasOrigin ? true : ManualInsertionRules.isManualDragStartAllowed(this.view.locked(), carriedHasOrigin, startInOriginInventory, startInPlayerInventory);
      } else {
         return true;
      }
   }

   private boolean isPlayerInventoryDragStartSlot(int slotIndex) {
      return slotIndex >= this.menu.mainInventoryStart && slotIndex < this.menu.hotbarStart + 9;
   }

   private boolean isCraftingDragStartSlot(int slotIndex) {
      return slotIndex >= this.menu.craftingGridStart && slotIndex < this.menu.craftingGridStart + 9;
   }

   private boolean isRemoteDragStartInOrigin(RemoteHit hit) {
      ItemStack carried = this.menu.getCarried();
      if (!carried.isEmpty() && this.view.locked()) {
         List<TerminalViewPayload.OriginTarget> origins = this.view.originTargets();
         boolean carriedHasOrigin = false;

         for(TerminalViewPayload.OriginTarget origin : origins) {
            if (ItemStack.isSameItemSameComponents(origin.item(), carried)) {
               carriedHasOrigin = true;
               break;
            }
         }

         if (!carriedHasOrigin) {
            return true;
         } else {
            UUID hitConnectionId = this.remoteConnectionIds.get(this.key(hit.section(), hit.slot()));
            UUID originConnectionId = null;

            for(TerminalViewPayload.OriginTarget origin : origins) {
               if (ItemStack.isSameItemSameComponents(origin.item(), carried)) {
                  UUID candidate = this.remoteConnectionIds.get(this.key(origin.section(), origin.slot()));
                  if (candidate != null) {
                     originConnectionId = candidate;
                     break;
                  }
               }
            }

            return originConnectionId != null && hitConnectionId != null && hitConnectionId.equals(originConnectionId);
         }
      } else {
         return true;
      }
   }

   private void clearItemDragState() {
      this.itemDragButton = -1;
      this.itemDragShift = false;
      this.itemDragMoved = false;
      this.itemDragCreative = false;
      this.itemDragStartSlot = -1;
      this.itemDragSlots.clear();
      this.remoteDragMode = 0;
      this.remoteDragShift = false;
      this.remoteDragTargets.clear();
      this.remoteDragInitialCarried = ItemStack.EMPTY;
   }

   private void beginRemoteDrag(RemoteHit hit, int button, boolean shift, boolean creative) {
      this.itemDragButton = button;
      this.itemDragShift = shift;
      this.itemDragCreative = creative;
      this.itemDragMoved = false;
      this.itemDragStartSlot = -1;
      this.itemDragSlots.clear();
      this.remoteDragMode = creative ? 2 : (button == 1 ? 1 : 0);
      this.remoteDragShift = shift;
      this.remoteDragInitialCarried = this.menu.getCarried().copy();
      this.remoteDragTargets.clear();
      this.addRemoteDragTarget(hit);
      this.updateCombinedPreview();
   }

   private void beginStaticDrag(Slot slot, int button, boolean shift, boolean creative) {
      this.itemDragButton = button;
      this.itemDragShift = shift;
      this.itemDragCreative = creative;
      this.itemDragMoved = false;
      this.itemDragStartSlot = slot.index;
      this.itemDragSlots.clear();
      this.itemDragSlots.add(slot.index);
      this.remoteDragMode = creative ? 2 : (button == 1 ? 1 : 0);
      this.remoteDragShift = shift;
      this.remoteDragInitialCarried = this.menu.getCarried().copy();
      this.remoteDragTargets.clear();
      this.updateCombinedPreview();
   }

   private void addStaticDragTarget(Slot slot) {
      if (!this.itemDragShift || this.itemDragStartSlot >= 0) {
         if (slot != null && this.itemDragSlots.size() < 256 && this.itemDragSlots.add(slot.index)) {
            if (this.itemDragSlots.size() > 1 || !this.remoteDragTargets.isEmpty()) {
               this.itemDragMoved = true;
            }

            this.updateCombinedPreview();
            this.finishDragWhenCarriedIsEmpty();
         }
      }
   }

   private void updateCombinedPreview() {
      if (!this.itemDragSlots.isEmpty()) {
         this.updateStaticPreview();
      } else {
         this.remotePreviewCarried = this.remoteDragInitialCarried.copy();
      }

      if (!this.remoteDragTargets.isEmpty()) {
         this.updateRemotePreview();
      }

   }

   private void updateStaticPreview() {
      this.playerPreviewItems.clear();
      ItemStack remaining = this.menu.getCarried().copy();
      if (!this.itemDragShift && !remaining.isEmpty()) {
         Int2ObjectOpenHashMap<ItemStack> working = new Int2ObjectOpenHashMap<>();
         List<Integer> eligible = new ArrayList<>();

         for(int index : this.itemDragSlots) {
            Slot slot = this.menu.slots.get(index);
            ItemStack existing = slot.getItem().copy();
            if (this.menu.canDragTo(slot) && slot.mayPlace(remaining) && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, remaining)) && (this.itemDragCreative || remaining.getCount() > eligible.size())) {
               int limit = Math.min(remaining.getMaxStackSize(), slot.getMaxStackSize(remaining));
               if (limit > (existing.isEmpty() ? 0 : existing.getCount())) {
                  eligible.add(index);
                  working.put(index, existing);
               }
            }
         }

         if (this.itemDragCreative) {
            if (this.player.getAbilities().instabuild) {
               for(int index : eligible) {
                  Slot slot = this.menu.slots.get(index);
                  int count = Math.min(remaining.getCount(), slot.getMaxStackSize(remaining));
                  ItemStack existing = working.get(index);
                  working.put(index, existing.isEmpty() ? remaining.copyWithCount(count) : existing.copyWithCount(Math.max(existing.getCount(), count)));
               }
            }
         } else {
            int amount = this.itemDragButton == 1 ? 1 : (eligible.isEmpty() ? 0 : remaining.getCount() / eligible.size());

            for(int index : eligible) {
               if (remaining.isEmpty() || amount <= 0) {
                  break;
               }

               Slot slot = this.menu.slots.get(index);
               ItemStack existing = working.get(index);
               int currentCount = existing.isEmpty() ? 0 : existing.getCount();
               int limit = Math.min(remaining.getMaxStackSize(), slot.getMaxStackSize(remaining));
               int inserted = Math.min(amount, limit - currentCount);
               if (inserted > 0) {
                  working.put(index, existing.isEmpty() ? remaining.copyWithCount(inserted) : existing.copyWithCount(currentCount + inserted));
                  remaining.shrink(inserted);
               }
            }
         }

         this.playerPreviewItems.putAll(working);
         this.remotePreviewCarried = this.itemDragCreative ? this.remoteDragInitialCarried.copy() : remaining;
         this.publishVisualPreview();
      } else {
         if (this.itemDragShift) {
            this.updateShiftPreview();
         }

         this.remotePreviewCarried = remaining;
      }
   }

   private void updateShiftPreview() {
      if (this.isUpgradeExtractionShiftDrag()) {
         this.playerPreviewItems.clear();
         this.remotePreviewItems.clear();
         this.updateUpgradeExtractionPreview();
         this.publishVisualPreview();
      }

      this.latestShiftPreviewRequestId = ++this.nextShiftPreviewRequestId;
      ClientPlayNetworking.send(new TerminalShiftDragPreviewRequestPayload(this.latestShiftPreviewRequestId, List.copyOf(this.itemDragSlots)));
   }

   private boolean isUpgradeExtractionShiftDrag() {
      return this.itemDragShift && this.itemDragStartSlot >= this.menu.upgradeStart && this.itemDragStartSlot < this.menu.upgradeStart + 3 && !this.itemDragSlots.isEmpty() && this.itemDragSlots.stream().allMatch((index) -> index >= this.menu.upgradeStart && index < this.menu.upgradeStart + 3);
   }

   private void updateUpgradeExtractionPreview() {
      boolean hasLevel2 = !(this.menu.slots.get(this.menu.upgradeStart + 1)).getItem().isEmpty();
      boolean hasLevel3 = !(this.menu.slots.get(this.menu.upgradeStart + 0)).getItem().isEmpty();
      Int2ObjectOpenHashMap<ItemStack> playerStacks = new Int2ObjectOpenHashMap<>();
      Int2ObjectOpenHashMap<ItemStack> originalPlayerStacks = new Int2ObjectOpenHashMap<>();

      for(int index = this.menu.mainInventoryStart; index < this.menu.hotbarStart + 9; ++index) {
         if (!this.menu.isTerminalSlot(index)) {
            ItemStack stack = (this.menu.slots.get(index)).getItem().copy();
            playerStacks.put(index, stack);
            originalPlayerStacks.put(index, stack.copy());
         }
      }

      for(int sourceIndex : this.itemDragSlots) {
         int upgradeSlot = sourceIndex - this.menu.upgradeStart;
         if (UpgradeSlotRules.canRemove(upgradeSlot, hasLevel2, hasLevel3)) {
            ItemStack source = (this.menu.slots.get(sourceIndex)).getItem().copy();
            if (!source.isEmpty()) {
               ItemStack moving = source.copy();

               for(int pass = 0; pass < 2 && !moving.isEmpty(); ++pass) {
                  for(int targetIndex = this.menu.mainInventoryStart; targetIndex < this.menu.hotbarStart + 9 && !moving.isEmpty(); ++targetIndex) {
                     Slot target = this.menu.slots.get(targetIndex);
                     if (!this.menu.isTerminalSlot(targetIndex) && target.mayPlace(moving)) {
                        ItemStack existing = playerStacks.get(targetIndex);
                        if ((pass != 0 || !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, moving)) && (pass != 1 || existing.isEmpty())) {
                           int currentCount = existing.isEmpty() ? 0 : existing.getCount();
                           int limit = Math.min(moving.getMaxStackSize(), target.getMaxStackSize(moving));
                           int accepted = Math.min(moving.getCount(), Math.max(0, limit - currentCount));
                           if (accepted > 0) {
                              playerStacks.put(targetIndex, existing.isEmpty() ? moving.copyWithCount(accepted) : existing.copyWithCount(currentCount + accepted));
                              moving.shrink(accepted);
                           }
                        }
                     }
                  }
               }

               if (moving.getCount() != source.getCount()) {
                  this.playerPreviewItems.put(sourceIndex, moving);
               }

               if (moving.isEmpty()) {
                  if (upgradeSlot == 0) {
                     hasLevel3 = false;
                  }

                  if (upgradeSlot == 1) {
                     hasLevel2 = false;
                  }
               }
            }
         }
      }

      for (Int2ObjectMap.Entry<ItemStack> entry : playerStacks.int2ObjectEntrySet()) {
         if (!ItemStack.matches(originalPlayerStacks.get(entry.getIntKey()), entry.getValue())) {
            this.playerPreviewItems.put(entry.getIntKey(), entry.getValue());
         }
      }

   }

   public void applyShiftDragPreview(TerminalShiftDragPreviewPayload payload) {
      if ((this.itemDragShift || this.shiftDragCommitPending) && payload.requestId() == this.latestShiftPreviewRequestId) {
         this.appliedShiftPreviewRequestId = payload.requestId();
         boolean applies = payload.complete() || !this.itemDragShift && !this.shiftDragCommitPending;
         if (applies) {
            this.playerPreviewItems.clear();
            this.remotePreviewItems.clear();
            this.visualPlayerPreviewItems.clear();
            this.visualRemotePreviewItems.clear();
            this.visualRemotePreviewCarried = null;
         }

         if (payload.complete()) {
            for(TerminalShiftDragPreviewPayload.RemoteResult destination : payload.destinations()) {
               long slotKey = this.key(destination.section(), destination.slot());
               if (!destination.connectionId().equals(this.remoteConnectionIds.get(slotKey)) || destination.storageSlot() != this.remoteStorageSlots.getOrDefault(slotKey, -1)) {
                  this.cancelPendingShiftDrag();
                  this.requestView(this.view.scrollOffset());
                  return;
               }
            }

            for(TerminalShiftDragPreviewPayload.SourceResult source : payload.sources()) {
               if (source.menuSlot() >= 0 && source.menuSlot() < this.menu.slots.size()) {
                  this.playerPreviewItems.put(source.menuSlot(), source.remaining());
               }
            }

            for(TerminalShiftDragPreviewPayload.PlayerResult destination : payload.playerDestinations()) {
               if (destination.menuSlot() >= 0 && destination.menuSlot() < this.menu.slots.size()) {
                  this.playerPreviewItems.put(destination.menuSlot(), destination.stack());
               }
            }

            for(TerminalShiftDragPreviewPayload.RemoteResult destination : payload.destinations()) {
               this.remotePreviewItems.put(this.key(destination.section(), destination.slot()), destination.stack());
            }

            this.publishVisualPreview();
         }

         if (this.shiftDragCommitPending) {
            this.sendShiftDragCommit();
         }

      }
   }

   private void finishItemDrag() {
      boolean shiftOperation = this.itemDragShift && (!this.itemDragSlots.isEmpty() || !this.remoteDragTargets.isEmpty());
      if (shiftOperation && this.pendingOperationSequence == 0L) {
         this.pendingOperationSequence = ++this.nextOperationSequence;
      }

      if (shiftOperation && (!this.isUpgradeExtractionShiftDrag() || !this.itemDragMoved) && !this.itemDragSlots.isEmpty() && this.appliedShiftPreviewRequestId != this.latestShiftPreviewRequestId) {
         this.shiftDragCommitPending = true;
         this.itemDragButton = -1;
      } else {
         if (!this.itemDragSlots.isEmpty() && this.itemDragShift) {
            this.sendShiftDragCommit();
         } else if (!this.itemDragSlots.isEmpty() && !this.itemDragCreative && this.itemDragMoved) {
            int type = this.remoteDragMode;
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, -999, AbstractContainerMenu.getQuickcraftMask(0, type), ClickType.QUICK_CRAFT, this.player);

            for(int slotIndex : this.itemDragSlots) {
               this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, AbstractContainerMenu.getQuickcraftMask(1, type), ClickType.QUICK_CRAFT, this.player);
            }

            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, -999, AbstractContainerMenu.getQuickcraftMask(2, type), ClickType.QUICK_CRAFT, this.player);
         } else if (!this.itemDragSlots.isEmpty() && !this.itemDragMoved && this.itemDragStartSlot >= 0) {
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, this.itemDragStartSlot, this.itemDragCreative ? this.itemDragButton : (this.itemDragButton == 1 ? 1 : 0), this.itemDragShift ? ClickType.QUICK_MOVE : (this.itemDragCreative ? ClickType.CLONE : ClickType.PICKUP), this.player);
         }

         boolean hasRemoteTargets = !this.remoteDragTargets.isEmpty();
         if (!this.itemDragCreative || !hasRemoteTargets && this.itemDragSlots.isEmpty()) {
            if (hasRemoteTargets) {
               TerminalStorageClickPayload.SlotTarget first = (TerminalStorageClickPayload.SlotTarget)this.remoteDragTargets.get(0);
               boolean sendBatch = this.itemDragMoved || this.remoteDragShift;
               ClientPlayNetworking.send(sendBatch ? new TerminalStorageClickPayload(first.section(), first.slot(), this.remoteDragMode, this.remoteDragShift, List.copyOf(this.remoteDragTargets), List.of(), this.remoteDragShift ? this.pendingOperationSequence : 0L) : new TerminalStorageClickPayload(first.section(), first.slot(), this.remoteDragMode, false));
            }
         } else if (this.itemDragMoved) {
            TerminalStorageClickPayload.SlotTarget first = hasRemoteTargets ? (TerminalStorageClickPayload.SlotTarget)this.remoteDragTargets.get(0) : new TerminalStorageClickPayload.SlotTarget(-1, -1);
            ClientPlayNetworking.send(new TerminalStorageClickPayload(first.section(), first.slot(), 2, false, List.copyOf(this.remoteDragTargets), List.copyOf(this.itemDragSlots)));
         } else if (hasRemoteTargets) {
            TerminalStorageClickPayload.SlotTarget target = (TerminalStorageClickPayload.SlotTarget)this.remoteDragTargets.get(0);
            ClientPlayNetworking.send(new TerminalStorageClickPayload(target.section(), target.slot(), 2, false));
         }

         this.itemDragButton = -1;
         this.itemDragShift = false;
         this.itemDragMoved = false;
         this.itemDragCreative = false;
         this.itemDragStartSlot = -1;
         this.itemDragSlots.clear();
         this.remoteDragMode = 0;
         this.remoteDragShift = false;
         this.remoteDragTargets.clear();
         this.remoteDragInitialCarried = ItemStack.EMPTY;
         if (!hasRemoteTargets && !shiftOperation) {
            this.clearRemotePreview();
         }

      }
   }

   private void sendShiftDragCommit() {
      ClientPlayNetworking.send(new TerminalStorageClickPayload(-1, -1, 0, true, List.of(), List.copyOf(this.itemDragSlots), this.pendingOperationSequence, this.isUpgradeExtractionShiftDrag() && this.itemDragMoved ? 0L : this.latestShiftPreviewRequestId));
      this.shiftDragCommitPending = false;
      this.itemDragButton = -1;
      this.itemDragShift = false;
      this.itemDragMoved = false;
      this.itemDragCreative = false;
      this.itemDragStartSlot = -1;
      this.itemDragSlots.clear();
      this.remoteDragMode = 0;
      this.remoteDragShift = false;
      this.remoteDragTargets.clear();
      this.remoteDragInitialCarried = ItemStack.EMPTY;
   }

   private void cancelPendingShiftDrag() {
      this.shiftDragCommitPending = false;
      this.pendingOperationSequence = 0L;
      this.itemDragButton = -1;
      this.itemDragShift = false;
      this.itemDragMoved = false;
      this.itemDragCreative = false;
      this.itemDragStartSlot = -1;
      this.itemDragSlots.clear();
      this.remoteDragMode = 0;
      this.remoteDragShift = false;
      this.remoteDragTargets.clear();
      this.remoteDragInitialCarried = ItemStack.EMPTY;
      this.clearRemotePreview();
   }

   private void addRemoteDragTarget(RemoteHit hit) {
      if (!this.itemDragShift || this.itemDragStartSlot < 0) {
         if (hit != null && this.remoteDragTargets.size() < 256) {
            TerminalStorageClickPayload.SlotTarget target = new TerminalStorageClickPayload.SlotTarget(hit.section(), hit.slot());
            if (!this.remoteDragTargets.contains(target)) {
               if (!this.remoteDragTargets.isEmpty() || !this.itemDragSlots.isEmpty()) {
                  this.itemDragMoved = true;
               }

               this.remoteDragTargets.add(target);
               this.updateCombinedPreview();
               this.finishDragWhenCarriedIsEmpty();
            }

         }
      }
   }

   private void finishDragWhenCarriedIsEmpty() {
      if (this.itemDragMoved && !this.itemDragShift && !this.itemDragCreative && this.remotePreviewCarried != null && this.remotePreviewCarried.isEmpty()) {
         if (this.itemDragButton == 1 || this.itemDragButton == 0 && this.eligibleDragSlotCount() >= this.remoteDragInitialCarried.getCount()) {
            this.finishItemDrag();
         }

      }
   }

   private int eligibleDragSlotCount() {
      int eligible = 0;
      ItemStack carried = this.remoteDragInitialCarried;

      for(int index : this.itemDragSlots) {
         Slot slot = this.menu.slots.get(index);
         ItemStack existing = slot.getItem();
         if (this.menu.canDragTo(slot) && slot.mayPlace(carried) && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, carried)) && slot.getMaxStackSize(carried) > (existing.isEmpty() ? 0 : existing.getCount())) {
            ++eligible;
         }
      }

      for(TerminalStorageClickPayload.SlotTarget target : this.remoteDragTargets) {
         long slotKey = this.key(target.section(), target.slot());
         ItemStack existing = this.remoteItems.getOrDefault(slotKey, ItemStack.EMPTY);
         if ((existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, carried)) && this.remoteSlotLimit(slotKey, carried) > (existing.isEmpty() ? 0 : existing.getCount())) {
            ++eligible;
         }
      }

      return eligible;
   }

   private void updateRemotePreview() {
      this.remotePreviewItems.clear();
      ItemStack remaining = this.itemDragSlots.isEmpty() ? this.remoteDragInitialCarried.copy() : (this.remotePreviewCarried == null ? this.remoteDragInitialCarried.copy() : this.remotePreviewCarried.copy());
      if (this.remoteDragShift) {
         this.playerPreviewItems.clear();

         for(int index = this.menu.mainInventoryStart; index < this.menu.hotbarStart + 9; ++index) {
            this.playerPreviewItems.put(index, (this.menu.slots.get(index)).getItem().copy());
         }

         for(TerminalStorageClickPayload.SlotTarget target : this.remoteDragTargets) {
            long slotKey = this.key(target.section(), target.slot());
            ItemStack source = (this.remoteItems.getOrDefault(slotKey, ItemStack.EMPTY)).copy();
            if (!source.isEmpty()) {
               int requested = TerminalInsertionRules.shiftExtractionCount(source.getCount(), source.getMaxStackSize());
               ItemStack moving = source.copyWithCount(requested);
               this.insertPreviewIntoPlayerInventory(moving);
               source.shrink(requested - moving.getCount());
               this.remotePreviewItems.put(slotKey, source);
            }
         }

         this.remotePreviewCarried = ItemStack.EMPTY;
         this.publishVisualPreview();
      } else {
         List<TerminalStorageClickPayload.SlotTarget> eligible = new ArrayList<>();
         Long2ObjectOpenHashMap<ItemStack> working = new Long2ObjectOpenHashMap<>();

         for(TerminalStorageClickPayload.SlotTarget target : this.remoteDragTargets) {
            long slotKey = this.key(target.section(), target.slot());
            ItemStack existing = (this.remoteItems.getOrDefault(slotKey, ItemStack.EMPTY)).copy();
            if ((existing.isEmpty() || !remaining.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) && (remaining.isEmpty() || existing.getCount() < this.remoteSlotLimit(slotKey, remaining))) {
               eligible.add(target);
               working.put(slotKey, existing);
            }
         }

         if (!remaining.isEmpty()) {
            if (this.remoteDragMode == 2 && this.player.getAbilities().instabuild) {
               for(TerminalStorageClickPayload.SlotTarget target : eligible) {
                  long slotKey = this.key(target.section(), target.slot());
                  ItemStack existing = working.get(slotKey);
                  int limit = this.remoteSlotLimit(slotKey, remaining);
                  int count = Math.min(remaining.getCount(), limit);
                  if (count > 0) {
                     if (existing.isEmpty()) {
                        working.put(slotKey, remaining.copyWithCount(count));
                     } else {
                        working.put(slotKey, existing.copyWithCount(Math.max(existing.getCount(), count)));
                     }
                  }
               }
            } else {
               int amount = this.remoteDragMode == 1 ? 1 : (eligible.isEmpty() ? 0 : remaining.getCount() / eligible.size());

               for(TerminalStorageClickPayload.SlotTarget target : eligible) {
                  if (remaining.isEmpty() || amount <= 0) {
                     break;
                  }

                  long slotKey = this.key(target.section(), target.slot());
                  ItemStack existing = working.get(slotKey);
                  int currentCount = existing.isEmpty() ? 0 : existing.getCount();
                  int inserted = Math.min(amount, this.remoteSlotLimit(slotKey, remaining) - currentCount);
                  if (inserted > 0) {
                     working.put(slotKey, existing.isEmpty() ? remaining.copyWithCount(inserted) : existing.copyWithCount(currentCount + inserted));
                     remaining.shrink(inserted);
                  }
               }
            }

            this.remotePreviewItems.putAll(working);
            this.remotePreviewCarried = this.remoteDragMode == 2 ? this.remoteDragInitialCarried.copy() : remaining;
            this.publishVisualPreview();
         }
      }
   }

   private void insertPreviewIntoPlayerInventory(ItemStack moving) {
      for(int pass = 0; pass < 2 && !moving.isEmpty(); ++pass) {
         for(int index = this.menu.mainInventoryStart; index < this.menu.hotbarStart + 9 && !moving.isEmpty(); ++index) {
            Slot slot = this.menu.slots.get(index);
            if (!this.menu.isTerminalSlot(index) && slot.mayPlace(moving)) {
               ItemStack current = this.playerPreviewItems.get(index);
               if ((pass != 0 || !current.isEmpty() && ItemStack.isSameItemSameComponents(current, moving)) && (pass != 1 || current.isEmpty())) {
                  int limit = Math.min(moving.getMaxStackSize(), slot.getMaxStackSize(moving));
                  int available = limit - (current.isEmpty() ? 0 : current.getCount());
                  int count = Math.min(moving.getCount(), available);
                  if (count > 0) {
                     this.playerPreviewItems.put(index, current.isEmpty() ? moving.copyWithCount(count) : current.copyWithCount(current.getCount() + count));
                     moving.shrink(count);
                  }
               }
            }
         }
      }

   }

   private void clearRemotePreview() {
      this.remotePreviewItems.clear();
      this.playerPreviewItems.clear();
      this.remotePreviewCarried = null;
      this.clearVisualPreview();
   }

   private void publishVisualPreview() {
      this.visualPlayerPreviewItems.clear();
      this.visualPlayerPreviewItems.putAll(this.playerPreviewItems);
      this.visualRemotePreviewItems.clear();
      this.visualRemotePreviewItems.putAll(this.remotePreviewItems);
      this.visualRemotePreviewCarried = this.remotePreviewCarried;
   }

   private void clearVisualPreview() {
      this.visualPlayerPreviewItems.clear();
      this.visualRemotePreviewItems.clear();
      this.visualRemotePreviewCarried = null;
   }

   private RemoteHit remoteHitAt(double mouseX, double mouseY) {
      TerminalScreenLayout.Rect area = this.variant.contentArea();
      if (!this.isInside(area, mouseX, mouseY)) {
         return null;
      } else {
         double x = mouseX - (double)this.leftPos;
         double y = mouseY - (double)this.topPos;
         int cursor = area.top() + 2 - this.view.scrollOffset();

         for(ClientLayoutEntry entry : this.layoutEntries()) {
            if (entry.notice() != null) {
               cursor += this.sectionHeight(entry.notice().slotCount());
            } else {
               TerminalViewPayload.Section section = entry.section();
               int slotY = cursor + 12;

               for(int slot = 0; slot < section.slotCount(); ++slot) {
                  int slotX = area.left() + slot % TerminalScreenLayout.MAIN_COLUMNS * TerminalScreenLayout.SLOT_SIZE;
                  int currentY = slotY + slot / TerminalScreenLayout.MAIN_COLUMNS * TerminalScreenLayout.SLOT_SIZE;
                  if (x >= (double)slotX && x < (double)(slotX + TerminalScreenLayout.SLOT_SIZE) && y >= (double)currentY && y < (double)(currentY + TerminalScreenLayout.SLOT_SIZE)) {
                     return new RemoteHit(entry.sectionIndex(), slot, this.visualRemotePreviewItems.getOrDefault(this.key(entry.sectionIndex(), slot), this.remoteItems.getOrDefault(this.key(entry.sectionIndex(), slot), ItemStack.EMPTY)));
                  }
               }

               cursor += this.sectionHeight(section.slotCount());
            }
         }

         return null;
      }
   }

   private boolean clickNotice(double mouseX, double mouseY) {
      int cursor = this.variant.contentArea().top() + 2 - this.view.scrollOffset();

      for(ClientLayoutEntry entry : this.layoutEntries()) {
         if (entry.notice() != null) {
            int bottom = cursor + this.sectionHeight(entry.notice().slotCount()) - 4;
            int crossX = this.variant.contentArea().right() - 9;
            int crossY = cursor + 2;
            if (mouseX >= (double)(this.leftPos + crossX) && mouseX < (double)(this.leftPos + crossX + 7) && mouseY >= (double)(this.topPos + crossY) && mouseY < (double)(this.topPos + crossY + 7)) {
               TerminalViewPayload.Notice notice = entry.notice();
               ClientPlayNetworking.send(new TerminalNoticeDismissPayload(notice.dimension(), notice.pos()));
               return true;
            }

            cursor = bottom + 4;
         } else {
            cursor += this.sectionHeight(entry.section().slotCount());
         }
      }

      return false;
   }

   private Slot staticSlotAt(double mouseX, double mouseY) {
      double x = mouseX - (double)this.leftPos;
      double y = mouseY - (double)this.topPos;
      if (this.pointHit(this.variant.result(), x, y)) {
         return this.menu.slots.get(this.menu.craftingResultIndex);
      } else {
         for(int row = 0; row < 3; ++row) {
            for(int column = 0; column < 3; ++column) {
               TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(this.variant.craftOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.craftOrigin().y() + row * TerminalScreenLayout.SLOT_SIZE);
               if (this.pointHit(point, x, y)) {
                  return this.menu.slots.get(this.menu.craftingGridStart + row * 3 + column);
               }
            }
         }

         for(int i = 0; i < 4; ++i) {
            if (this.pointHit(this.variant.armor()[i], x, y)) {
               return this.menu.slots.get(this.menu.armorStart + i);
            }
         }

         if (this.pointHit(this.variant.offhand(), x, y)) {
            return this.menu.slots.get(this.menu.offhandIndex);
         } else if (this.pointHit(this.variant.upgrade3(), x, y)) {
            return this.menu.slots.get(this.menu.upgradeStart);
         } else if (this.pointHit(this.variant.upgrade2(), x, y)) {
            return this.menu.slots.get(this.menu.upgradeStart + 1);
         } else if (this.pointHit(this.variant.upgrade1(), x, y)) {
            return this.menu.slots.get(this.menu.upgradeStart + 2);
         } else {
            for(int row = 0; row < 3; ++row) {
               for(int column = 0; column < TerminalScreenLayout.MAIN_COLUMNS; ++column) {
                  TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(this.variant.inventoryOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.inventoryOrigin().y() + row * TerminalScreenLayout.SLOT_SIZE);
                  if (this.pointHit(point, x, y)) {
                     return this.menu.slots.get(this.menu.mainInventoryStart + row * TerminalScreenLayout.MAIN_COLUMNS + column);
                  }
               }
            }

            for(int column = 0; column < TerminalScreenLayout.MAIN_COLUMNS; ++column) {
               TerminalScreenLayout.Point point = new TerminalScreenLayout.Point(this.variant.hotbarOrigin().x() + column * TerminalScreenLayout.SLOT_SIZE, this.variant.hotbarOrigin().y());
               if (this.pointHit(point, x, y)) {
                  return this.menu.slots.get(this.menu.hotbarStart + column);
               }
            }

            return null;
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (!this.filterEditSections.isEmpty() && button == 0 && this.menu.getCarried().isEmpty()) {
         RemoteHit hit = this.remoteHitAt(mouseX, mouseY);
         if (hit != null && this.isFilterEditing(hit.section())) {
            this.clearFilterSlot(hit.section(), hit.slot());
            return true;
         }
      }

      if (this.itemDragButton >= 0 && button == this.itemDragButton) {
         if (!this.itemDragMoved && !this.itemDragShift && !this.itemDragCreative && !this.menu.getCarried().isEmpty()) {
            if (this.itemDragStartSlot < 0) {
               if (!this.remoteDragTargets.isEmpty()) {
                  TerminalStorageClickPayload.SlotTarget startTarget = (TerminalStorageClickPayload.SlotTarget)this.remoteDragTargets.get(0);
                  RemoteHit startHit = new RemoteHit(startTarget.section(), startTarget.slot(), ItemStack.EMPTY);
                  if (!this.isManualDragStartAllowed(this.menu.getCarried(), this.isRemoteDragStartInOrigin(startHit), false)) {
                     this.clearItemDragState();
                     return true;
                  }
               }
            } else if (!this.isManualDragStartAllowed(this.menu.getCarried(), false, this.isPlayerInventoryDragStartSlot(this.itemDragStartSlot) || this.isCraftingDragStartSlot(this.itemDragStartSlot))) {
               this.clearItemDragState();
               return true;
            }
         }

         this.addStaticDragTarget(this.staticSlotAt(mouseX, mouseY));
         if (this.itemDragButton < 0) {
            return true;
         } else {
            this.addRemoteDragTarget(this.remoteHitAt(mouseX, mouseY));
            return true;
         }
      } else if (this.draggingScroller) {
         this.updateScroll(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.itemDragButton >= 0 && button == this.itemDragButton) {
         this.addStaticDragTarget(this.staticSlotAt(mouseX, mouseY));
         if (this.itemDragButton < 0) {
            return true;
         } else {
            this.addRemoteDragTarget(this.remoteHitAt(mouseX, mouseY));
            if (this.itemDragButton < 0) {
               return true;
            } else {
               this.finishItemDrag();
               return true;
            }
         }
      } else if (this.draggingScroller) {
         this.draggingScroller = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   private void updateScroll(double mouseY) {
      if (this.editingSection < 0) {
         int travel = this.variant.scrollTrack().height() - 15;
         if (travel > 0 && this.view.maxScroll() > 0) {
            int next = (int)((mouseY - (double)this.topPos - (double)this.variant.scrollTrack().top() - (double)7.5F) / (double)travel * (double)this.view.maxScroll());
            next = Math.max(0, Math.min(this.view.maxScroll(), next));
            this.requestView(next);
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.editingSection >= 0) {
         return true;
      } else if (!this.isInside(this.variant.contentArea(), mouseX, mouseY) && !this.isInside(this.variant.scrollTrack(), mouseX, mouseY)) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      } else {
         int speed = SimplestoragemanagerConfig.syncedScrollSpeed();
         this.requestView(this.view.scrollOffset() - (int)Math.signum(scrollY) * speed);
         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.searchBar != null && this.searchBar.isFocused() && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
         return true;
      } else if (this.editingSection >= 0 && this.titleEditBox != null && this.titleEditBox.isFocused()) {
         if (keyCode == 257) {
            this.confirmTitleEdit();
            return true;
         } else if (keyCode == 256) {
            this.cancelTitleEdit();
            return true;
         } else {
            return this.titleEditBox.keyPressed(keyCode, scanCode, modifiers);
         }
      } else if (this.sectionMenuOpen && keyCode == 256) {
         this.closeSectionMenu();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean charTyped(char codePoint, int modifiers) {
      if (this.editingSection >= 0 && this.titleEditBox != null && this.titleEditBox.isFocused()) {
         String before = this.titleEditBox.getValue();
         boolean result = this.titleEditBox.charTyped(codePoint, modifiers);
         String after = this.titleEditBox.getValue();
         return result;
      } else if (this.searchBar != null && this.searchBar.isFocused()) {
         String before = this.searchBar.getValue();
         boolean result = super.charTyped(codePoint, modifiers);
         String after = this.searchBar.getValue();
         return result;
      } else {
         return super.charTyped(codePoint, modifiers);
      }
   }

   public void resize(Minecraft minecraft, int width, int height) {
      String value = this.searchBar == null ? "" : this.searchBar.getValue();
      super.resize(minecraft, width, height);
      if (this.editingSection >= 0) {
         this.cancelTitleEdit();
      }

      if (this.searchBar != null) {
         this.searchBar.setValue(value);
      }

   }

   public void updateMenuState(int elementType, String name, Object state) {
   }

   private boolean isInside(TerminalScreenLayout.Rect rect, double mouseX, double mouseY) {
      return mouseX >= (double)(this.leftPos + rect.left()) && mouseX < (double)(this.leftPos + rect.right() + 1) && mouseY >= (double)(this.topPos + rect.top()) && mouseY < (double)(this.topPos + rect.bottom() + 1);
   }

   public int getGuiLeft() {
      return this.leftPos;
   }

   public int getGuiTop() {
      return this.topPos;
   }

   public Slot getSlotAtRelative(double x, double y) {
      TerminalScreenLayout.Point craftOrigin = this.variant.craftOrigin();
      if (x >= (double)craftOrigin.x() && x < (double)(craftOrigin.x() + 54) && y >= (double)craftOrigin.y() && y < (double)(craftOrigin.y() + 54)) {
         int column = (int)((x - (double)craftOrigin.x()) / (double)TerminalScreenLayout.SLOT_SIZE);
         int row = (int)((y - (double)craftOrigin.y()) / (double)TerminalScreenLayout.SLOT_SIZE);
         if (column >= 0 && column < 3 && row >= 0 && row < 3) {
            return this.menu.slots.get(this.menu.craftingGridStart + row * 3 + column);
         }
      }

      TerminalScreenLayout.Point invOrigin = this.variant.inventoryOrigin();
      if (x >= (double)invOrigin.x() && x < (double)(invOrigin.x() + 162) && y >= (double)invOrigin.y() && y < (double)(invOrigin.y() + 54)) {
         int column = (int)((x - (double)invOrigin.x()) / (double)TerminalScreenLayout.SLOT_SIZE);
         int row = (int)((y - (double)invOrigin.y()) / (double)TerminalScreenLayout.SLOT_SIZE);
         if (column >= 0 && column < 9 && row >= 0 && row < 3) {
            return this.menu.slots.get(this.menu.mainInventoryStart + row * TerminalScreenLayout.MAIN_COLUMNS + column);
         }
      }

      TerminalScreenLayout.Point hotbarOrigin = this.variant.hotbarOrigin();
      if (x >= (double)hotbarOrigin.x() && x < (double)(hotbarOrigin.x() + TerminalScreenLayout.MAIN_COLUMNS * TerminalScreenLayout.SLOT_SIZE) && y >= (double)hotbarOrigin.y() && y < (double)(hotbarOrigin.y() + TerminalScreenLayout.SLOT_SIZE)) {
         int column = (int)((x - (double)hotbarOrigin.x()) / (double)TerminalScreenLayout.SLOT_SIZE);
         if (column >= 0 && column < 9) {
            return this.menu.slots.get(this.menu.hotbarStart + column);
         }
      }

      return null;
   }

   /** Item sob o mouse em coordenadas absolutas da tela, incluindo os baús remotos. */
   public ItemStack getHoveredItem(double mouseX, double mouseY) {
      RemoteHit remote = this.remoteHitAt(mouseX, mouseY);
      if (remote != null && !remote.stack().isEmpty()) {
         return remote.stack();
      }

      Slot slot = this.getSlotAtRelative(mouseX - (double)this.leftPos, mouseY - (double)this.topPos);
      if (slot != null && !slot.getItem().isEmpty()) {
         return slot.getItem();
      }

      if (this.pointHit(this.variant.result(), mouseX - (double)this.leftPos, mouseY - (double)this.topPos)) {
         ItemStack result = this.menu.slots.get(this.menu.craftingResultIndex).getItem();
         if (!result.isEmpty()) {
            return result;
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean pointHit(TerminalScreenLayout.Point point, double x, double y) {
      return x >= (double)point.x() && x < (double)(point.x() + TerminalScreenLayout.SLOT_SIZE) && y >= (double)point.y() && y < (double)(point.y() + TerminalScreenLayout.SLOT_SIZE);
   }

   private boolean isPointInside(TerminalScreenLayout.Point point, int size, double mouseX, double mouseY) {
      return mouseX >= (double)(this.leftPos + point.x()) && mouseX < (double)(this.leftPos + point.x() + size) && mouseY >= (double)(this.topPos + point.y()) && mouseY < (double)(this.topPos + point.y() + size);
   }

   private long key(int section, int slot) {
      return (long)section << 32 | (long)slot & 4294967295L;
   }

   private List<ClientLayoutEntry> layoutEntries() {
      if (this.cachedLayoutEntries != null && this.cachedLayoutRevision == this.layoutRevision) {
         return this.cachedLayoutEntries;
      } else {
         List<TerminalViewPayload.Notice> notices = new ArrayList<>(this.view.notices());
         notices.sort(Comparator.comparingInt(TerminalViewPayload.Notice::sectionIndex));
         List<ClientLayoutEntry> entries = new ArrayList<>();
         int noticeIndex = 0;

         for(int sectionIndex = 0; sectionIndex < this.view.sections().size(); ++sectionIndex) {
            while(noticeIndex < notices.size() && (notices.get(noticeIndex)).sectionIndex() <= sectionIndex) {
               entries.add(StorageManagerGUIScreen.ClientLayoutEntry.notice(notices.get(noticeIndex++)));
            }

            entries.add(StorageManagerGUIScreen.ClientLayoutEntry.section(sectionIndex, this.view.sections().get(sectionIndex)));
         }

         while(noticeIndex < notices.size()) {
            entries.add(StorageManagerGUIScreen.ClientLayoutEntry.notice(notices.get(noticeIndex++)));
         }

         this.cachedLayoutEntries = entries;
         this.cachedLayoutRevision = this.layoutRevision;
         return entries;
      }
   }

   private int sectionHeight(int slotCount) {
      int rows = Math.max(1, (slotCount + 8) / 9);
      return 12 + rows * TerminalScreenLayout.SLOT_SIZE + 2 + 4;
   }

   private static record SectionButtonHit(int section, int kind) {
   }

   private static final class TitleScrollAnimation {
      private long startedAtTick = -1L;
   }

   private static record RemoteHit(int section, int slot, ItemStack stack) {
   }

   private static record ClientLayoutEntry(int sectionIndex, TerminalViewPayload.Section section, TerminalViewPayload.Notice notice) {
      private static ClientLayoutEntry section(int index, TerminalViewPayload.Section section) {
         return new ClientLayoutEntry(index, section, null);
      }

      private static ClientLayoutEntry notice(TerminalViewPayload.Notice notice) {
         return new ClientLayoutEntry(-1, null, notice);
      }
   }
}
