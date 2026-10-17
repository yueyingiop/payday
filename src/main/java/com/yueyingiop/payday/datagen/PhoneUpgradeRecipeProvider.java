package com.yueyingiop.payday.datagen;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.PhoneUpgradeRecipeGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates the built-in phone upgrade recipes without hand-written JSON files. */
public final class PhoneUpgradeRecipeProvider implements DataProvider {
    private final PackOutput.PathProvider taczRecipePathProvider;
    private final PackOutput.PathProvider craftingRecipePathProvider;

    public PhoneUpgradeRecipeProvider(PackOutput output) {
        this.taczRecipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                "recipes/tacz");
        this.craftingRecipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                "recipes");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> saves = new ArrayList<>();
        for (PhoneUpgradeRecipeGenerator.GeneratedRecipe recipe
                : PhoneUpgradeRecipeGenerator.builtInRecipes()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PayDay.MODID, recipe.id());
            saves.add(DataProvider.saveStable(output, recipe.json(), taczRecipePathProvider.json(id)));
        }
        for (PhoneUpgradeRecipeGenerator.GeneratedRecipe recipe
                : PhoneUpgradeRecipeGenerator.builtInCraftingRecipes()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PayDay.MODID, recipe.id());
            saves.add(DataProvider.saveStable(output, recipe.json(), craftingRecipePathProvider.json(id)));
        }
        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "PayDay phone upgrade recipes";
    }
}
