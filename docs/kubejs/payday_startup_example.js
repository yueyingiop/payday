// 将本文件内容复制到 KubeJS 的 startup_scripts/payday.js 后即可按需修改。

StartupEvents.registry('item', event => {
    // 创建护甲饰品(ArmorCurioItem),由护甲饰品构造器(ArmorCurioBuilder)提供函数
    event.create('kjs:test_armor_curio', "payday:armor_curio")
        .upgradeSlots(18) // 设置升级插槽数量
        .addAttribute('minecraft:generic.movement_speed', 'MULTIPLY_BASE', -0.1)
        
        
    // 创建升级插槽(ArmorSlotItem),由插槽升级构造器(ArmorSlotBuilder)提供函数
    event.create('kjs:test_armor_slot', "payday:armor_slot")
        .addArmor("resistance", 100)
        .addArmor("adaptive", 50)
        .addArmor("impact", 10)
        .recoverySeconds(2) // 设置护甲恢复时间(秒)
        .addPotionEffect("minecraft:fire_resistance", 0) // 添加药水效果
        .addAttribute('minecraft:generic.movement_speed', 'MULTIPLY_BASE', 0.15) // 添加属性修饰符
})

StartupEvents.registry("item", event=>{
    event.create("kjs:test_phone_upgrade", "payday:phone_upgrade")
        .category("SPECIAL")
        .series("apple") // 设置系列
        .level(1) // 设置等级
        .prerequisite(false) // 设置是否需要前置升级
        .effectKey("test_phone_upgrade") // JEItooltip效果键


        .action()
        // 以下所有函数都相当于在创建一个PhoneUpgradeAction传入.action()


        // 添加属性修饰符 
        // 等效于传入:
        // PhoneUpgradeAction.builder()
        //   .attribute('minecraft:generic.max_health', 0.6, 'MULTIPLY_TOTAL')
        // .build()
        .attribute(
            'minecraft:generic.max_health', 
            0.60, 
            'MULTIPLY_TOTAL'
        )

        // 添加属性修饰符
        // 等效于传入:
        // PhoneUpgradeAction.builder()
        //   .effect("minecraft:fire_resistance", 100, 0)
        // .build()
        .effect("minecraft:fire_resistance", 100, 0)
        // .effect("minecraft:fire_resistance", 100, 0, true, true, true) // 写法二
        
        .ammoExpansion(0.50, 'MULTIPLY') // 设置弹药扩容
        // .ammoExpansion(20, "ADDITION") // 设置弹药扩容

        .immune('minecraft:is_fire', 1.0) // 概率免疫伤害

        .curioTick((identifier, index, livingEntity, itemStack) => {
            // 执行自定义 tick 逻辑
        })
        .addProxyEvent(
            'net.minecraftforge.event.entity.living.LivingHealEvent',
            (event, owner) => {
                // 代理逻辑
            }
        ) // 代理事件

        // maxStackSize之后提供的就是ItemBuilder了,不再是PhoneUpgradeBuilder
        .maxStackSize(1) // 设置最大堆叠数量
        .barColor(barColor)

})
