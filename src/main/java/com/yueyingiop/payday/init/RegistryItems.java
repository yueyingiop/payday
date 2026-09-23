package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.ArmorBoxItem;
import com.yueyingiop.payday.item.ClothesBoxItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import com.yueyingiop.payday.item.MedicalBoxItem;

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
    public static final RegistryObject<Item> LIGHT_VEST = ITEMS.register("light_bulletproof_vest", () -> armor(3, 0.05D));
    public static final RegistryObject<Item> HEAVY_VEST = ITEMS.register("heavy_bulletproof_vest", () -> armor(4, -0.05D));
    public static final RegistryObject<Item> BLAST_VEST = ITEMS.register("blast_vest", () -> armor(5, -0.15D));
    public static final RegistryObject<Item> TACTICAL_VEST = ITEMS.register("composite_tactical_vest", () -> armor(6, -0.25D));
    public static final RegistryObject<Item> IRON_MAN = ITEMS.register("improved_composite_heavy_tactical_vest", () -> armor(7, -0.35D));
    
    public static final RegistryObject<Item> IMPACT_SLOT = ITEMS.register("impact_slot", () -> new ArmorSlotItem("impact_slot"));
    public static final RegistryObject<Item> ADAPTIVE_SLOT = ITEMS.register("adaptive_slot", () -> new ArmorSlotItem("adaptive_slot"));
    public static final RegistryObject<Item> RESISTANCE_SLOT = ITEMS.register("resistance_slot", () -> new ArmorSlotItem("resistance_slot"));
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlateItem::new);
    public static final RegistryObject<Item> FIRSTAID_KIT = ITEMS.register("firstaid_kit", FirstAidKitItem::new);
    public static final RegistryObject<Item> ARMOR_BOX = ITEMS.register("armor_box", ArmorBoxItem::new);
    public static final RegistryObject<Item> FIRST_AID_BOX = ITEMS.register("first_aid_box", MedicalBoxItem::new);
    public static final RegistryObject<Item> CLOTHES_BOX = ITEMS.register("clothes_box", ClothesBoxItem::new);

    /** 创建内置护甲饰品，并以 Java 定义其自身的移动速度修饰符。 */
    /**
     * 执行 <code>armor</code> 对应的操作。
     * @param upgradeSlots 参数值。
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
