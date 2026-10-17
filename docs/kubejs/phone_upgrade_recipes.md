# 手机升级配方

PayDay 将手机升级配方生成到 TACZ 附件工作台的配方类型
`tacz:gun_smith_table_crafting` 中。内置配方由 Java 生成器批量生成，不需要手写几十个 JSON 文件。

## 配方规则

- 一级基础升级：铁锭、皮革、红石各 1 个。
- 二级及以上基础升级：前一级升级 2 个、粘液球 1 个。
- 一级特殊升级：铁锭 1 个、皮革 1 个、红石 1 个、粘液球 2 个。
- 二级及以上特殊升级：前一级升级 2 个、岩浆膏 1 个、蜜脾 1 个。

所有内置结果都归入 `payday:phone_upgrades`，对应 TACZ 标签页“手机升级”。

PayDay 还会生成以下配方：

- 弹药箱：原版合成台配方，图案为 `ABA / ACA / ABA`。
- 手机：原版合成台配方，图案为 `ABC / DEF / GHI`。
- 弹药包：TACZ 附件工作台配方，鳞甲 1 个、黄绿色染料 1 个、紫水晶碎片 4 个。

弹药箱和手机使用 `minecraft:crafting_shaped`，会保留 3×3 图案位置。

## KubeJS API

KubeJS 脚本可以直接使用绑定的 `PhoneUpgradeRecipeGenerator`：

```js
ServerEvents.recipes(event => {
    event.custom(PhoneUpgradeRecipeGenerator.basicLevelOne(
        'kubejs:phone_upgrade/basic_example_1'
    ))

    event.custom(PhoneUpgradeRecipeGenerator.specialUpgrade(
        'kubejs:phone_upgrade/special_example_1',
        'kubejs:phone_upgrade/special_example_2'
    ))
})
```

### `basicLevelOne(outputItemId)`

生成一级基础升级配方。

- `outputItemId`：结果物品的完整 ID，例如 `kubejs:phone_upgrade/basic_example_1`。
- 材料固定为：1 个铁锭、1 个皮革、1 个红石。
- 返回一个可以直接传给 `event.custom(...)` 的 JSON 对象。

```js
event.custom(PhoneUpgradeRecipeGenerator.basicLevelOne(
    'kubejs:phone_upgrade/basic_example_1'
))
```

### `basicUpgrade(previousItemId, outputItemId)`

生成二级及以上基础升级配方。

- `previousItemId`：前一级基础升级的物品 ID。
- `outputItemId`：当前等级基础升级的物品 ID。
- 材料固定为：2 个前一级升级、1 个粘液球。

```js
event.custom(PhoneUpgradeRecipeGenerator.basicUpgrade(
    'kubejs:phone_upgrade/basic_example_1',
    'kubejs:phone_upgrade/basic_example_2'
))
```

### `specialLevelOne(outputItemId)`

生成一级特殊升级配方。

- `outputItemId`：结果物品的完整 ID。
- 材料固定为：1 个铁锭、1 个皮革、1 个红石、2 个粘液球。

```js
event.custom(PhoneUpgradeRecipeGenerator.specialLevelOne(
    'kubejs:phone_upgrade/special_example_1'
))
```

### `specialUpgrade(previousItemId, outputItemId)`

生成二级及以上特殊升级配方。

- `previousItemId`：前一级特殊升级的物品 ID。
- `outputItemId`：当前等级特殊升级的物品 ID。
- 材料固定为：2 个前一级升级、1 个岩浆膏、1 个蜜脾。

```js
event.custom(PhoneUpgradeRecipeGenerator.specialUpgrade(
    'kubejs:phone_upgrade/special_example_1',
    'kubejs:phone_upgrade/special_example_2'
))
```

### `material(itemId, count)`

构造一个 TACZ 配方材料对象，用于传给 `recipe`。

- `itemId`：材料物品的完整 ID，例如 `minecraft:iron_ingot`。
- `count`：材料数量，必须大于 0。
- 返回值不是完整配方，不能单独传给 `event.custom`。

### `recipe(outputItemId, ...materials)`

使用自定义材料构造完整 TACZ 附件工作台配方。

- `outputItemId`：结果物品的完整 ID。
- `...materials`：一个或多个由 `material(itemId, count)` 生成的材料对象。
- 返回值可以直接传给 `event.custom`。
- 生成器会自动设置配方类型为 `tacz:gun_smith_table_crafting`，并将结果归入
  `payday:phone_upgrades` 标签页。

```js
const customRecipe = PhoneUpgradeRecipeGenerator.recipe(
    'kubejs:phone_upgrade/custom_example',
    PhoneUpgradeRecipeGenerator.material('minecraft:gold_ingot', 2),
    PhoneUpgradeRecipeGenerator.material('minecraft:slime_ball', 1)
)

event.custom(customRecipe)
```

### `recipeInGroup(group, outputItemId, ...materials)`

在指定 TACZ 标签页中构造配方。

- `group`：标签页 ID，例如 `payday:misc` 或 `payday:phone_upgrades`。
- `outputItemId`：结果物品的完整 ID。
- `...materials`：由 `material(itemId, count)` 构造的材料。

```js
event.custom(PhoneUpgradeRecipeGenerator.recipeInGroup(
    'payday:misc',
    'kubejs:custom_box',
    PhoneUpgradeRecipeGenerator.material('minecraft:iron_ingot', 2)
))
```

### `miscRecipe(outputItemId, ...materials)`

`recipeInGroup('payday:misc', ...)` 的简写，用于将自定义配方放入 PayDay 杂项标签页。

## 标签页 API

玩家可以使用 `TaczTabRegistry.register(tabId, translationKey, iconItemId)`
注册新的 TACZ 附件工作台标签页。图标物品会在工作台索引建立时解析，因此可以在
`startup_scripts` 中注册。
