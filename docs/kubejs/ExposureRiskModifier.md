# ExposureRiskModifier

`ExposureRiskModifier` 用于从 KubeJS 修改玩家的暴露风险。它已经注册为 KubeJS 全局绑定，
可以在 `server_scripts` 中直接使用：

```js
ExposureRiskModifier.addPermanent(player, 5.0)
```

暴露风险的最终值还会受到物品数量、物品 NBT、特殊风险配置、手机升级属性以及模组配置中的
最小值和最大值影响。修改器只是其中一项加法修正：正数提高风险，负数降低风险。

## 方法

### `setTemporary(stack, value)`

为一个 `ItemStack` 设置临时修正，并返回同一个物品堆。

```js
const modifiedStack = ExposureRiskModifier.setTemporary(stack, -8.0)
```

临时修正写入物品堆 NBT，键名为 `payday:exposure_modifier`。只要物品堆位于玩家的物品栏、
盔甲栏、副手或 Curios 槽位中，修正就会计入暴露风险；物品离开这些位置后不再计入。
再次调用会覆盖该物品堆原有的临时修正。

### `addPermanent(player, value)`

给玩家增加永久修正：

```js
ExposureRiskModifier.addPermanent(player, 5.0)
```

永久修正保存在玩家持久数据中，适合任务奖励、剧情阶段或一次性解锁。数值可以为负数。
该方法不会自动防止重复执行，因此重复触发事件会重复累加。

### `getPermanent(player)`

读取玩家当前的永久修正总值：

```js
const value = ExposureRiskModifier.getPermanent(player)
```

返回值只包含永久修正，不包含物品临时修正或暴露风险的其他组成部分。

### `setPermanent(player, value)`

直接替换玩家的永久修正总值：

```js
ExposureRiskModifier.setPermanent(player, 0.0)
```

这不会修改物品堆上的临时修正，也不会直接设置玩家的最终暴露风险。

## 调用位置

- `addPermanent`、`getPermanent` 和 `setPermanent` 应在服务端事件中调用。
- `setTemporary` 应对实际持有或装备的 `ItemStack` 调用；只修改一个新建但未放入玩家库存的物品堆不会影响玩家。
- 暴露风险最终会按模组配置的最小值和最大值限制，修正值不保证能让最终风险超出该范围。

## 示例

两个可直接复制到 `server_scripts` 的示例见：

[`payday_exposure_risk_example.js`](payday_exposure_risk_example.js)

