package com.yueyingiop.payday.api.record;

import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 指向一个护甲饰品中的具体 ArmorDefinition。 */
public record ArmorTarget(ItemStack armorStack, ArmorCurioItem curio, List<ItemStack> upgrades, ArmorSlotItem armor, ItemStack stack, int definitionIndex) {
}
