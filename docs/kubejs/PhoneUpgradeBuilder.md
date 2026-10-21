# PhoneUpgradeBuilder

PhoneUpgradeBuilder 是 KubeJS 用来注册手机升级物品的链式 Builder。

它同时配置两类数据：

1. 手机升级物品元数据：分类、系列、等级、前置要求和 tooltip 效果键。
2. PhoneUpgradeAction 实际效果：属性、药水、弹匣扩容、伤害穿透、伤害免疫和事件代理。

## 注册物品

```js
StartupEvents.registry('item', event => {
    event.create('guardian_1', 'payday:phone_upgrade')
        .category('SPECIAL')
        .series('guardian')
        .level(1)
        .effectKey('guardian_1')
        .attribute('minecraft:generic.max_health', 0.10, 'MULTIPLY_TOTAL')
})
```

物品类型支持以下两种写法：

```js
event.create('guardian_1', 'phone_upgrade')
event.create('guardian_1', 'payday:phone_upgrade')
```

没有调用 effectKey 时，效果键默认使用物品注册 ID 的路径部分。

PhoneUpgradeBuilder 继承 KubeJS 的 ItemBuilder，因此也可以继续使用：

```js
.texture('payday:item/phone_upgrade/guardian_1')
.rarity('RARE')
.maxStackSize(1)
```

## 元数据函数

### category(category)

设置升级分类。

参数：

- category：BASIC 或 SPECIAL，不区分大小写。

返回当前 PhoneUpgradeBuilder。

默认分类是 SPECIAL。没有显式调用 prerequisite 时，切换分类会同步更新默认前置要求。

### series(series)

设置升级系列标识。

参数：

- series：非空字符串，例如 muscle、assassin、custom。

返回当前 PhoneUpgradeBuilder。

### level(level)

设置升级等级。

参数：

- level：大于等于 1 的整数。

返回当前 PhoneUpgradeBuilder。

默认规则：

- 特殊升级等级大于 1 时需要前置升级。
- 基础升级默认不需要前置升级。
- 显式调用 prerequisite 后，不再自动覆盖该设置。

### prerequisite(required)

显式设置是否需要前置升级。

参数：

- required：布尔值。

返回当前 PhoneUpgradeBuilder。

示例：

```js
.level(5)
.prerequisite(false)
```

### effectKey(effectKey)

设置物品 tooltip 的效果键。

物品会读取以下翻译键：

```text
tooltip.payday.upgrade_effect.<effectKey>
```

参数：

- effectKey：非空字符串。

返回当前 PhoneUpgradeBuilder。

### action(action)

直接设置已经构建完成的 PhoneUpgradeAction。

参数：

- action：PhoneUpgradeAction 实例。

返回当前 PhoneUpgradeBuilder。

示例：

```js
const action = PhoneUpgradeAction.builder()
    .attribute('minecraft:generic.max_health', 0.25, 'MULTIPLY_TOTAL')
    .build()

StartupEvents.registry('item', event => {
    event.create('custom_action_upgrade', 'payday:phone_upgrade')
        .series('custom')
        .level(1)
        .action(action)
})
```

如果同时调用了 attribute 等效果函数和 action(action)，最终物品使用显式传入的 action。

### action(builder)

使用 PhoneUpgradeAction.Builder 构建动作并设置给物品。

参数：

- builder：PhoneUpgradeAction.Builder 实例。

返回当前 PhoneUpgradeBuilder。

示例：

```js
StartupEvents.registry('item', event => {
    event.create('custom_action_upgrade', 'payday:phone_upgrade')
        .action(PhoneUpgradeAction.builder()
            .pierce(0.25)
            .build())
})
```

## 效果函数(效果函数都来自于 PhoneUpgradeAction.Builder)

### attribute(attributeId, amount, operation)

添加属性修饰符。

参数：

- attributeId：属性注册表 ID，例如 minecraft:generic.max_health。
- amount：属性修饰数值。
- operation：ADDITION、MULTIPLY_BASE 或 MULTIPLY_TOTAL。

自定义属性（例如 `payday:ranged_damage`）也可以直接传入。属性 ID 会延迟解析，
不会在 KubeJS 启动注册物品时提前查找 Forge 属性注册表。

返回当前 PhoneUpgradeBuilder。

示例：

```js
.attribute('minecraft:generic.max_health', 0.60, 'MULTIPLY_TOTAL')
.attribute('minecraft:generic.movement_speed', 0.15, 'MULTIPLY_BASE')
```

TACZ 爆头伤害也可以直接使用通用属性，不需要额外的事件回调：

```js
.attribute('payday:headshot_damage', 0.25, 'MULTIPLY_TOTAL')
```

### effect(effectId, durationTicks, amplifier)

添加药水效果。

参数：

- effectId：药水效果 ID，例如 minecraft:fire_resistance。
- durationTicks：持续时间，单位为 tick。
- amplifier：效果等级，0 表示一级。

返回当前 PhoneUpgradeBuilder。

