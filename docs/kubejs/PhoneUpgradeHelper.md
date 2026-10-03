# PhoneUpgradeHelper

`PhoneUpgradeHelper` 是 PayDay 提供给 KubeJS 的手机升级通用辅助类。

该类已经注册为全局绑定，可以直接在 `startup_scripts` 或 `server_scripts` 中使用：

```js
PhoneUpgradeHelper.isMeleeHit(event, player)
```

生命值回复模式通过 `PhoneUpgradeHealingMode` 访问：

```js
PhoneUpgradeHealingMode.DIRECT
PhoneUpgradeHealingMode.PERCENTAGE
```

## 方法

### `applyAttributeModifiers(baseValue, attribute)`

按照属性修饰符的运算类型计算最终数值。运算顺序与 Minecraft 属性系统一致：

- `ADDITION`：直接增加 `amount`
- `MULTIPLY_BASE`：增加 `baseValue * amount`
- `MULTIPLY_TOTAL`：将当前结果乘以 `1 + amount`

```js
const finalDamage = PhoneUpgradeHelper.applyAttributeModifiers(
    event.amount,
    player.getAttribute('payday:ranged_damage')
)
event.amount = finalDamage
```

### `applyExperienceBonus(event, owner, bonus)`

按比例增加指定玩家击杀生物获得的经验，并向上取整。

- `event`：`LivingExperienceDropEvent`
- `owner`：可能获得经验的实体
- `bonus`：经验加成比例，例如 `0.25` 表示增加25%

```js
PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25)
```

### `countUnfriendly(player, radius)`

统计玩家指定半径内的非友善生物数量。

- `player`：判定中心玩家
- `radius`：判定半径
- 返回值：非友善生物数量

```js
const hostileCount = PhoneUpgradeHelper.countUnfriendly(player, 16.0)
```

### `isUnfriendly(player, target)`

判断目标是否是对指定玩家而言的非友善生物。

```js
if (PhoneUpgradeHelper.isUnfriendly(player, event.entity)) {
    // 目标是敌对生物
}
```

### `getPlayerAttacker(source)`

从 `DamageSource` 中解析玩家攻击者，同时支持玩家直接攻击和玩家投射物攻击。

```js
const attacker = PhoneUpgradeHelper.getPlayerAttacker(event.source)
if (attacker != null) {
    attacker.heal(2.0)
}
```

### `isPlayerAttacker(source, player)`

判断伤害来源是否由指定玩家造成。

```js
if (PhoneUpgradeHelper.isPlayerAttacker(event.source, player)) {
    // 伤害由 player 造成
}
```

### `isMeleeHit(event, attacker)`

判断 `LivingHurtEvent` 是否为指定实体直接造成的近战命中。

```js
if (PhoneUpgradeHelper.isMeleeHit(event, player)) {
    // 近战命中
}
```

### `isPiercing(event)`

判断本次受伤事件是否带有穿透本模组减伤的标记。

```js
if (!PhoneUpgradeHelper.isPiercing(event)) {
    event.amount *= 0.92
}
```

### `tryMeleeHeal(event, attacker, amount, mode, cooldownTicks)`

近战命中后回复攻击者生命值，使用通用冷却键。

- `amount`：`DIRECT` 模式为直接生命值；`PERCENTAGE` 模式为最大生命值比例
- `mode`：`PhoneUpgradeHealingMode.DIRECT` 或 `PhoneUpgradeHealingMode.PERCENTAGE`
- `cooldownTicks`：冷却 tick，小于等于 `0` 表示无冷却

```js
PhoneUpgradeHelper.tryMeleeHeal(
    event,
    player,
    0.20,
    PhoneUpgradeHealingMode.PERCENTAGE,
    200
)
```

### `tryMeleeHeal(event, attacker, amount, mode, cooldownTicks, cooldownKey)`

近战回复生命值，并使用指定的持久化数据冷却键。适合不同升级分别维护冷却。

```js
PhoneUpgradeHelper.tryMeleeHeal(
    event,
    player,
    20.0,
    PhoneUpgradeHealingMode.DIRECT,
    20,
    'my_upgrade_melee_heal'
)
```

### `applyInfiltrationOnMeleeHit(event, attacker, duration)`

近战命中其他实体时给予目标渗透效果。

- `duration`：持续时间，单位为 tick

```js
PhoneUpgradeHelper.applyInfiltrationOnMeleeHit(event, player, 20)
```

### `restoreArmor(event, player, amount, cooldownTicks, cooldownKey)`

玩家击杀非友善生物后，按冷却恢复其护甲饰品中每个 `ArmorDefinition` 的护甲值。

```js
PhoneUpgradeHelper.restoreArmor(
    event,
    player,
    30.0,
    20,
    'my_upgrade_next_armor_repair'
)
```

### `isPlayerKillOfUnfriendly(event, player)`

判断玩家是否击杀了非友善生物。

```js
if (PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, player)) {
    // 击杀成立
}
```

### `isPlayerKillOfNonPlayer(event, player)`

判断玩家是否在16格内击杀了非玩家生物。

```js
if (PhoneUpgradeHelper.isPlayerKillOfNonPlayer(event, player)) {
    // 范围内击杀成立
}
```

## 完整示例

下面的示例将 Helper 用在手机升级的事件代理中：

```js
StartupEvents.registry('item', event => {
    event.create('payday:custom_melee_upgrade', 'phone_upgrade')
        .category('SPECIAL')
        .series('custom')
        .level(1)
        .addProxyEvent(
            'net.minecraftforge.event.entity.living.LivingHurtEvent',
            (hurtEvent, owner) => {
                if (owner == null || !PhoneUpgradeHelper.isMeleeHit(hurtEvent, owner)) {
                    return
                }

                PhoneUpgradeHelper.tryMeleeHeal(
                    hurtEvent,
                    owner,
                    0.20,
                    PhoneUpgradeHealingMode.PERCENTAGE,
                    200,
                    'custom_melee_upgrade_heal'
                )
                PhoneUpgradeHelper.applyInfiltrationOnMeleeHit(
                    hurtEvent,
                    owner,
                    20
                )
            }
        )
})
```
