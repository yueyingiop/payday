package com.yueyingiop.payday;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SUIT_UPGRADES;
    public static final ForgeConfigSpec.IntValue LIGHT_UPGRADES;
    public static final ForgeConfigSpec.IntValue HEAVY_UPGRADES;
    public static final ForgeConfigSpec.IntValue BLAST_UPGRADES;
    public static final ForgeConfigSpec.IntValue TACTICAL_UPGRADES;
    public static final ForgeConfigSpec.IntValue IRON_MAN_UPGRADES;
    public static final ForgeConfigSpec.DoubleValue IMPACT_ARMOR;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_ARMOR;
    public static final ForgeConfigSpec.DoubleValue RESISTANCE_ARMOR;
    public static final ForgeConfigSpec.DoubleValue IMPACT_RECOVERY;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_RECOVERY;
    public static final ForgeConfigSpec.DoubleValue RESISTANCE_RECOVERY;
    public static final ForgeConfigSpec.DoubleValue ARMOR_CHANGE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue EQUIPMENT_BOX_CHANGE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue EQUIPMENT_BOX_USE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue EXPOSURE_RISK_MIN;
    public static final ForgeConfigSpec.IntValue EXPOSURE_RISK_MAX;
    public static final ForgeConfigSpec.IntValue EXPOSURE_RISK_DEFAULT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXPOSURE_SPECIAL_ITEM_RISKS;

    public static final ForgeConfigSpec.BooleanValue COMPACT_HUD;
    public static final ForgeConfigSpec.DoubleValue EQUIPMENT_BOX_HUD_X_RATIO;
    public static final ForgeConfigSpec.DoubleValue EQUIPMENT_BOX_HUD_Y_RATIO;
    public static final ForgeConfigSpec.BooleanValue EQUIPMENT_BOX_HUD_ENABLED;
    public static final ForgeConfigSpec.DoubleValue EXPOSURE_RISK_HUD_X_RATIO;
    public static final ForgeConfigSpec.DoubleValue EXPOSURE_RISK_HUD_Y_RATIO;
    public static final ForgeConfigSpec.BooleanValue EXPOSURE_RISK_HUD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue EXPOSURE_RISK_HUD_POSITION_MIGRATED;

    public static final ForgeConfigSpec.ConfigValue<String> TURRET_AIM_DISPLAY_ITEM;
    public static final ForgeConfigSpec.DoubleValue TURRET_ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TURRET_ATTACK_RANGE;
    public static final ForgeConfigSpec.IntValue TURRET_YAW_ANGLE;
    public static final ForgeConfigSpec.IntValue TURRET_PITCH_DOWN_ANGLE;
    public static final ForgeConfigSpec.IntValue TURRET_PITCH_UP_ANGLE;
    public static final ForgeConfigSpec.DoubleValue TURRET_MAX_TEMPERATURE;
    public static final ForgeConfigSpec.IntValue TURRET_OVERLOAD_DESTROY_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue TURRET_ATTACK_INTERVAL_TICKS;

    static {
        BUILDER.push("ArmorCurio");
        SUIT_UPGRADES = BUILDER
            .comment("[两件西装]升级插槽数量")
            .defineInRange("suit_upgrades", 2, 0, 24);

        LIGHT_UPGRADES = BUILDER
            .comment("[轻型防弹背心]升级插槽数量")
            .defineInRange("light_vest_upgrades", 3, 0, 24);

        HEAVY_UPGRADES = BUILDER
            .comment("[重型防弹背心]升级插槽数量")
            .defineInRange("heavy_vest_upgrades", 4, 0, 24);
                
        BLAST_UPGRADES = BUILDER
            .comment("[防爆弹衣]升级插槽数量")
            .defineInRange("blast_vest_upgrades", 5, 0, 24);

        TACTICAL_UPGRADES = BUILDER
            .comment("[复合战术背心]升级插槽数量")
            .defineInRange("tactical_vest_upgrades", 6, 0, 24);

        IRON_MAN_UPGRADES = BUILDER
            .comment("[改良型复合重型战术背心]升级插槽数量")
            .defineInRange("iron_man_upgrades", 7, 0, 24);
        BUILDER.pop();

        BUILDER.push("ArmorSolt");
        IMPACT_ARMOR = BUILDER
            .comment("[冲击插槽]护甲值")
            .defineInRange("impact_armor", 25.0D, 0.0D, 100000.0D);

        ADAPTIVE_ARMOR = BUILDER
            .comment("[自适应插槽]护甲值")
            .defineInRange("adaptive_armor", 50.0D, 0.0D, 100000.0D);

        RESISTANCE_ARMOR = BUILDER
            .comment("[抗性插槽]护甲值")
            .defineInRange("resistance_armor", 75.0D, 0.0D, 100000.0D);

        IMPACT_RECOVERY = BUILDER
            .comment("[冲击插槽]护甲恢复时间(秒)")
            .defineInRange("impact_recovery_seconds", 1.75D, 0.0D, 3600.0D);

        ADAPTIVE_RECOVERY = BUILDER
            .comment("[自适应插槽]护甲恢复时间(秒)")
            .defineInRange("adaptive_recovery_seconds", 1.5D, 0.0D, 3600.0D);

        RESISTANCE_RECOVERY = BUILDER
            .comment("[抗性插槽]护甲恢复时间(秒)")
            .defineInRange("resistance_recovery_seconds", 1.25D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("Other");
        ARMOR_CHANGE_COOLDOWN_SECONDS = BUILDER
            .comment("更换护甲饰品的冷却时间(秒)")
            .defineInRange("armor_change_cooldown_seconds", 360.0D, 0.0D, 86400.0D);

        EQUIPMENT_BOX_CHANGE_COOLDOWN_SECONDS = BUILDER
            .comment("更换装备箱的冷却时间(秒)")
            .defineInRange("equipment_box_change_cooldown_seconds", 360.0D, 0.0D, 86400.0D);

        CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS = BUILDER
            .comment("更换衣物箱的冷却时间(秒)")
            .defineInRange("clothes_box_change_cooldown_seconds", 30.0D, 0.0D, 86400.0D);

        EQUIPMENT_BOX_USE_COOLDOWN_SECONDS = BUILDER
            .comment("使用装备箱的冷却时间(秒)")
            .defineInRange("equipment_box_use_cooldown_seconds", 1.0D, 0.0D, 3600.0D);
        
        EXPOSURE_RISK_MIN = BUILDER
            .comment("最小暴露风险值")
            .defineInRange("exposure_risk_min", 3, 0, 99);

        EXPOSURE_RISK_MAX = BUILDER
            .comment("最大暴露风险值")
            .defineInRange("exposure_risk_max", 75, 0, 99);

        EXPOSURE_RISK_DEFAULT = BUILDER
            .comment("默认暴露风险值")
            .defineInRange("exposure_risk_default", 30, 0, 99);
        
        EXPOSURE_SPECIAL_ITEM_RISKS = BUILDER
            .comment("特殊物品风险值。格式：物品ID=值 或 #物品标签=值。此类物品不计入普通堆叠/NBT风险值。")
            .comment("示例：minecraft:diamond_sword=2.0, #minecraft:planks=0.5")
            .defineListAllowEmpty(
                List.of("exposure_special_item_risks"),
                List::of, 
                value -> value instanceof String
            );
        BUILDER.pop();

        CLIENT_BUILDER.push("HUD");
        COMPACT_HUD = CLIENT_BUILDER
            .comment("是否启用紧凑型 HUD")
            .define("compact_hud", false);

        EQUIPMENT_BOX_HUD_X_RATIO = CLIENT_BUILDER
            .comment("装备箱 HUD 左侧位置比例")
            .defineInRange("equipment_box_hud_x_ratio", 0.025D, 0.0D, 1.0D);

        EQUIPMENT_BOX_HUD_Y_RATIO = CLIENT_BUILDER
            .comment("装备箱 HUD 顶部位置比例")
            .defineInRange("equipment_box_hud_y_ratio", 0.95D, 0.0D, 1.0D);

        EQUIPMENT_BOX_HUD_ENABLED = CLIENT_BUILDER
            .comment("是否启用装备箱 HUD")
            .define("equipment_box_hud_enabled", true);
                
        EXPOSURE_RISK_HUD_X_RATIO = CLIENT_BUILDER
            .comment("暴露风险 HUD 左侧位置比例")
            .defineInRange("exposure_risk_hud_x_ratio", 0.25D, 0.0D, 1.0D);

        EXPOSURE_RISK_HUD_Y_RATIO = CLIENT_BUILDER
            .comment("暴露风险 HUD 顶部位置比例")
            .defineInRange("exposure_risk_hud_y_ratio", 0.5D, 0.0D, 1.0D);

        EXPOSURE_RISK_HUD_ENABLED = CLIENT_BUILDER
            .comment("是否启用暴露风险 HUD")
            .define("exposure_risk_hud_enabled", true);

        EXPOSURE_RISK_HUD_POSITION_MIGRATED = CLIENT_BUILDER
            .comment("标记暴露风险 HUD 是否已经按默认物品栏左侧位置初始化")
            .define("exposure_risk_hud_position_migrated", false);
        CLIENT_BUILDER.pop();

        BUILDER.push("Turret");
        TURRET_ATTACK_DAMAGE = BUILDER
            .comment("炮台基础攻击伤害")
            .defineInRange("attack_damage", 10.0D, 0.0D, 100000.0D);

        TURRET_ATTACK_RANGE = BUILDER
            .comment("炮台攻击范围(方块)")
            .defineInRange("attack_range", 16.0D, 1.0D, 256.0D);

        TURRET_YAW_ANGLE = BUILDER
            .comment("炮台左右偏航角(度),左右对称")
            .defineInRange("yaw_angle", 7, 1, 180);

        TURRET_PITCH_DOWN_ANGLE = BUILDER
            .comment("炮台最大俯角(度)")
            .defineInRange("pitch_down_angle", 30, 1, 90);

        TURRET_PITCH_UP_ANGLE = BUILDER
            .comment("炮台最大仰角(度)")
            .defineInRange("pitch_up_angle", 60, 1, 90);

        TURRET_MAX_TEMPERATURE = BUILDER
            .comment("炮台默认最大温度")
            .defineInRange("max_temperature", 100.0D, 1.0D, 100000.0D);

        TURRET_OVERLOAD_DESTROY_TIME_SECONDS = BUILDER
            .comment("炮台过载后自然销毁时间(秒)")
            .defineInRange("overload_destroy_time_seconds", 1800, 0, 604800);

        TURRET_ATTACK_INTERVAL_TICKS = BUILDER
            .comment("炮台攻击间隔(游戏刻，20游戏刻=1秒)")
            .defineInRange("turret_attack_interval_ticks", 10, 1, 1200);
        BUILDER.pop();

        CLIENT_BUILDER.push("Turret");
        TURRET_AIM_DISPLAY_ITEM = CLIENT_BUILDER
            .comment("手持该物品时显示炮台判断区域，填写物品ID")
            .define("aim_display_item", "minecraft:stick");
        CLIENT_BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    /**
     * 注册对应内容。
     */
    public static void register() {
    }

    /**
     * 获取升级插槽数量。
     * @param id 标识。
     * @return 对应的执行结果。
     */
    public static int upgrades(String id) {
        return switch (id) {
            case "suit" -> SUIT_UPGRADES.get();
            case "light_vest", "light_bulletproof_vest" -> LIGHT_UPGRADES.get();
            case "heavy_vest", "heavy_bulletproof_vest" -> HEAVY_UPGRADES.get();
            case "blast_vest" -> BLAST_UPGRADES.get();
            case "tactical_vest", "composite_tactical_vest" -> TACTICAL_UPGRADES.get();
            default -> IRON_MAN_UPGRADES.get();
        };
    }

}
