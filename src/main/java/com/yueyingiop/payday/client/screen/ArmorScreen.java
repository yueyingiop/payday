package com.yueyingiop.payday.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.yueyingiop.payday.client.render.ArmorCurioRenderer;
import com.yueyingiop.payday.menu.ArmorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ArmorScreen extends AbstractContainerScreen<ArmorMenu> {
    /** 护甲界面在未缩放状态下的宽度。 */
    public static final int GUI_WIDTH = 212;
    /** 护甲界面在未缩放状态下的高度。 */
    public static final int GUI_HEIGHT = 177;

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("payday", "textures/gui/armor_solt_gui.png");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath("payday", "textures/gui/solt.png");
    private static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath("payday", "textures/gui/inventory.png");

    private ArmorCurioRenderer previewRenderer;
    private ResourceLocation previewModelId;

    public ArmorScreen(ArmorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    /**
     * 初始化界面或插件。
     */
    @Override
    protected void init() {
        super.init();
        leftPos = 0;
        topPos = 0;
        titleLabelX = 8;
        titleLabelY = -12;
        inventoryLabelX = 25;
        inventoryLabelY = 96;
    }

    /**
     * 绘制界面背景及预览。
     * @param graphics 图形渲染上下文。
     * @param partialTick 帧间插值。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(BG, leftPos, topPos, 0, 0, GUI_WIDTH, 89, 256, 256);

        for (int i = 0; i < menu.upgradeSize(); i++) {
            graphics.blit(SLOT, leftPos + menu.slotX(i), topPos + menu.slotY(i), 0, 0, 16, 16, 16, 16);
        }

        graphics.blit(INVENTORY, leftPos + 17, topPos + 92, 0, 0, 176, 88, 176, 88);

        if (!menu.armorDisplay().isEmpty()) {
            graphics.renderItem(menu.armorDisplay(), leftPos + 12, topPos + 5);
        }

        ResourceLocation modelId = menu.armorModelId();
        if (modelId != null) {
            graphics.pose().pushPose();
            ArmorCurioRenderer renderer = previewRenderer(modelId);
            if (renderer.hasRenderResources()) {
                renderer.preparePreview(menu.armorDisplay());
                graphics.pose().translate(leftPos + 67.0F, topPos + 35.0F, 100.0F);
                float mouseYaw = Mth.clamp((mouseX - 67.0F) * 0.35F, -35.0F, 35.0F);
                float mousePitch = Mth.clamp((mouseY - 35.0F) * 0.20F, -20.0F, 20.0F);
                graphics.pose().mulPose(Axis.YP.rotationDegrees(180.0F + mouseYaw));
                graphics.pose().mulPose(Axis.XP.rotationDegrees(mousePitch));
                graphics.pose().scale(25.0F, 25.0F, 25.0F);
                MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                try {
                    renderer.renderModel(graphics.pose(), buffers, 15728880);
                    buffers.endBatch();
                } catch (RuntimeException ignored) {
                    // 预览资源缺失或资源包重载失败时，跳过模型预览而不关闭整个界面。
                }
            }
            graphics.pose().popPose();
        }
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
        float scale = getGuiScale();
        float offsetX = getGuiLeft(scale);
        float offsetY = getGuiTop(scale);
        int logicalMouseX = (int) ((mouseX - offsetX) / scale);
        int logicalMouseY = (int) ((mouseY - offsetY) / scale);
        graphics.pose().pushPose();
        graphics.pose().translate(offsetX, offsetY, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        super.render(graphics, logicalMouseX, logicalMouseY, partialTick);
        renderTooltip(graphics, logicalMouseX, logicalMouseY);
        graphics.pose().popPose();
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
        float scale = getGuiScale();
        double offsetX = getGuiLeft(scale);
        double offsetY = getGuiTop(scale);
        return super.mouseClicked((mouseX - offsetX) / scale, (mouseY - offsetY) / scale, button);
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
        float scale = getGuiScale();
        double offsetX = getGuiLeft(scale);
        double offsetY = getGuiTop(scale);
        return super.mouseReleased((mouseX - offsetX) / scale, (mouseY - offsetY) / scale, button);
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
        float scale = getGuiScale();
        double offsetX = getGuiLeft(scale);
        double offsetY = getGuiTop(scale);
        return super.mouseDragged((mouseX - offsetX) / scale, (mouseY - offsetY) / scale,
                button, dragX / scale, dragY / scale);
    }

    /**
     * 根据模型标识获取护甲预览渲染器。
     * @param modelId 模型标识。
     * @return 对应的执行结果。
     */
    private ArmorCurioRenderer previewRenderer(ResourceLocation modelId) {
        if (previewRenderer == null || !modelId.equals(previewModelId)) {
            previewRenderer = new ArmorCurioRenderer(modelId);
            previewModelId = modelId;
        }
        return previewRenderer;
    }

    /**
     * 绘制界面标签。
     * @param graphics 图形渲染上下文。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
        // graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFFFF, false); //物品栏
    }

    /**
     * 获取当前窗口下护甲界面的实际缩放比例。
     *
     * @return 绘制护甲界面使用的缩放比例
     */
    public float getGuiScale() {
        return Math.min(1.0F, Math.min((float) width / GUI_WIDTH, (float) height / GUI_HEIGHT));
    }

    /**
     * 获取当前窗口下护甲界面的实际左边界。
     *
     * @param scale 当前界面的缩放比例
     * @return 以屏幕像素为单位的左边界
     */
    public float getGuiLeft(float scale) {
        return (width - GUI_WIDTH * scale) * 0.5F;
    }

    /**
     * 获取当前窗口下护甲界面的实际上边界。
     *
     * @param scale 当前界面的缩放比例
     * @return 以屏幕像素为单位的上边界
     */
    public float getGuiTop(float scale) {
        return (height - GUI_HEIGHT * scale) * 0.5F;
    }

    /**
     * 获取当前屏幕宽度，供可选的客户端兼容层读取。
     *
     * @return 屏幕宽度
     */
    public int getScreenWidth() {
        return width;
    }

    /**
     * 获取当前屏幕高度，供可选的客户端兼容层读取。
     *
     * @return 屏幕高度
     */
    public int getScreenHeight() {
        return height;
    }
}
