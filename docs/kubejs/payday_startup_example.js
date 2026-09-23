// 将本文件内容复制到 KubeJS 的 startup_scripts/payday.js 后即可按需修改。

StartupEvents.registry('item', event => {
    event.create('custom_vest', 'payday:armor_curio')
        .upgradeSlots(4)
})

StartupEvents.registry('item', event => {
    event.create('custom_impact_slot', 'payday:armor_slot')
        .armorType('impact')
        .armorValue(25)
        .recoverySeconds(1.75)
        .addAttribute('minecraft:generic.movement_speed', 'MULTIPLY_BASE', 0.15)
})

StartupEvents.registry('item', event => {
    event.create('custom_resistance_slot', 'payday:armor_slot')
        .armorType('resistance')
        .armorValue(75)
        .recoverySeconds(1.25)
        .addPotionEffect('minecraft:fire_resistance', 0)
})

StartupEvents.init(event => {
    new ArmorModifier('payday:custom_vest')
        .addAttribute('minecraft:generic.max_health', 'ADDITION', 10)
        .modifyAttribute('minecraft:generic.movement_speed', 'MULTIPLY_BASE', 0.1)
        .removeAttribute('minecraft:generic.max_health', 'ADDITION')
})
