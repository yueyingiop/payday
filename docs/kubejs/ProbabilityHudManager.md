# ProbabilityHudManager

`ProbabilityHudManager` 管理手机升级自动生成的概率型 CDHUD。它已经注册为 KubeJS 全局绑定，
只处理服务端玩家。

## `refresh(entity)`

根据玩家当前已装备的手机和手机升级重建自动 HUD。相同属性只生成一个 HUD，并直接读取玩家的
最终属性值；属性图标由 `Config` 中的 `probability_hud_attribute_icons` 白名单决定。

```js
// 手机或升级由其他脚本直接修改后，可以手动刷新。
ProbabilityHudManager.refresh(player)
```

普通情况下不需要手动调用，手机装备变化时 PayDay 会自动刷新。

## `clear(entity)`

移除玩家的全部自动概率型 HUD，并清理内部缓存：

```js
ProbabilityHudManager.clear(player)
```

## `forget(entity)`

玩家离开服务器时清除内部指纹缓存。通常由 PayDay 自动调用，不需要脚本手动调用。

## 属性图标白名单

配置格式为：

```text
属性ID.-图标资源路径
```

PayDay 内置属性图标表：

```text
payday:gui/cd_attribute
```

该纹理为 64x64，每个图标占 16x16，顺序为速度、近战、远程、爆头、闪避、减伤。
其他 Mod 的属性可以填写独立的 16x16 图标资源路径。

属性 HUD 默认不隐藏数字，而是使用原始属性值显示；按住 Tab 时切换到 `cd_text` 纹理并显示
属性名称。名称使用属性的翻译键。远程伤害属性在手持 TACZ 枪械时以枪械基础攻击力为默认值，
未手持枪械时以 0 为默认值，再应用该属性的修饰符。

## 自动显示内容

- `PhoneUpgradeAction.attribute(...)` 声明的白名单属性，每个属性只显示一次。
- 击杀掉落和经验掉落加成。
- 护盾值和护盾冷却加成。
- 没有对应属性的其他固定概率效果。

自动 HUD 使用 `payday_auto_probability_` 前缀管理，不会删除 KubeJS 通过
`CooldownHudManager` 创建的 HUD。
