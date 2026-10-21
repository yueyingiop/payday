# TriggerHudManager

`TriggerHudManager` 是 PayDay 内置条件型触发 HUD 的刷新器，已经注册为 KubeJS 全局绑定。
普通脚本只需要使用 `CooldownHudManager.setTriggerActive(...)`，不需要手动调用它。

## 内置条件

PayDay 每 tick 为服务端玩家刷新以下 HUD：

- `LEADER1`：领队范围减伤、生命值低于最大生命值 50% 时的自身减伤。
- `LEADER3`：半径 16 格内其他玩家获得最大生命值加成。
- `LEADER5`：半径 16 格内玩家人数加成。
- `ROGUE2`：半径 8 格内存在其他玩家时的闪避加成。
- `THIEF5`：潜行移动时的移动速度加成。
- `INFILTRATOR1`：半径 8 格内至少有 3 个非友善生物时的减伤。
- `INFILTRATOR2`、`INFILTRATOR3`、`INFILTRATOR4`：半径 16 格内存在非友善生物时的减伤。
- `ANTISOCIAL1`：半径 8 格内至少有 3 个非友善生物时，近战渗透条件已满足。
- `ANTISOCIAL3`：半径 16 格内存在非友善生物时的减伤。

这些 HUD 使用 `payday_trigger_` 前缀管理，不会删除 KubeJS 创建的其他 HUD。条件失效后，
对应 HUD 会立即从客户端移除。
