# PhoneUpgradeAction

PhoneUpgradeAction 是手机升级的底层效果对象。

KubeJS 中可以通过绑定直接创建：

~~~js
const action = PhoneUpgradeAction.builder()
    .attribute('minecraft:generic.max_health', 0.10, 'MULTIPLY_TOTAL')
    .build()
~~~

## 1. 静态函数

### 1.1. PhoneUpgradeAction.builder()

创建手机升级动作 Builder。

返回：

- PhoneUpgradeAction.Builder

Builder 的配置函数都返回 Builder，因此支持链式调用。

### 1.2. PhoneUpgradeAction.clearAttributeModifiers(entity)

移除实体上由 PayDay 手机升级添加的属性修饰符。

参数：

- entity：要清理的实体。

返回：

- 无返回值。

通常不需要在脚本中手动调用，手机 tick 会负责刷新。

### 1.3. PhoneUpgradeAction.refreshAmmoExpansion(entity)

重新计算实体主手 TACZ 枪械的最终弹匣容量。

参数：

- entity：玩家实体。

返回：

- 无返回值。

该函数不会直接补充当前弹药，只同步最终容量；如果当前弹药超过容量，会裁剪到新容量。

## 2. PhoneUpgradeAction.Builder 函数

### 2.1.1. attribute(attribute, amount, operation)

通过属性对象或属性注册表 ID 添加属性修饰符。

KubeJS 推荐传入字符串，例如 `payday:ranged_damage`。字符串会延迟到升级实际应用时解析，
因此可以安全引用 PayDay 自定义属性。

参数：

- attribute：属性对象或属性注册表 ID 字符串
- amount：修饰值。
- operation：运算类型字符串或 `AttributeModifier.Operation`，支持 `ADDITION`、`MULTIPLY_BASE`、`MULTIPLY_TOTAL`。

返回当前 Builder。

TACZ 爆头属性由 PayDay 的通用事件统一应用，不需要在 KubeJS 中额外注册爆头事件：

```js
.attribute('payday:headshot_damage', 0.25, 'MULTIPLY_TOTAL')
```

计算时使用 TACZ 当前爆头倍率作为基础值：`ADDITION` 直接加值，`MULTIPLY_BASE`
按基础值增加，`MULTIPLY_TOTAL` 按当前总值增加。

### 2.2.1. effect(effectId, durationTicks, amplifier)

通过注册表 ID 添加药水效果。

参数：

- effectId：药水效果 ID。
- durationTicks：持续时间，单位为 tick。
- amplifier：效果等级，0 表示一级。

返回当前 Builder。

### 2.2.2. effect(effectId, durationTicks, amplifier, ambient, showParticles, showIcon)

添加药水效果并控制显示参数。

参数：

- ambient：是否使用环境效果显示。
- showParticles：是否显示粒子。
- showIcon：是否显示图标。

返回当前 Builder。

### 2.2.3. effect(MobEffectInstance)

Java 专用重载，直接添加 MobEffectInstance。

普通 KubeJS 脚本应使用字符串重载。

### 2.3. ammoExpansion(amount, operation, ...gunTypes)

添加 TACZ 弹匣扩容。

参数：

- amount：扩容数值。
- operation：MULTIPLY 或 ADDITION。
- gunTypes：可选枪械类型；不传时支持全部枪械。

返回当前 Builder。

计算方式：

- MULTIPLY：ceil(容量 * (1 + amount))。
- ADDITION：ceil(容量 + amount)。

### 2.4. pierce(probability)

设置穿透本模组伤害减免和护甲吸收的概率。

参数：

- probability：0.0 到 1.0。

返回当前 Builder。

### 2.4.1. immune(damageTypeTagId, probability)

通过伤害类型标签 ID 添加概率免疫。

参数：

- damageTypeTagId：伤害类型标签 ID，例如 minecraft:is_fire。
- probability：免疫概率。

返回当前 Builder。

### 2.4.2. immune(TagKey<DamageType>, probability)

Java 专用重载，直接传入伤害类型标签对象。

### 2.4.3. immune(Predicate<DamageSource>, probability)

Java 专用重载，使用自定义 Java Predicate 判断伤害来源。

普通 KubeJS 脚本应使用字符串重载。

### 2.5. curioTick(callback)

设置 Curios tick 回调。

回调参数：

~~~js
(identifier, index, livingEntity, itemStack)
~~~

参数含义：

- identifier：Curios 槽位标识。
- index：升级在手机中的槽位索引。
- livingEntity：佩戴手机的实体。
- itemStack：当前升级物品堆。

返回当前 Builder。

### 2.5. armorSoltModifier(callback)

设置护甲插槽修改回调。

方法名中的 Solt 是现有 Java API 的兼容拼写。

回调参数：

~~~js
(armorModifier)
~~~

