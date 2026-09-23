package com.yueyingiop.payday.compat.jei;

import com.yueyingiop.payday.client.screen.ArmorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/**
 * PayDay 与 JEI 的客户端兼容插件。
 *
 * <p>ArmorScreen 使用缩放后的姿态矩阵居中绘制，不能直接使用
 * AbstractContainerScreen 默认的 leftPos 和 topPos。这里把实际屏幕占用区域
 * 告诉 JEI，避免小窗口模式下 JEI 物品列表覆盖护甲界面。</p>
 */
@JeiPlugin
public final class PayDayJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("payday", "jei_plugin");

    /**
     * 获取 JEI 插件唯一标识。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /**
     * 注册 ArmorScreen 的实际绘制区域。
     *
     * @param registration JEI 图形处理器注册器
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(ArmorScreen.class, PayDayJeiPlugin::createArmorScreenProperties);
    }

    /**
     * 根据 ArmorScreen 当前窗口大小创建 JEI 屏幕属性。
     *
     * @param screen 护甲界面
     * @return JEI 使用的实际屏幕占用区域
     */
    private static IGuiProperties createArmorScreenProperties(ArmorScreen screen) {
        float scale = screen.getGuiScale();
        int left = Math.round(screen.getGuiLeft(scale));
        int top = Math.round(screen.getGuiTop(scale));
        int guiWidth = Math.max(1, Math.round(ArmorScreen.GUI_WIDTH * scale));
        int guiHeight = Math.max(1, Math.round(ArmorScreen.GUI_HEIGHT * scale));

        return new ArmorScreenGuiProperties(
                left,
                top,
                guiWidth,
                guiHeight,
                screen.getScreenWidth(),
                screen.getScreenHeight()
        );
    }

    /** JEI 所需的只读 GUI 属性实现，避免依赖 JEI 的内部实现类。 */
    private static final class ArmorScreenGuiProperties implements IGuiProperties {
        private final int guiLeft;
        private final int guiTop;
        private final int guiWidth;
        private final int guiHeight;
        private final int screenWidth;
        private final int screenHeight;

        private ArmorScreenGuiProperties(
                int guiLeft,
                int guiTop,
                int guiWidth,
                int guiHeight,
                int screenWidth,
                int screenHeight
        ) {
            this.guiLeft = guiLeft;
            this.guiTop = guiTop;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }

        /**
         * 获取该 JEI 界面处理器对应的界面类型。
         * @return 护甲界面类型
         */
        @Override
        public Class<? extends Screen> getScreenClass() {
            return ArmorScreen.class;
        }

        /**
         * 获取 JEI 界面的左边界。
         * @return 对应的执行结果。
         */
        @Override
        public int getGuiLeft() {
            return guiLeft;
        }

        /**
         * 获取 JEI 界面的上边界。
         * @return 对应的执行结果。
         */
        @Override
        public int getGuiTop() {
            return guiTop;
        }

        /**
         * 获取 JEI 界面宽度。
         * @return 对应的执行结果。
         */
        @Override
        public int getGuiXSize() {
            return guiWidth;
        }

        /**
         * 获取 JEI 界面高度。
         * @return 对应的执行结果。
         */
        @Override
        public int getGuiYSize() {
            return guiHeight;
        }

        /**
         * 获取 JEI 宿主屏幕宽度。
         * @return 对应的执行结果。
         */
        @Override
        public int getScreenWidth() {
            return screenWidth;
        }

        /**
         * 获取 JEI 宿主屏幕高度。
         * @return 对应的执行结果。
         */
        @Override
        public int getScreenHeight() {
            return screenHeight;
        }
    }
}
