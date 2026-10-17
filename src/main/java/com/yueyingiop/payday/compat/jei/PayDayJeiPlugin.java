package com.yueyingiop.payday.compat.jei;

import com.yueyingiop.payday.client.screen.ArmorScreen;
import com.yueyingiop.payday.api.TurretFluid;
import com.yueyingiop.payday.api.TurretFluidManager;
import com.yueyingiop.payday.init.RegistryItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new TurretFluidRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(RegistryItems.TURRET.get(),
                TurretFluidRecipeCategory.RECIPE_TYPE);
    }

    /** Registers the level-specific information for the three Anarchist upgrades. */
    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        List<net.minecraft.world.item.ItemStack> upgrades = List.of(
                RegistryItems.SPECIAL_ANARCHY_2.get().getDefaultInstance(),
                RegistryItems.SPECIAL_ANARCHY_3.get().getDefaultInstance(),
                RegistryItems.SPECIAL_ANARCHY_4.get().getDefaultInstance()
        );
        registration.addIngredientInfo(upgrades, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.payday.linear_edge.description"),
                Component.translatable("jei.payday.linear_edge.formula"),
                Component.translatable("jei.payday.linear_edge.variables"),
                Component.translatable("jei.payday.linear_edge.life_conversion"),
                Component.translatable("jei.payday.linear_edge.example_level_2"),
                Component.translatable("jei.payday.linear_edge.example_level_3"),
                Component.translatable("jei.payday.linear_edge.example_level_4"));

        registration.addItemStackInfo(RegistryItems.SPECIAL_THIEF_3.get().getDefaultInstance(),
                Component.translatable("jei.payday.redstone_signal.description"),
                Component.translatable("jei.payday.redstone_signal.examples"),
                Component.translatable("jei.payday.redstone_signal.duration"));

        registration.addItemStackInfo(RegistryItems.SPECIAL_LEADER_5.get().getDefaultInstance(),
                Component.translatable("jei.payday.hostage_event.description"),
                Component.translatable("jei.payday.hostage_event.counting"),
                Component.translatable("jei.payday.hostage_event.example"));

        List<TurretFluidJeiRecipe> turretFluidRecipes = createTurretFluidRecipes();
        registration.addRecipes(TurretFluidRecipeCategory.RECIPE_TYPE, turretFluidRecipes);
        registerTurretFluidInfo(registration, turretFluidRecipes);
    }

    /** 注册所有炮台流体的纹理、类型和行为参数说明。 */
    private static void registerTurretFluidInfo(
            mezz.jei.api.registration.IRecipeRegistration registration,
            List<TurretFluidJeiRecipe> recipes) {
        for (TurretFluidJeiRecipe recipe : recipes) {
            FluidStack stack = recipe.fluidStack();
            registration.addIngredientInfo(stack, ForgeTypes.FLUID_STACK,
                    recipe.typeText(), recipe.effectText());
        }
    }

    /** 创建所有当前可用的炮台流体配方。 */
    private static List<TurretFluidJeiRecipe> createTurretFluidRecipes() {
        List<TurretFluidJeiRecipe> recipes = new ArrayList<>();
        for (TurretFluid definition : TurretFluidManager.all()) {
            var fluid = ForgeRegistries.FLUIDS.getValue(definition.fluidId());
            if (fluid == null) {
                continue;
            }
            recipes.add(new TurretFluidJeiRecipe(definition, new FluidStack(fluid, 1000)));
        }
        return recipes;
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
