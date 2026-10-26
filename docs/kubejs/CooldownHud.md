# CooldownHud 和 CooldownHudManager

`CooldownHud` 描述屏幕左上角显示的一个冷却 HUD，`CooldownHudManager` 负责在服务端
创建、更新、移除并同步 HUD。

PayDay 已将两个类注册为 KubeJS 全局绑定。HUD 的创建和触发逻辑应放在服务端事件中，
这样所有冷却状态都以服务端时间为准。

## 1. 创建 CooldownHud

使用 Builder 创建 HUD：

```js
const hud = CooldownHud.builder()
    .type('TIMER')
    .texture('payday:item/phone_upgrade/generated/special_muscle_5')
    .value(5.0)
    .build()
```

### `type(type)`

HUD 有三种类型：

- `TIMER`：计时型，显示剩余秒数和竖直进度条。
- `PROBABILITY`：概率型，永久显示一个数值；默认按百分比数值处理。
- `TRIGGER`：触发型，触发后固定显示 5 秒，不使用 `value()`。

也支持字符串别名，例如 `timer`、`probability`、`trigger`，以及对应中文名称。

### `texture(texture)`

设置 HUD 左侧 16x16 图标。推荐使用资源路径简写：

```js
.texture('payday:item/phone_upgrade/generated/special_muscle_5')
```

也可以使用完整的资源路径：

```js
.texture('payday:textures/item/phone_upgrade/generated/special_muscle_5.png')
```

不调用 `texture()` 或传入空字符串时，不显示图标。

### `texturePosition(u, v)`

设置 16x16 图标在纹理表中的左上角坐标。普通单图标纹理不需要调用；属性 HUD 使用
PayDay 内置的 `payday:gui/cd_attribute` 纹理表时，可以用它选择图标分块。

### `value(value)`

- `TIMER`：冷却时间，单位为秒，最大显示值为 `999.9`。
- `PROBABILITY`：普通整数显示范围为 `-999` 到 `999`；原始数值或带百分号的万分比
  显示范围为 `-999999` 到 `999999`。
- `TRIGGER`：该值会被忽略。

当调用 `.percentage(false)` 时，`PROBABILITY` 会显示原始数值，不进行百分比格式处理，
例如 `0.15` 会显示为 `0.15`。

### `percentage(percentage)`

设置概率型 HUD 的数字是否按整数格式绘制。默认值为 `true`；属性原始值需要保留小数时，
可以设置为 `false`。

### `showPercentage(showPercentage)`

设置概率型 HUD 是否在数字后绘制百分号。启用后，数值按万分比解释，`1000` 会显示为
`10%`，百分号使用 `cd.png` 中的百分号纹理。启用后会自动允许万分比数值范围，
也可以同时关闭 `percentage()` 的整数格式：

```js
CooldownHud.builder()
    .type('PROBABILITY')
    .value(1000)
    .percentage(false)
    .showPercentage(true)
    .build()
```

属性汇总 HUD 使用：

```js
CooldownHud.builder()
    .type('PROBABILITY')
    .texture('payday:gui/cd_attribute')
    .texturePosition(0, 16)
    .value(1500)
    .percentage(false)
    .showPercentage(true)
    .showName(Component.translatable('attribute.name.payday.dodge_chance'))
    .build()
```

### `showValue(showValue)`

控制是否绘制右侧数字，默认值为 `true`。需要显示属性原始值时使用
`.percentage(false)`；万分比属性使用 `.showPercentage(true)` 显示百分号。

### `showName(name)`

设置概率型 HUD 按住 Tab 时显示的属性名称。按住 Tab 后 HUD 会切换到 `cd_text` 背景，
名称绘制在中间 34x16 区域，并使用 1px 字间距。

推荐使用 `Component.translatable()`，这样名称会跟随 lang 文件切换：

```js
CooldownHud.builder()
    .type('PROBABILITY')
    .texture('payday:item/phone.png')
    .value(15)
    .showName(Component.translatable('attribute.name.payday.dodge_chance'))
    .build()
```

也可以传入普通字符串：

```js
.showName('闪避几率')
```

如果没有调用 `showName()`，名称默认为翻译键 `cooldown_hud.payday.unknown`，中文显示为“未知”。

## 2. CooldownHudManager

### `startTimer(player, id, hud)`

启动一个计时型 HUD。相同玩家使用相同 ID 时会覆盖旧 HUD，并从当前时间重新开始：

```js
CooldownHudManager.startTimer(player, 'payday_example_timer',
    CooldownHud.builder()
        .type('TIMER')
        .texture('payday:item/phone.png')
        .value(10.0)
        .build())
```