返回当前 Builder。

### 2.6. bindKeyActivate(key, callback)

为手机升级绑定按键，并在服务端按下该按键时执行激活回调。

参数：

- key：按键码。`Z` 键为 `90`，Java 中可以使用 `InputConstants.KEY_Z`。
- callback：服务端激活回调，参数为 `player`；成功激活返回 `true`，冷却中或条件不满足返回 `false`。

示例：

~~~js
PhoneUpgradeAction.builder()
    .bindKeyActivate(90, player => {
        if (player.level.isClientSide()) {
            return false
        }
        // 执行激活逻辑
        return true
    })
    .build()
~~~

服务端会自动检查玩家是否安装了该升级，客户端只发送按键码，不能直接伪造升级效果。

### 2.6.1. addProxyEvent(eventClassName, callback)

通过事件完整类名添加 Forge 事件代理。

参数：

- eventClassName：Forge 事件完整类名。
- callback：接收 event 和 owner 的回调。

示例：

~~~js
.addProxyEvent(
    'net.minecraftforge.event.entity.living.LivingHealEvent',
    (event, owner) => {
        if (event.entity == owner) {
            event.amount *= 1.20
        }
    }
)
~~~

### 2.6.2.addProxyEvent(Class, callback)

Java 专用重载，直接传入事件 Class。

普通 KubeJS 脚本应使用事件完整类名重载。

### 2.7. build()

构建不可变的 PhoneUpgradeAction。

返回：

- PhoneUpgradeAction。

## 3. PhoneUpgradeAction 实例函数(一般情况下没用)

### 3.1. action.getAttributes()

返回该动作声明的属性定义列表。

返回：

- 不可变的属性定义列表。

### 3.2. action.getEffects()

返回该动作声明的药水效果列表。

返回：

- 不可变的药水效果列表。

### 3.3.1. action.apply(identifier, slotIndex, entity, upgrade)

将动作应用到实体。

参数：

- identifier：Curios 槽位标识。
- slotIndex：手机升级槽位索引。
- entity：目标实体。
- upgrade：升级物品堆。

该重载没有手机物品上下文，因此不会执行护甲插槽修改回调。

### 3.3.2.action.apply(identifier, slotIndex, entity, upgrade, phoneStack)

将动作应用到实体，并传入手机物品上下文。

参数：

- identifier：Curios 槽位标识。
- slotIndex：手机升级槽位索引。
- entity：目标实体。
- upgrade：升级物品堆。
- phoneStack：当前装备的手机物品堆。

该重载会额外执行护甲插槽修改回调。

### 3.4. action.applyArmorSoltModifier(phoneStack, entity)

遍历实体装备的护甲饰品，并执行护甲插槽修改回调。

参数：

- phoneStack：手机物品堆。
- entity：代理实体。

客户端不会执行护甲状态修改。

### 3.5. action.dispatchProxyEvent(event, owner)

向当前动作注册的事件代理分发 Forge 事件。

参数：

- event：Forge 事件对象。
- owner：手机升级持有者。

### 3.6. action.applyDamageRules(event, owner)

应用动作声明的穿透和伤害免疫规则。

参数：

- event：当前 Forge 事件。
- owner：手机升级持有者。

通常由 PayDay 通用事件分发器调用，不建议普通脚本手动调用。

## 4. 完整案例

~~~js
const customAction = PhoneUpgradeAction.builder()
    .attribute('minecraft:generic.max_health', 0.20, 'MULTIPLY_TOTAL')
    .attribute('minecraft:generic.movement_speed', 0.10, 'MULTIPLY_BASE')
    .effect('minecraft:fire_resistance', 200, 0)
    .ammoExpansion(0.50, 'MULTIPLY')
    .pierce(0.25)
    .immune('minecraft:is_fire', 1.0)
    .addProxyEvent(
        'net.minecraftforge.event.entity.living.LivingHealEvent',
        (event, owner) => {
            if (event.entity == owner) {
                event.amount *= 1.20
            }
        }
    )
    .build()

StartupEvents.registry('item', event => {
    event.create('custom_action_upgrade', 'payday:phone_upgrade')
        .series('custom')
        .level(1)
        .effectKey('custom_action_upgrade')
        .action(customAction)
})
~~~

## 5. 推荐用法和限制

普通 KubeJS 脚本优先使用字符串属性 ID：

~~~js
event.create('custom_upgrade', 'payday:phone_upgrade')
    .attribute('minecraft:generic.max_health', 0.10, 'MULTIPLY_TOTAL')
~~~

以下重载需要 Java 对象、Supplier、Predicate 或 Class：

- effect(MobEffectInstance)
- immune(TagKey<DamageType>, ...)
- immune(Predicate<DamageSource>, ...)
- addProxyEvent(Class, ...)
