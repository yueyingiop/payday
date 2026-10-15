package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.ArmorBoxItem;
import com.yueyingiop.payday.item.AmmoBagItem;
import com.yueyingiop.payday.item.AmmoBoxItem;
import com.yueyingiop.payday.item.ClothesBoxItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import com.yueyingiop.payday.item.MedicalBoxItem;
import com.yueyingiop.payday.item.PhoneItem;
import com.yueyingiop.payday.item.PhoneUpgradeItem;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.item.TestEffectStickItem;
import com.yueyingiop.payday.item.TurretItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class RegistryItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, PayDay.MODID);

    public static final RegistryObject<Item> SUIT = ITEMS.register("suit", () -> armor(2, 0.15D));
    public static final RegistryObject<Item> LIGHT_VEST = ITEMS.register("light_bulletproof_vest",
            () -> armor(3, 0.05D));
    public static final RegistryObject<Item> HEAVY_VEST = ITEMS.register("heavy_bulletproof_vest",
            () -> armor(4, -0.05D));
    public static final RegistryObject<Item> BLAST_VEST = ITEMS.register("blast_vest", () -> armor(5, -0.15D));
    public static final RegistryObject<Item> TACTICAL_VEST = ITEMS.register("composite_tactical_vest",
            () -> armor(6, -0.25D));
    public static final RegistryObject<Item> IRON_MAN = ITEMS.register("improved_composite_heavy_tactical_vest",
            () -> armor(7, -0.35D));

    public static final RegistryObject<Item> IMPACT_SLOT = ITEMS.register("impact_slot",
            () -> new ArmorSlotItem("impact_slot"));
    public static final RegistryObject<Item> ADAPTIVE_SLOT = ITEMS.register("adaptive_slot",
            () -> new ArmorSlotItem("adaptive_slot"));
    public static final RegistryObject<Item> RESISTANCE_SLOT = ITEMS.register("resistance_slot",
            () -> new ArmorSlotItem("resistance_slot"));
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlateItem::new);
    public static final RegistryObject<Item> AMMO_BAG = ITEMS.register("ammo_bag", AmmoBagItem::new);
    public static final RegistryObject<Item> FIRSTAID_KIT = ITEMS.register("firstaid_kit", FirstAidKitItem::new);
    public static final RegistryObject<Item> TURRET = ITEMS.register("turret",
            () -> new TurretItem((com.yueyingiop.payday.block.TurretBlock) RegistryBlocks.TURRET.get(),
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ARMOR_BOX = ITEMS.register("armor_box", ArmorBoxItem::new);
    public static final RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static final RegistryObject<Item> FIRST_AID_BOX = ITEMS.register("first_aid_box", MedicalBoxItem::new);
    public static final RegistryObject<Item> CLOTHES_BOX = ITEMS.register("clothes_box", ClothesBoxItem::new);
    public static final RegistryObject<Item> PHONE = ITEMS.register("phone", PhoneItem::new);
    public static final RegistryObject<Item> TEST_EFFECT_STICK = ITEMS.register("test_effect_stick",
            TestEffectStickItem::new);

    // Basic upgrades: all are repeatable and may be installed multiple times.
    public static final RegistryObject<Item> BASIC_ATTACK_1 = basic("basic_attack_1", "attack", 1);
    public static final RegistryObject<Item> BASIC_ATTACK_2 = basic("basic_attack_2", "attack", 2);
    public static final RegistryObject<Item> BASIC_ATTACK_3 = basic("basic_attack_3", "attack", 3);
    public static final RegistryObject<Item> BASIC_ATTACK_4 = basic("basic_attack_4", "attack", 4);
    public static final RegistryObject<Item> BASIC_RANGED_1 = basic("basic_ranged_1", "ranged", 1);
    public static final RegistryObject<Item> BASIC_RANGED_2 = basic("basic_ranged_2", "ranged", 2);
    public static final RegistryObject<Item> BASIC_RANGED_3 = basic("basic_ranged_3", "ranged", 3);
    public static final RegistryObject<Item> BASIC_RANGED_4 = basic("basic_ranged_4", "ranged", 4);
    public static final RegistryObject<Item> BASIC_ARMOR_1 = basic("basic_armor_1", "armor", 1);
    public static final RegistryObject<Item> BASIC_ARMOR_2 = basic("basic_armor_2", "armor", 2);
    public static final RegistryObject<Item> BASIC_ARMOR_3 = basic("basic_armor_3", "armor", 3);
    public static final RegistryObject<Item> BASIC_ARMOR_4 = basic("basic_armor_4", "armor", 4);
    public static final RegistryObject<Item> BASIC_HEALTH_1 = basic("basic_health_1", "health", 1);
    public static final RegistryObject<Item> BASIC_HEALTH_2 = basic("basic_health_2", "health", 2);
    public static final RegistryObject<Item> BASIC_HEALTH_3 = basic("basic_health_3", "health", 3);
    public static final RegistryObject<Item> BASIC_HEALTH_4 = basic("basic_health_4", "health", 4);
    public static final RegistryObject<Item> BASIC_SPEED_1 = basic("basic_speed_1", "speed", 1);
    public static final RegistryObject<Item> BASIC_SPEED_2 = basic("basic_speed_2", "speed", 2);
    public static final RegistryObject<Item> BASIC_SPEED_3 = basic("basic_speed_3", "speed", 3);
    public static final RegistryObject<Item> BASIC_SPEED_4 = basic("basic_speed_4", "speed", 4);

    // Special upgrades. Names and levels are stable IDs; detailed effects can be
    // added without renaming items.
    public static final RegistryObject<Item> SPECIAL_HELMET_DANCE = special("special_helmet_dance", "misc", 1, PhoneUpgradeConfig.MISC_HELMET_DANCE);
    public static final RegistryObject<Item> SPECIAL_FOREST = special("special_forest", "misc", 1, PhoneUpgradeConfig.MISC_FOREST);
    public static final RegistryObject<Item> SPECIAL_WARDROBE = special("special_wardrobe", "misc", 1, PhoneUpgradeConfig.MISC_WARDROBE);
    public static final RegistryObject<Item> SPECIAL_LAST_STAND = special("special_last_stand", "misc", 1, PhoneUpgradeConfig.MISC_LAST_STAND);
    public static final RegistryObject<Item> SPECIAL_MUSCLE_1 = special("special_muscle_1", "muscle", 1, PhoneUpgradeConfig.MUSCLE1);
    public static final RegistryObject<Item> SPECIAL_MUSCLE_2 = special("special_muscle_2", "muscle", 2, PhoneUpgradeConfig.MUSCLE2);
    public static final RegistryObject<Item> SPECIAL_MUSCLE_3 = special("special_muscle_3", "muscle", 3, PhoneUpgradeConfig.MUSCLE3);
    public static final RegistryObject<Item> SPECIAL_MUSCLE_4 = special("special_muscle_4", "muscle", 4, PhoneUpgradeConfig.MUSCLE4);
    public static final RegistryObject<Item> SPECIAL_MUSCLE_5 = special("special_muscle_5", "muscle", 5, PhoneUpgradeConfig.MUSCLE5);
    public static final RegistryObject<Item> SPECIAL_LEADER_1 = special("special_leader_1", "leader", 1, PhoneUpgradeConfig.LEADER1);
    public static final RegistryObject<Item> SPECIAL_LEADER_2 = special("special_leader_2", "leader", 2, PhoneUpgradeConfig.LEADER2);
    public static final RegistryObject<Item> SPECIAL_LEADER_3 = special("special_leader_3", "leader", 3, PhoneUpgradeConfig.LEADER3);
    public static final RegistryObject<Item> SPECIAL_LEADER_4 = special("special_leader_4", "leader", 4, PhoneUpgradeConfig.LEADER4);
    public static final RegistryObject<Item> SPECIAL_LEADER_5 = special("special_leader_5", "leader", 5, PhoneUpgradeConfig.LEADER5);
    public static final RegistryObject<Item> SPECIAL_ARMOR_1 = special("special_armorer_1", "armorer", 1, PhoneUpgradeConfig.ARMORER1);
    public static final RegistryObject<Item> SPECIAL_ARMOR_2 = special("special_armorer_2", "armorer", 2, PhoneUpgradeConfig.ARMORER2);
    public static final RegistryObject<Item> SPECIAL_ARMOR_3 = special("special_armorer_3", "armorer", 3, PhoneUpgradeConfig.ARMORER3);
    public static final RegistryObject<Item> SPECIAL_ARMOR_4 = special("special_armorer_4", "armorer", 4, PhoneUpgradeConfig.ARMORER4);
    public static final RegistryObject<Item> SPECIAL_ARMOR_5 = special("special_armorer_5", "armorer", 5, PhoneUpgradeConfig.ARMORER5);
    public static final RegistryObject<Item> SPECIAL_ROGUE_1 = special("special_rogue_1", "rogue", 1, PhoneUpgradeConfig.ROGUE1);
    public static final RegistryObject<Item> SPECIAL_ROGUE_2 = special("special_rogue_2", "rogue", 2, PhoneUpgradeConfig.ROGUE2);
    public static final RegistryObject<Item> SPECIAL_ROGUE_3 = special("special_rogue_3", "rogue", 3, PhoneUpgradeConfig.ROGUE3);
    public static final RegistryObject<Item> SPECIAL_ROGUE_4 = special("special_rogue_4", "rogue", 4, PhoneUpgradeConfig.ROGUE4);
    public static final RegistryObject<Item> SPECIAL_ROGUE_5 = special("special_rogue_5", "rogue", 5, PhoneUpgradeConfig.ROGUE5);
    public static final RegistryObject<Item> SPECIAL_ASSASSIN_1 = special("special_assassin_1", "assassin", 1, PhoneUpgradeConfig.ASSASSIN1);
    public static final RegistryObject<Item> SPECIAL_ASSASSIN_2 = special("special_assassin_2", "assassin", 2, PhoneUpgradeConfig.ASSASSIN2);
    public static final RegistryObject<Item> SPECIAL_ASSASSIN_3 = special("special_assassin_3", "assassin", 3, PhoneUpgradeConfig.ASSASSIN3);
    public static final RegistryObject<Item> SPECIAL_ASSASSIN_4 = special("special_assassin_4", "assassin", 4, PhoneUpgradeConfig.ASSASSIN4);
    public static final RegistryObject<Item> SPECIAL_ASSASSIN_5 = special("special_assassin_5", "assassin", 5, PhoneUpgradeConfig.ASSASSIN5);
    public static final RegistryObject<Item> SPECIAL_THIEF_1 = special("special_thief_1", "thief", 1, PhoneUpgradeConfig.THIEF1);
    public static final RegistryObject<Item> SPECIAL_THIEF_2 = special("special_thief_2", "thief", 2, PhoneUpgradeConfig.THIEF2);
    public static final RegistryObject<Item> SPECIAL_THIEF_3 = special("special_thief_3", "thief", 3, PhoneUpgradeConfig.THIEF3);
    public static final RegistryObject<Item> SPECIAL_THIEF_4 = special("special_thief_4", "thief", 4, PhoneUpgradeConfig.THIEF4);
    public static final RegistryObject<Item> SPECIAL_THIEF_5 = special("special_thief_5", "thief", 5, PhoneUpgradeConfig.THIEF5);
    public static final RegistryObject<Item> SPECIAL_INFILT_1 = special("special_infiltrator_1", "infiltrator", 1, PhoneUpgradeConfig.INFILTRATOR1);
    public static final RegistryObject<Item> SPECIAL_INFILT_2 = special("special_infiltrator_2", "infiltrator", 2, PhoneUpgradeConfig.INFILTRATOR2);
    public static final RegistryObject<Item> SPECIAL_INFILT_3 = special("special_infiltrator_3", "infiltrator", 3, PhoneUpgradeConfig.INFILTRATOR3);
    public static final RegistryObject<Item> SPECIAL_INFILT_4 = special("special_infiltrator_4", "infiltrator", 4, PhoneUpgradeConfig.INFILTRATOR4);
    public static final RegistryObject<Item> SPECIAL_INFILT_5 = special("special_infiltrator_5", "infiltrator", 5, PhoneUpgradeConfig.INFILTRATOR5);
    public static final RegistryObject<Item> SPECIAL_ANTISOCIAL_1 = special("special_antisocial_1", "antisocial", 1, PhoneUpgradeConfig.ANTISOCIAL1);
    public static final RegistryObject<Item> SPECIAL_ANTISOCIAL_2 = special("special_antisocial_2", "antisocial", 2, PhoneUpgradeConfig.ANTISOCIAL2);
    public static final RegistryObject<Item> SPECIAL_ANTISOCIAL_3 = special("special_antisocial_3", "antisocial", 3, PhoneUpgradeConfig.ANTISOCIAL3);
    public static final RegistryObject<Item> SPECIAL_ANTISOCIAL_4 = special("special_antisocial_4", "antisocial", 4, PhoneUpgradeConfig.ANTISOCIAL4);
    public static final RegistryObject<Item> SPECIAL_ANTISOCIAL_5 = special("special_antisocial_5", "antisocial", 5, PhoneUpgradeConfig.ANTISOCIAL5);
    public static final RegistryObject<Item> SPECIAL_ANARCHY_1 = special("special_anarchist_1", "anarchist", 1, PhoneUpgradeConfig.ANARCHIST1);
    public static final RegistryObject<Item> SPECIAL_ANARCHY_2 = special("special_anarchist_2", "anarchist", 2, PhoneUpgradeConfig.ANARCHIST2);
    public static final RegistryObject<Item> SPECIAL_ANARCHY_3 = special("special_anarchist_3", "anarchist", 3, PhoneUpgradeConfig.ANARCHIST3);
    public static final RegistryObject<Item> SPECIAL_ANARCHY_4 = special("special_anarchist_4", "anarchist", 4, PhoneUpgradeConfig.ANARCHIST4);
    public static final RegistryObject<Item> SPECIAL_ANARCHY_5 = special("special_anarchist_5", "anarchist", 5, PhoneUpgradeConfig.ANARCHIST5);
    public static final RegistryObject<Item> SPECIAL_HACKER_1 = special("special_hacker_1", "hacker", 1, PhoneUpgradeConfig.HACKER1);
    public static final RegistryObject<Item> SPECIAL_HACKER_2 = special("special_hacker_2", "hacker", 2, PhoneUpgradeConfig.HACKER2);
    public static final RegistryObject<Item> SPECIAL_HACKER_3 = special("special_hacker_3", "hacker", 3, PhoneUpgradeConfig.HACKER3);
    public static final RegistryObject<Item> SPECIAL_HACKER_4 = special("special_hacker_4", "hacker", 4, PhoneUpgradeConfig.HACKER4);
    public static final RegistryObject<Item> SPECIAL_HACKER_5 = special("special_hacker_5", "hacker", 5, PhoneUpgradeConfig.HACKER5);

    private static RegistryObject<Item> basic(String id, String series, int level) {
        return ITEMS.register("phone_upgrade/" + id,
                () -> new PhoneUpgradeItem(PhoneUpgradeItem.Category.BASIC, series, level, false, id,
                        PhoneUpgradeConfig.basic(series, level)));
    }

    private static RegistryObject<Item> special(String id, String series, int level, PhoneUpgradeAction action) {
        return ITEMS.register("phone_upgrade/" + id,
                () -> new PhoneUpgradeItem(PhoneUpgradeItem.Category.SPECIAL, series, level, level > 1, id,
                        action));
    }

    /** 创建内置护甲饰品，并以 Java 定义其自身的移动速度修饰符。 */
    /**
     * 执行 <code>armor</code> 对应的操作。
     *
     * @param upgradeSlots  参数值。
     * @param movementSpeed 参数值。
     * @return 方法执行结果。
     */
    private static ArmorCurioItem armor(int upgradeSlots, double movementSpeed) {
        return new ArmorCurioItem(upgradeSlots,
                List.of(new ArmorCurioItem.AttributeDefinition(
                        Attributes.MOVEMENT_SPEED,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        movementSpeed)),
                new Item.Properties());
    }
}
