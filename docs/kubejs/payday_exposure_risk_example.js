// 将本文件复制到 KubeJS 的 server_scripts/payday_exposure_risk_example.js。

// 示例 1：玩家手持望远镜右键时，为该物品堆设置 -8 暴露风险。
// 物品堆被放入玩家的物品栏、装备栏、副手或 Curios 后，修正才会生效。
ItemEvents.rightClicked('minecraft:spyglass', event => {
    const stack = event.getItem()
    ExposureRiskModifier.setTemporary(stack, -8.0)
})

// 示例 2：玩家第一次登录时增加 +5 永久暴露风险。
// 使用持久数据作标记，避免玩家每次登录都重复累加。
PlayerEvents.loggedIn(event => {
    const player = event.getPlayer()
    const data = player.getPersistentData()
    const marker = 'payday_kubejs_exposure_example_initialized'

    if (!data.getBoolean(marker)) {
        ExposureRiskModifier.addPermanent(player, 5.0)
        data.putBoolean(marker, true)
    }
})

