# TurretFluid 和 TurretFluidManager

`TurretFluid` 用于描述一种已经存在的 Forge 流体在炮台中的行为。
它不会创建或注册新的流体；水、岩浆以及其他模组或 KubeJS 已注册的流体都可以作为目标。

PayDay 已将 `TurretFluid` 和 `TurretFluidManager` 注册为 KubeJS 全局绑定，可以在
`startup_scripts` 中直接使用。

## 1. 注册一个炮台流体

最常用的写法是创建 Builder，设置流体 ID、类型和对应参数，最后调用 `register()`：

```js
StartupEvents.init(() => {
    TurretFluid.builder()
        .fluid('minecraft:water')
        .type('CONSUMABLE_COOLANT')
        .mbPerTemperature(50)
        .build()
        .register()
})
```

`type()` 也支持中文名称：

```js
TurretFluid.builder()
    .fluid('minecraft:water')
    .type('消耗型冷却液')
    .mbPerTemperature(50)
    .build()
    .register()
```

如果需要覆盖已有定义，使用相同的流体 ID 再次注册即可。

## 2. 流体类型

### 2.1 消耗型冷却液

消耗型冷却液会在炮台需要降温时消耗流体。每消耗指定数量的流体，抵消 1 点温度。

```js
TurretFluid.builder()
    .fluid('minecraft:water')
    .type('CONSUMABLE_COOLANT')
    .mbPerTemperature(50) // 每50 mB抵消1点温度
    .build()
    .register()
```

也可以直接设置每 1 mB 抵消的温度：

```js
TurretFluid.builder()
    .fluid('kjs:strong_coolant')
    .type('CONSUMABLE_COOLANT')
    .temperaturePerMb(0.1) // 每1 mB抵消0.1点温度
    .build()
    .register()
```

可用参数：

- `fluid(id)`：流体注册表 ID。
- `type('CONSUMABLE_COOLANT')`：设置为消耗型冷却液。
- `mbPerTemperature(amount)`：每多少 mB 抵消 1 点温度。
- `temperaturePerMb(amount)`：每 1 mB 抵消的温度。

### 2.2 循环型冷却液

循环型冷却液不会消耗流体。炮台每隔指定 tick 数按照以下公式降低温度：

```text
减少温度 = coolingCoefficient * 当前流体量(mB)
```

```js
TurretFluid.builder()
    .fluid('kjs:circulating_coolant')
    .type('CIRCULATING_COOLANT')
    .coolingCoefficient(0.002)
    .coolingInterval(20) // 每20 tick处理一次
    .build()
    .register()
```

可用参数：

- `coolingCoefficient(value)`：降温系数。
- `coolingInterval(ticks)`：降温处理间隔，单位为 tick。

### 2.3 过载液

过载液会提高炮台的最大温度，并按照固定间隔消耗指定数量的流体。

```js
TurretFluid.builder()
    .fluid('minecraft:lava')
    .type('OVERLOAD')
    .maximumTemperatureBonus(40)
    .consumptionInterval(20) // 每20 tick消耗一次
    .consumptionAmount(1) // 每次消耗1 mB
    .build()
    .register()
```

可用参数：

- `maximumTemperatureBonus(value)`：提供的最大温度上限加成。
- `consumptionInterval(ticks)`：消耗间隔，单位为 tick。
- `consumptionAmount(amountMb)`：每次消耗的流体量。

## 3. TurretFluidManager

### `TurretFluidManager.register(fluid)`

注册一个已经构建好的 `TurretFluid`：

```js
const coolant = TurretFluid.builder()
    .fluid('kjs:coolant')
    .type('CONSUMABLE_COOLANT')
    .mbPerTemperature(25)
    .build()

TurretFluidManager.register(coolant)
```

`TurretFluid.register()` 是上面写法的链式简写。

### `TurretFluidManager.register(fluidId, fluid)`

使用流体 ID 和定义注册。两个 ID 必须一致：

```js
const overload = TurretFluid.builder()
    .fluid('minecraft:lava')
    .type('OVERLOAD')
    .maximumTemperatureBonus(40)
    .build()

TurretFluidManager.register('minecraft:lava', overload)
```

### `TurretFluidManager.get(fluidId)`

获取指定流体的炮台行为定义。没有注册时返回 `null`：

```js
const definition = TurretFluidManager.get('minecraft:water')
if (definition != null) {
    console.log(definition.type())
}
```

### `TurretFluidManager.all()`

获取所有已注册定义的只读列表。列表按流体 ID 排序，适合调试或生成说明：

```js
TurretFluidManager.all().forEach(definition => {
    console.log(definition.fluidId() + ': ' + definition.type())
})
```

## 4. 注意事项

- `fluid(id)` 必须指向已经存在的 Forge 流体。
- 一个流体只能有一个炮台行为定义；重复注册会覆盖旧定义。
- 炮台冷却液槽只能接受冷却型流体，过载液槽只能接受 `OVERLOAD` 流体。
- 默认定义由 PayDay 自动注册：水为每 50 mB 抵消 1 点温度，岩浆提供 40 点最大温度加成。
- JEI 会自动为所有已注册且存在的流体显示流体纹理、类型和效果参数。
