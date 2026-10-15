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
- `PROBABILITY`：概率型，永久显示一个百分比数值。
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

### `value(value)`

- `TIMER`：冷却时间，单位为秒，最大显示值为 `999.9`。
- `PROBABILITY`：百分比数值，范围为 `0` 到 `999`。
- `TRIGGER`：该值会被忽略。

### `showName(name)`

设置概率型 HUD 按住 Tab 时显示的属性名称。名称绘制在 40x22 概率型 HUD 的右下角，
只在按住 Tab 时显示。

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
- 触发型 HUD 固定显示 5 秒，`value()` 不生效。
- HUD 的图标资源应使用已经存在的客户端纹理。
