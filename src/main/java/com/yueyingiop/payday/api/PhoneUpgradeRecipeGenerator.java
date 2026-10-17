package com.yueyingiop.payday.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates TACZ attachment-workbench recipes for phone upgrades.
 *
 * <p>All methods return normal JSON objects, so they work both with
 * {@code event.custom(...)} in KubeJS and with Forge data generation.</p>
 */
public final class PhoneUpgradeRecipeGenerator {
    public static final String RECIPE_TYPE = "tacz:gun_smith_table_crafting";
    public static final String OUTPUT_GROUP = "payday:phone_upgrades";

    private PhoneUpgradeRecipeGenerator() {
    }

    public static JsonObject basicLevelOne(String outputItemId) {
        return recipe(outputItemId, material("minecraft:iron_ingot", 1),
                material("minecraft:leather", 1), material("minecraft:redstone", 1));
    }

    public static JsonObject basicUpgrade(String previousItemId, String outputItemId) {
        return recipe(outputItemId, material(previousItemId, 2),
                material("minecraft:slime_ball", 1));
    }

    public static JsonObject specialLevelOne(String outputItemId) {
        return recipe(outputItemId, material("minecraft:iron_ingot", 1),
                material("minecraft:leather", 1), material("minecraft:redstone", 1),
                material("minecraft:slime_ball", 2));
    }

    public static JsonObject specialUpgrade(String previousItemId, String outputItemId) {
        return recipe(outputItemId, material(previousItemId, 2),
                material("minecraft:magma_cream", 1), material("minecraft:honeycomb", 1));
    }

