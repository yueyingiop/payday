// PayDay phone upgrade recipe generator examples for KubeJS.
// Put this in server_scripts when you want to add custom upgrades.

ServerEvents.recipes(event => {
    // Example 1: a level-one basic upgrade.
    event.custom(PhoneUpgradeRecipeGenerator.basicLevelOne(
        'kubejs:phone_upgrade/basic_example_1'
    ))

    // Example 2: combine two previous upgrades and one slime ball.
    event.custom(PhoneUpgradeRecipeGenerator.basicUpgrade(
        'kubejs:phone_upgrade/basic_example_1',
        'kubejs:phone_upgrade/basic_example_2'
    ))

    // Example 3: a custom ammo bag recipe in the PayDay miscellaneous tab.
    event.custom(PhoneUpgradeRecipeGenerator.miscRecipe(
        'kubejs:ammo_bag',
        PhoneUpgradeRecipeGenerator.material('minecraft:scute', 1),
        PhoneUpgradeRecipeGenerator.material('minecraft:lime_dye', 1),
        PhoneUpgradeRecipeGenerator.material('minecraft:amethyst_shard', 4)
    ))
})
