package com.yueyingiop.payday.client.screen;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.helper.RenderHelper;
import com.yueyingiop.payday.client.render.EquipmentBoxHudRenderer;
import com.yueyingiop.payday.client.render.ExposureRiskHudRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** 通过拖动预览框配置装备箱 HUD 位置的界面。 */
public final class HudConfigScreen extends Screen {
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private HudSelection selectedHud;
    private Button doneButton;

    public HudConfigScreen() {
        super(Component.translatable("screen.payday.hud_config.title"));
    }

    /**
     * 初始化界面或插件。
     */
    @Override
    protected void init() {
        doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        ExposureRiskHudRenderer.ensureDefaultPosition(width, height);
    }

    /** 保存 HUD 位置并关闭界面。 */
    private void saveAndClose() {
        Config.CLIENT_SPEC.save();
        minecraft.setScreen(null);
    }

    /**
     * 保存 HUD 位置并关闭配置界面。
     */
    @Override
    public void onClose() {
        saveAndClose();
    }

    /**
     * 判断界面是否暂停游戏。
     * @return 对应的执行结果。
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 根据配置比例计算装备箱 HUD 横坐标。
     * @return 对应的执行结果。
     */
    private int equipmentBoxHudX() {
        return RenderHelper.xFromRatio(
            Config.EQUIPMENT_BOX_HUD_X_RATIO.get(), 
            width,
            EquipmentBoxHudRenderer.HUD_WIDTH
        );
    }

    /**
     * 根据配置比例计算装备箱 HUD 纵坐标。
     * @return 对应的执行结果。
     */
    private int equipmentBoxHudY() {
        return RenderHelper.yFromRatio(Config.EQUIPMENT_BOX_HUD_Y_RATIO.get(), height,
                EquipmentBoxHudRenderer.HUD_HEIGHT);
    }

    /**
     * 根据配置比例计算暴露风险 HUD 横坐标。
     * @return 对应的执行结果。
     */
    private int exposureHudX() {
        return RenderHelper.xFromRatio(Config.EXPOSURE_RISK_HUD_X_RATIO.get(), width,
                ExposureRiskHudRenderer.HUD_WIDTH);
    }

    /**
     * 根据配置比例计算暴露风险 HUD 纵坐标。
     * @return 对应的执行结果。
     */
    private int exposureHudY() {
        return RenderHelper.yFromRatio(Config.EXPOSURE_RISK_HUD_Y_RATIO.get(), height,
                ExposureRiskHudRenderer.HUD_HEIGHT);
    }

    /**
     * 处理鼠标点击输入。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param button 鼠标按键。
     * @return 对应的执行结果。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= exposureHudX()
                && mouseX < exposureHudX() + ExposureRiskHudRenderer.HUD_WIDTH
                && mouseY >= exposureHudY() && mouseY < exposureHudY() + ExposureRiskHudRenderer.HUD_HEIGHT) {
            selectedHud = HudSelection.EXPOSURE_RISK;
            dragging = true;
            dragOffsetX = (int) mouseX - exposureHudX();
            dragOffsetY = (int) mouseY - exposureHudY();
            return true;
        }
        if (button == 0 && mouseX >= equipmentBoxHudX() && mouseX < equipmentBoxHudX() + EquipmentBoxHudRenderer.HUD_WIDTH
                && mouseY >= equipmentBoxHudY() && mouseY < equipmentBoxHudY() + EquipmentBoxHudRenderer.HUD_HEIGHT) {
            selectedHud = HudSelection.EQUIPMENT_BOX;
            dragging = true;
            dragOffsetX = (int) mouseX - equipmentBoxHudX();
            dragOffsetY = (int) mouseY - equipmentBoxHudY();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 根据鼠标拖动更新界面元素位置。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param button 鼠标按键。
     * @param dragX 横向拖动距离。
     * @param dragY 纵向拖动距离。
     * @return 对应的执行结果。
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            int top = (int) mouseY - dragOffsetY;
            int left = (int) mouseX - dragOffsetX;
            if (selectedHud == HudSelection.EXPOSURE_RISK) {
                Config.EXPOSURE_RISK_HUD_X_RATIO.set(RenderHelper.xRatioFromPosition(
                        left, width, ExposureRiskHudRenderer.HUD_WIDTH));
                Config.EXPOSURE_RISK_HUD_Y_RATIO.set(RenderHelper.yRatioFromPosition(
                        top, height, ExposureRiskHudRenderer.HUD_HEIGHT));
            } else {
                Config.EQUIPMENT_BOX_HUD_X_RATIO.set(
                        RenderHelper.xRatioFromPosition(left, width, EquipmentBoxHudRenderer.HUD_WIDTH));
                Config.EQUIPMENT_BOX_HUD_Y_RATIO.set(
                        RenderHelper.yRatioFromPosition(top, height, EquipmentBoxHudRenderer.HUD_HEIGHT));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 处理鼠标释放输入。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param button 鼠标按键。
     * @return 对应的执行结果。
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            selectedHud = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 绘制对应内容。
     * @param graphics 图形渲染上下文。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param partialTick 帧间插值。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.payday.hud_config.drag"),
                width / 2, 30, 0xD0D0D0);

        int x = equipmentBoxHudX();
        int y = equipmentBoxHudY();
        Player player = minecraft.player;
        EquipmentBoxHudRenderer.Contents contents = player == null
                ? new EquipmentBoxHudRenderer.Contents(net.minecraft.world.item.ItemStack.EMPTY, 0)
                : EquipmentBoxHudRenderer.findContents(player);
        EquipmentBoxHudRenderer.renderAt(graphics, contents, x, y, true);
        ExposureRiskHudRenderer.renderPreview(graphics, player, exposureHudX(), exposureHudY());

        doneButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private enum HudSelection {
        EQUIPMENT_BOX,
        EXPOSURE_RISK
    }
}