    public static JsonObject material(String itemId, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Material count must be positive");
        }
        JsonObject material = new JsonObject();
        JsonObject item = new JsonObject();
        item.addProperty("item", requireId(itemId));
        material.add("item", item);
        material.addProperty("count", count);
        return material;
    }

    public static JsonObject recipe(String outputItemId, JsonObject... materials) {
        return recipeInGroup(OUTPUT_GROUP, outputItemId, materials);
    }

    /** Creates a TACZ recipe in the supplied attachment-workbench tab group. */
    public static JsonObject recipeInGroup(String group, String outputItemId,
                                           JsonObject... materials) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Recipe group is required");
        }
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", RECIPE_TYPE);
        JsonArray ingredients = new JsonArray();
        for (JsonObject material : materials) {
            if (material == null) {
                throw new IllegalArgumentException("Recipe materials cannot contain null");
            }
            ingredients.add(material);
        }
        recipe.add("materials", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("type", "custom");
        result.addProperty("group", group);
        result.add("item", itemStack(outputItemId, 1));
        recipe.add("result", result);
        return recipe;
    }

    /** Creates a recipe for the general PayDay TACZ tab. */
    public static JsonObject miscRecipe(String outputItemId, JsonObject... materials) {
        return recipeInGroup("payday:misc", outputItemId, materials);
    }

    private static JsonObject vanillaShapedRecipe(String outputItemId,
                                                  String patternA,
                                                  String patternB,
                                                  String patternC,
                                                  String... keyDefinitions) {
        if (keyDefinitions.length % 2 != 0) {
            throw new IllegalArgumentException("Shaped recipe keys must be symbol/item pairs");
        }
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");

        JsonArray pattern = new JsonArray();
        pattern.add(requirePattern(patternA));
        pattern.add(requirePattern(patternB));
        pattern.add(requirePattern(patternC));
        recipe.add("pattern", pattern);

        JsonObject keys = new JsonObject();
        for (int i = 0; i < keyDefinitions.length; i += 2) {
            String symbol = keyDefinitions[i];
            if (symbol == null || symbol.length() != 1 || symbol.charAt(0) == ' ') {
                throw new IllegalArgumentException("Shaped recipe key symbols must be one non-space character");
            }
            JsonObject ingredient = new JsonObject();
            ingredient.addProperty("item", requireId(keyDefinitions[i + 1]));
            keys.add(symbol, ingredient);
        }
        recipe.add("key", keys);
        recipe.add("result", itemStack(outputItemId, 1));
        return recipe;
    }

    /** Returns the built-in recipes as stable recipe-id/JSON pairs. */
    public static List<GeneratedRecipe> builtInRecipes() {
        List<GeneratedRecipe> recipes = new ArrayList<>();
        addBasicSeries(recipes, "attack", 4);
        addBasicSeries(recipes, "ranged", 4);
        addBasicSeries(recipes, "armor", 4);
        addBasicSeries(recipes, "health", 4);
        addBasicSeries(recipes, "speed", 4);
        addSpecialSeries(recipes, "helmet_dance", 1);
        addSpecialSeries(recipes, "forest", 1);
        addSpecialSeries(recipes, "wardrobe", 1);
        addSpecialSeries(recipes, "last_stand", 1);
        addSpecialSeries(recipes, "muscle", 5);
        addSpecialSeries(recipes, "leader", 5);
        addSpecialSeries(recipes, "armorer", 5);
        addSpecialSeries(recipes, "rogue", 5);
        addSpecialSeries(recipes, "assassin", 5);
        addSpecialSeries(recipes, "thief", 5);
        addSpecialSeries(recipes, "infiltrator", 5);
        addSpecialSeries(recipes, "antisocial", 5);
        addSpecialSeries(recipes, "anarchist", 5);
        addSpecialSeries(recipes, "hacker", 5);
        recipes.add(new GeneratedRecipe("ammo_bag", miscRecipe("payday:ammo_bag",
                material("minecraft:scute", 1),
                material("minecraft:lime_dye", 1),
                material("minecraft:amethyst_shard", 4))));
        return List.copyOf(recipes);
    }

    /** Returns built-in vanilla crafting-table recipes. */
    public static List<GeneratedRecipe> builtInCraftingRecipes() {
        return List.of(
                new GeneratedRecipe("ammo_box", vanillaShapedRecipe("payday:ammo_box",
                        "ABA", "ACA", "ABA",
                        "A", "minecraft:lime_wool",
                        "B", "minecraft:honeycomb",
                        "C", "minecraft:chest")),
                new GeneratedRecipe("phone", vanillaShapedRecipe("payday:phone",
                        "ABC", "DEF", "GHI",
                        "A", "minecraft:daylight_detector",
                        "B", "minecraft:redstone_repeater",
                        "C", "minecraft:lightning_rod",
                        "D", "minecraft:clock",
                        "E", "minecraft:observer",
                        "F", "minecraft:tripwire_hook",
                        "G", "minecraft:note_block",
                        "H", "minecraft:lectern",
                        "I", "minecraft:redstone_lamp")));
    }

    private static void addBasicSeries(List<GeneratedRecipe> recipes, String series, int levels) {
        String previous = null;
        for (int level = 1; level <= levels; level++) {
            String output = upgradeId("basic_" + series + "_" + level);
            JsonObject json = level == 1 ? basicLevelOne(output) : basicUpgrade(previous, output);
            recipes.add(new GeneratedRecipe("phone_upgrade/basic_" + series + "_" + level, json));
            previous = output;
        }
    }

    private static void addSpecialSeries(List<GeneratedRecipe> recipes, String series, int levels) {
        String previous = null;
        for (int level = 1; level <= levels; level++) {
            String suffix = levels == 1 ? series : series + "_" + level;
            String output = upgradeId("special_" + suffix);
            JsonObject json = level == 1 ? specialLevelOne(output) : specialUpgrade(previous, output);
            recipes.add(new GeneratedRecipe("phone_upgrade/special_" + suffix, json));
            previous = output;
        }
    }

    private static String upgradeId(String id) {
        return "payday:phone_upgrade/" + id;
    }

    private static JsonObject itemStack(String itemId, int count) {
        JsonObject item = new JsonObject();
        item.addProperty("item", requireId(itemId));
        item.addProperty("count", count);
        return item;
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item id is required");
        }
        return id;
    }

    private static String requirePattern(String pattern) {
        if (pattern == null || pattern.isEmpty() || pattern.length() > 3) {
            throw new IllegalArgumentException("Shaped recipe rows must contain 1 to 3 characters");
        }
        return pattern;
    }

    public record GeneratedRecipe(String id, JsonObject json) {
    }
}
