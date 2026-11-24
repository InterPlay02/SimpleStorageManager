package net.interplay.simplestoragemanager.client.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.logic.UpgradeAccessRules;
import net.interplay.simplestoragemanager.network.DimensionalUpgradeSavePayload;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lets players select the dimensions where the level-two upgrade can access storage. */
public final class DimensionalUpgradeSelectionScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 256;
    private static final int LIST_LEFT = 9;
    private static final int LIST_TOP = 18;
    private static final int LIST_RIGHT = 246;
    private static final int LIST_BOTTOM = 202;
    private static final int ROW_HEIGHT = 20;
    private static final int CHECKBOX_X = 14;
    private static final int CHECKBOX_SIZE = 10;
    private static final int CHECKBOX_TEXTURE_SIZE = 20;
    private static final int TEXT_X = 27;
    private static final int SCROLLER_X = LIST_RIGHT - 12;
    private static final int SCROLLER_SIZE = 15;
    private static final int BUTTON_Y = 209;
    private static final ResourceLocation BACKGROUND = texture("dimensional_upgrade_gui.png");
    private static final ResourceLocation ROW = texture("bg.png");
    private static final ResourceLocation ROW_HIGHLIGHTED = texture("bg_highlighted.png");
    private static final ResourceLocation CHECKBOX = texture("checkbox.png");
    private static final ResourceLocation CHECKBOX_HIGHLIGHTED = texture("checkbox_highlighted.png");
    private static final ResourceLocation CHECKBOX_SELECTED = texture("checkbox_selected.png");
    private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED = texture("checkbox_selected_highlighted.png");
    private static final ResourceLocation SCROLLER = texture("scroller.png");
    private static final ResourceLocation SCROLLER_DISABLED = texture("scroller_disabled.png");

    private final List<String> dimensions;
    private final Set<String> selected;
    private final boolean offhand;
    private int leftPos;
    private int topPos;
    private int scrollOffset;
    private boolean draggingScroller;
    private boolean savePending;
    private Component message;
    private Button saveButton;
    private int cancelWidth;
    private int saveWidth;

    public DimensionalUpgradeSelectionScreen(List<String> dimensions, List<String> selected, boolean offhand) {
        super(Component.translatable("screen.simplestoragemanager.dimension_selector.title"));
        this.dimensions = dimensions.stream().distinct().sorted().toList();
        this.selected = new LinkedHashSet<>();
        for (String dimension : selected) {
            if (this.dimensions.contains(dimension)
                    && this.selected.size() < UpgradeAccessRules.MAX_SELECTED_DIMENSIONS) {
                this.selected.add(dimension);
            }
        }
        this.offhand = offhand;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "textures/screens/" + name);
    }

    @Override
    protected void init() {
        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;
        cancelWidth = font.width(Component.translatable("screen.simplestoragemanager.dimension_selector.cancel")) + 16;
        Component saveText = Component.translatable("screen.simplestoragemanager.dimension_selector.save");
        saveWidth = font.width(saveText) + 16;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.simplestoragemanager.dimension_selector.cancel"),
                        button -> onClose())
                .bounds(leftPos + 8, topPos + BUTTON_Y, cancelWidth, 20)
                .build());
        saveButton = addRenderableWidget(Button.builder(saveText, button -> save())
                .bounds(leftPos + 247 - saveWidth, topPos + BUTTON_Y, saveWidth, 20)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21.8: a cor do shader faz parte do estado do pipeline; o branco padrão mantém o resultado anterior.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
                0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        int visibleRows = visibleRows();
        int end = Math.min(dimensions.size(), scrollOffset + visibleRows);
        graphics.enableScissor(leftPos + LIST_LEFT, topPos + LIST_TOP,
                leftPos + LIST_RIGHT, topPos + LIST_BOTTOM);
        for (int index = scrollOffset; index < end; index++) {
            int rowY = LIST_TOP + (index - scrollOffset) * ROW_HEIGHT;
            int screenX = leftPos + LIST_LEFT;
            int screenY = topPos + rowY;
            boolean hovered = mouseX >= screenX && mouseX < leftPos + LIST_RIGHT
                    && mouseY >= screenY && mouseY < screenY + ROW_HEIGHT;
            graphics.blit(RenderPipelines.GUI_TEXTURED, hovered ? ROW_HIGHLIGHTED : ROW, screenX, screenY,
                    0, 0, 225, ROW_HEIGHT, 225, ROW_HEIGHT);

            boolean checked = selected.contains(dimensions.get(index));
            boolean checkboxHovered = mouseX >= leftPos + CHECKBOX_X
                    && mouseX < leftPos + CHECKBOX_X + CHECKBOX_SIZE
                    && mouseY >= screenY + 5 && mouseY < screenY + 5 + CHECKBOX_SIZE;
            ResourceLocation checkbox = checked
                    ? (checkboxHovered ? CHECKBOX_SELECTED_HIGHLIGHTED : CHECKBOX_SELECTED)
                    : (checkboxHovered ? CHECKBOX_HIGHLIGHTED : CHECKBOX);
            // A textura inteira (20x20) é redimensionada para 10x10: u/v começam em 0 e
            // a região de origem cobre a textura toda, sem recorte.
            graphics.blit(RenderPipelines.GUI_TEXTURED, checkbox, leftPos + CHECKBOX_X, screenY + 5,
                    0, 0, CHECKBOX_SIZE, CHECKBOX_SIZE, CHECKBOX_TEXTURE_SIZE, CHECKBOX_TEXTURE_SIZE,
                    CHECKBOX_TEXTURE_SIZE, CHECKBOX_TEXTURE_SIZE);
            graphics.drawString(font, dimensionName(dimensions.get(index)),
                    leftPos + TEXT_X, screenY + (ROW_HEIGHT - font.lineHeight) / 2, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();

        Component selectionHint = Component.translatable("screen.simplestoragemanager.dimension_selector.selection_hint");
        int cancelRight = 8 + cancelWidth;
        int saveLeft = 247 - saveWidth;
        int centerX = (cancelRight + saveLeft) / 2;
        int availableWidth = Math.max(1, saveLeft - cancelRight - 8);
        int textWidth = availableWidth;
        for (int candidate = 1; candidate <= availableWidth; candidate++) {
            if (font.split(selectionHint, candidate).size() <= 2) {
                textWidth = candidate;
                break;
            }
        }
        List<net.minecraft.util.FormattedCharSequence> hintLines = font.split(selectionHint, textWidth);
        int hintY = BUTTON_Y + Math.max(0, (20 - hintLines.size() * font.lineHeight) / 2);
        for (int i = 0; i < hintLines.size(); i++) {
            net.minecraft.util.FormattedCharSequence line = hintLines.get(i);
            graphics.drawString(font, line, leftPos + centerX - font.width(line) / 2,
                    topPos + hintY + i * font.lineHeight, 0xFF404040, false);
        }

        int maxScroll = maxScroll();
        ResourceLocation scroller = maxScroll > 0 ? SCROLLER : SCROLLER_DISABLED;
        int travel = LIST_BOTTOM - LIST_TOP - SCROLLER_SIZE;
        int thumbY = LIST_TOP + (maxScroll == 0 ? 0 : (int) ((double) scrollOffset / maxScroll * travel));
        graphics.blit(RenderPipelines.GUI_TEXTURED, scroller, leftPos + SCROLLER_X, topPos + thumbY,
                0, 0, 12, SCROLLER_SIZE, 12, SCROLLER_SIZE);

        if (message != null) {
            graphics.drawCenteredString(font, message, leftPos + GUI_WIDTH / 2,
                    topPos + 200 - font.lineHeight, 0xFFFF5555);
        }
    }

    private Component dimensionName(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) return Component.literal(key);
        String translationKey = "dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        Component translated = Component.translatable(translationKey);
        if (!translated.getString().equals(translationKey)) return translated;
        String path = id.getPath().replace('/', ' ').replace('_', ' ');
        String name = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        return Component.literal(id.getNamespace().equals("minecraft") ? name : id.getNamespace() + ": " + name);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (maxScroll() > 0 && localX >= SCROLLER_X && localX < LIST_RIGHT
                && localY >= LIST_TOP && localY < LIST_BOTTOM) {
            draggingScroller = true;
            updateScroll(localY);
            return true;
        }
        if (localX >= LIST_LEFT && localX < LIST_RIGHT && localY >= LIST_TOP
                && localY < LIST_TOP + visibleRows() * ROW_HEIGHT) {
            int row = (localY - LIST_TOP) / ROW_HEIGHT;
            int index = scrollOffset + row;
            if (index < dimensions.size()
                    && localX >= CHECKBOX_X && localX < CHECKBOX_X + CHECKBOX_SIZE
                    && (localY - LIST_TOP) % ROW_HEIGHT >= 5
                    && (localY - LIST_TOP) % ROW_HEIGHT < 5 + CHECKBOX_SIZE) {
                toggleSelection(dimensions.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleSelection(String dimension) {
        playClickSound();
        if (selected.remove(dimension)) {
            message = null;
            return;
        }
        if (selected.size() >= UpgradeAccessRules.MAX_SELECTED_DIMENSIONS) {
            message = Component.translatable("screen.simplestoragemanager.dimension_selector.limit");
            return;
        }
        selected.add(dimension);
        message = null;
    }

    private void playClickSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void save() {
        if (savePending) return;
        savePending = true;
        saveButton.active = false;
        message = null;
        ClientPlayNetworking.send(new DimensionalUpgradeSavePayload(new ArrayList<>(selected), offhand));
    }

    public void handleSaveResult(boolean success) {
        if (success) {
            onClose();
            return;
        }
        savePending = false;
        if (saveButton != null) saveButton.active = true;
        message = Component.translatable("screen.simplestoragemanager.dimension_selector.save_failed");
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroller && button == 0) {
            updateScroll((int) mouseY - topPos);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroller = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localX >= LIST_LEFT && localX < LIST_RIGHT && localY >= LIST_TOP && localY < LIST_BOTTOM) {
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScroll(int localY) {
        int travel = LIST_BOTTOM - LIST_TOP - SCROLLER_SIZE;
        double progress = Math.max(0, Math.min(1, (localY - LIST_TOP - SCROLLER_SIZE / 2.0) / travel));
        scrollOffset = (int) Math.round(progress * maxScroll());
    }

    private int visibleRows() {
        return (LIST_BOTTOM - LIST_TOP) / ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, dimensions.size() - visibleRows());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
