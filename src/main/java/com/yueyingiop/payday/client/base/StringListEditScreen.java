package com.yueyingiop.payday.client.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用字符串列表编辑界面。
 *
 * <p>每一行对应一个字符串，支持添加、删除、滚动、保存和取消。保存时会自动去除首尾空白并忽略空行。</p>
 */
public final class StringListEditScreen extends Screen {
    private static final int ROW_TOP = 56;
    private static final int ROW_HEIGHT = 28;
    private static final int BOTTOM_MARGIN = 36;

    private final Screen parent;
    private final Component description;
    private final Consumer<List<String>> saveHandler;
    private final List<String> values;
    private final List<EditBox> boxes = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();
    private int scroll;

    public StringListEditScreen(Screen parent, Component title, Component description,
                                List<? extends String> initialValues,
                                Consumer<List<String>> saveHandler) {
        super(title);
        this.parent = parent;
        this.description = description;
        this.saveHandler = saveHandler;
        this.values = new ArrayList<>(initialValues);
        if (this.values.isEmpty()) {
            this.values.add("");
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("添加输入框"), ignored -> addEntry())
                .bounds(width / 2 - 60, 30, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> saveAndReturn())
                .bounds(width / 2 - 105, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> returnToParent())
                .bounds(width / 2 + 5, height - 28, 100, 20).build());
        rebuildRows();
    }

    private void addEntry() {
        captureValues();
        values.add("");
        scroll = maxScroll();
        rebuildRows();
    }

    private void removeEntry(int index) {
        captureValues();
        if (values.size() > 1) {
            values.remove(index);
        } else {
            values.set(0, "");
        }
        scroll = Math.min(scroll, maxScroll());
        rebuildRows();
    }

    private void captureValues() {
        for (int index = 0; index < Math.min(values.size(), boxes.size()); index++) {
            values.set(index, boxes.get(index).getValue());
        }
    }

    private void rebuildRows() {
        boxes.forEach(this::removeWidget);
        deleteButtons.forEach(this::removeWidget);
        boxes.clear();
        deleteButtons.clear();

        int fieldWidth = Math.min(220, Math.max(120, width - 112));
        int x = Math.max(8, (width - fieldWidth - 58) / 2);
        for (int index = 0; index < values.size(); index++) {
            int entryIndex = index;
            EditBox box = new EditBox(font, x, 0, fieldWidth, 20, Component.literal("列表项"));
            box.setMaxLength(256);
            box.setValue(values.get(index));
            boxes.add(addRenderableWidget(box));

            Button delete = Button.builder(Component.literal("删除"), ignored -> removeEntry(entryIndex))
                    .bounds(x + fieldWidth + 6, 0, 52, 20).build();
            deleteButtons.add(addRenderableWidget(delete));
        }
        repositionRows();
    }

    private int maxScroll() {
        int contentHeight = ROW_TOP + values.size() * ROW_HEIGHT;
        return Math.max(0, contentHeight - (height - BOTTOM_MARGIN));
    }

    private void repositionRows() {
        int bottom = height - BOTTOM_MARGIN;
        for (int index = 0; index < boxes.size(); index++) {
            int y = ROW_TOP + index * ROW_HEIGHT - scroll;
            EditBox box = boxes.get(index);
            Button delete = deleteButtons.get(index);
            box.setY(y);
            delete.setY(y);
            boolean visible = y + ROW_HEIGHT >= ROW_TOP && y < bottom;
            box.visible = visible;
            delete.visible = visible;
        }
    }

    private void saveAndReturn() {
        captureValues();
        saveHandler.accept(values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList());
        returnToParent();
    }

    private void returnToParent() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= ROW_TOP && mouseY < height - BOTTOM_MARGIN) {
            scroll = Mth.clamp(scroll - (int) Math.round(delta * 18.0D), 0, maxScroll());
            repositionRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, description, width / 2, 20, 0xD0D0D0);
        graphics.enableScissor(0, ROW_TOP, width, height - BOTTOM_MARGIN);
        for (int index = 0; index < boxes.size(); index++) {
            EditBox box = boxes.get(index);
            if (box.visible) {
                graphics.drawString(font, Component.literal("条目 " + (index + 1)),
                        box.getX(), box.getY() - 11, 0xFFFFFF, false);
            }
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        returnToParent();
    }
}