### `showProbability(player, id, hud)`

显示或更新一个永久存在的概率型 HUD：

```js
CooldownHudManager.showProbability(player, 'payday_example_chance',
    CooldownHud.builder()
        .type('PROBABILITY')
        .texture('payday:item/phone_upgrade/generated/special_rogue_1')
        .value(15)
        .build())
```

如果效果不再存在，应主动移除：

```js
CooldownHudManager.remove(player, 'payday_example_chance')
```

### `trigger(player, id, hud)`

触发一个显示 5 秒的 HUD：

```js
CooldownHudManager.trigger(player, 'payday_example_trigger',
    CooldownHud.builder()
        .type('TRIGGER')
        .texture('payday:item/phone_upgrade/generated/special_hacker_1')
        .build())
```

再次使用相同 ID 会重新计时 5 秒。

### `setTriggerActive(player, id, hud, active)`

按条件控制一个持续存在的触发型 HUD。`active` 为 `true` 时 HUD 一直显示，变为
`false` 时立即移除。它适合范围判定、生命值阈值和潜行状态等持续条件：

```js
function refreshNearbyPlayerHud(player) {
    const active = player.level.getEntitiesOfClass(Player, player.boundingBox.inflate(8))
        .some(other => other !== player && !other.isSpectator())

    CooldownHudManager.setTriggerActive(player, 'kjs_nearby_player',
        active ? CooldownHud.builder()
            .type('TRIGGER')
            .texture('payday:item/phone_upgrade/generated/special_rogue_2')
            .showName('附近玩家')
            .build() : null,
        active)
}
```

条件 HUD 使用同一个 ID 时只会在状态或 HUD 内容改变时同步一次，不会因为每 tick 刷新而
重复发送相同数据包。`setTriggerActive` 只接受 `TRIGGER` 类型；条件为 `false` 时 HUD
参数可以传入 `null`。

### `show(player, id, hud)`

通用入口。建议优先使用与类型匹配的 `startTimer`、`showProbability` 或 `trigger`，
因为这些方法会检查 HUD 类型：

```js
CooldownHudManager.show(player, 'payday_example', hud)
```

### `remove(player, id)` 和 `clear(player)`

移除指定 HUD 或移除玩家的全部 HUD：

```js
CooldownHudManager.remove(player, 'payday_example_timer')
CooldownHudManager.clear(player)
```

## 3. 完整事件示例

下面的例子在玩家受到伤害后触发 5 秒 HUD：

```js
PlayerEvents.hurt(event => {
    const player = event.player
    CooldownHudManager.trigger(player, 'kjs_damage_notice',
        CooldownHud.builder()
            .type('TRIGGER')
            .texture('payday:item/phone.png')
            .build())
})
```

## 4. 注意事项

- `player` 必须是服务端玩家；客户端调用不会创建服务端 HUD。
- `id` 是每个玩家内部的唯一键，应使用稳定且具有命名空间的字符串。
- 计时型 HUD 的 `value` 使用秒，Manager 会转换为游戏 tick。
- 概率型 HUD 不会自动过期，效果结束时必须调用 `remove()`。
- `trigger()` 创建的触发型 HUD 固定显示 5 秒，`value()` 不生效。
- `setTriggerActive()` 创建的条件型触发 HUD 不自动过期，必须在条件失效时传入 `false`。
- HUD 的图标资源应使用已经存在的客户端纹理。

## 5. 手机升级自动概率 HUD

手机装备或升级变化后，PayDay 会自动重建内置概率型 HUD。自动 HUD 使用手机升级物品作为
图标；按住 Tab 时会切换到文本背景并显示属性名称。

属性 HUD 直接读取玩家当前属性最终值。爆头伤害、闪避率和伤害减免使用万分比显示，
例如 `2500` 显示为 `25%`。内置属性图标来自
`payday:gui/cd_attribute`，每格大小为 16x16，顺序为：速度、近战、远程、爆头、闪避、减伤。

以下内容会自动显示：

- `PhoneUpgradeAction.attribute(...)` 声明的属性；名称使用属性自己的翻译键。
- 相同属性只显示一个，并直接读取玩家当前的最终属性值。
- 远程攻击力在最终值为 `0` 时不显示。

击杀掉落、经验掉落、护盾值、护盾冷却、初始伤害和生命恢复速度不再自动显示为概率型 HUD。

自动 HUD 使用内部 ID 前缀管理，不会删除 KubeJS 通过 `CooldownHudManager` 创建的 HUD。
