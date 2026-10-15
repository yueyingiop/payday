package com.yueyingiop.payday.compat.jei;

import com.yueyingiop.payday.init.RegistryItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** JEI 中独立展示炮台冷却液和过载液的配方类别。 */
public final class TurretFluidRecipeCategory extends AbstractRecipeCategory<TurretFluidJeiRecipe> {
    public static final RecipeType<TurretFluidJeiRecipe> RECIPE_TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath("payday", "turret_fluid"),
            TurretFluidJeiRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 78;
    private final IDrawable background;

    public TurretFluidRecipeCategory(IGuiHelper guiHelper) {
        super(RECIPE_TYPE,
                Component.translatable("jei.payday.turret_fluid.recipe_title"),
                guiHelper.createDrawableItemStack(RegistryItems.TURRET.get().getDefaultInstance()),
                WIDTH,
                HEIGHT);
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder layout, TurretFluidJeiRecipe recipe,
                          IFocusGroup focuses) {
        layout.addSlot(RecipeIngredientRole.INPUT, 16, 20)
                .setStandardSlotBackground()
                .setFluidRenderer(1000, true, 16, 16)
                .addIngredient(ForgeTypes.FLUID_STACK, recipe.fluidStack());
        layout.addSlot(RecipeIngredientRole.OUTPUT, 112, 20)
                .setOutputSlotBackground()
                .addIngredient(VanillaTypes.ITEM_STACK, RegistryItems.TURRET.get().getDefaultInstance());
    }

    @Override
    public void draw(TurretFluidJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.typeText(), 4, 4, 0x404040, false);
        drawWrapped(graphics, font, recipe.effectText(), 4, 48, WIDTH - 8);
    }

    private static void drawWrapped(GuiGraphics graphics, Font font, Component text,
                                    int x, int y, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, 0x404040, false);
            y += 9;
        }
    }

}
