// 放入 KubeJS 的 server_scripts 目录。

// 手机装备变化时 PayDay 会自动刷新自动属性 HUD。
// 如果其他脚本直接修改了手机内容，可以手动重新计算：
PlayerEvents.inventoryChanged(event => {
    if (event.item.id == 'payday:phone') {
        ProbabilityHudManager.refresh(event.player)
    }
})

// 也可以创建一个普通概率 HUD。
PlayerEvents.loggedIn(event => {
    const player = event.player
    CooldownHudManager.showProbability(player, 'kjs_raw_attribute_value',
        CooldownHud.builder()
            .type('PROBABILITY')
            .texture('payday:gui/cd_attribute')
            .texturePosition(0, 16)
            .value(1500)
            .percentage(false)
            .showPercentage(true)
            .showName(Component.translatable('attribute.name.payday.dodge_chance'))
            .build())
})