### effect(effectId, durationTicks, amplifier, ambient, showParticles, showIcon)

添加药水效果并控制显示参数。

参数：

- ambient：是否使用环境效果显示。
- showParticles：是否显示粒子。
- showIcon：是否显示图标。

返回当前 PhoneUpgradeBuilder。

### ammoExpansion(amount, operation, ...gunTypes)

添加 TACZ 枪械弹匣扩容。

参数：

- amount：扩容数值。
- operation：MULTIPLY 或 ADDITION。
- gunTypes：可选的枪械类型；不传时支持全部枪械。

计算方式：

- MULTIPLY：ceil(容量 * (1 + amount))。
- ADDITION：ceil(容量 + amount)。

示例：

```js
// 125 -> ceil(125 * 1.5) = 188
.ammoExpansion(0.50, 'MULTIPLY')

// 直接增加 20 发
.ammoExpansion(20, 'ADDITION')
```

该效果只改变最终弹匣容量，不会在安装升级时直接增加当前弹药。

### pierce(probability)

设置穿透本模组伤害减免和护甲吸收的概率。

参数：

- probability：0.0 到 1.0 之间的概率。

返回当前 PhoneUpgradeBuilder。

### immune(damageTypeTagId, probability)

添加伤害类型标签免疫。

参数：

- damageTypeTagId：伤害类型标签 ID，例如 minecraft:is_fire。
- probability：0.0 到 1.0 之间的概率。

返回当前 PhoneUpgradeBuilder。

示例：

```js
.immune('minecraft:is_fire', 1.0)
```

### curioTick(callback)

设置手机升级的 Curios tick 回调。

回调参数依次为：

1. identifier：Curios 槽位标识。
2. index：升级在手机中的槽位索引。
3. livingEntity：佩戴手机的实体。
4. itemStack：当前升级物品堆。

示例：

```js
.curioTick((identifier, index, livingEntity, itemStack) => {
    // 执行自定义 tick 逻辑
})
```

返回当前 PhoneUpgradeBuilder。

### armorSoltModifier(callback)

设置护甲插槽修改回调。方法名中的 Solt 是现有 Java API 的兼容拼写。

回调参数：

- modifier：ArmorModifier 实例。

示例：

```js
.armorSoltModifier(modifier => {
    modifier.addArmorMaxMultiplier(0.10)
})
```

返回当前 PhoneUpgradeBuilder。

### bindKeyActivate(key, callback)

为当前手机升级绑定按键激活回调。

参数：

- key：按键码，例如 `90` 表示 Z 键。
- callback：服务端激活回调，参数为玩家实体；返回 `true` 表示成功激活。

示例：

```js
.bindKeyActivate(90, player => {
    if (player.level.isClientSide()) {
        return false
    }
    // 执行按键技能
    return true
})
```

服务端只会对玩家已安装的升级执行回调。

### addProxyEvent(eventClassName, callback)

代理任意 Forge 事件。

参数：

- eventClassName：事件完整类名，例如 net.minecraftforge.event.entity.living.LivingHealEvent。
- callback：接收 event 和 owner 的回调。

示例：

```js
.addProxyEvent(
    'net.minecraftforge.event.entity.living.LivingHealEvent',
    (event, owner) => {
        if (event.entity == owner) {
            event.amount *= 1.20
        }
    }
)
```

返回当前 PhoneUpgradeBuilder。

事件对象能否修改以及字段名称，取决于 Forge 事件本身。

## 物品函数

### maxStackSize(maxStackSize)

设置手机升级物品的最大堆叠数量。

这是继承自 KubeJS ItemBuilder 的方法，在 PhoneUpgradeBuilder 中保留链式返回。

参数：

- maxStackSize：最大堆叠数量。

返回当前 PhoneUpgradeBuilder。

### createObject()

由 KubeJS 注册系统调用，根据 Builder 配置创建 PhoneUpgradeItem。

一般不需要在脚本中手动调用。

返回 PhoneUpgradeItem。

## 完整案例

```js
StartupEvents.registry('item', event => {
    event.create('special_guardian_5', 'payday:phone_upgrade')
        .category('SPECIAL')
        .series('guardian')
        .level(5)
        .prerequisite(true)
        .effectKey('special_guardian_5')
        .attribute('minecraft:generic.max_health', 0.60, 'MULTIPLY_TOTAL')
        .effect('minecraft:regeneration', 100, 0)
        .ammoExpansion(0.50, 'MULTIPLY')
        .pierce(0.25)
        .immune('minecraft:is_fire', 0.10)
        .maxStackSize(1)
        .texture('payday:item/phone_upgrade/guardian_5')
})
```

## 限制

以下 Java 重载存在，但普通 KubeJS 脚本应优先使用字符串属性 ID：

- effect(MobEffectInstance)
- immune(TagKey<DamageType>, ...)
- immune(Predicate<DamageSource>, ...)
- addProxyEvent(Class, ...)

这些方法需要 Java 对象、Supplier、Predicate 或 Class。
