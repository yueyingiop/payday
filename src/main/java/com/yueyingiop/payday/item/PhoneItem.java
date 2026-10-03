package com.yueyingiop.payday.item;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import top.theillusivec4.curios.api.SlotContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.DistExecutor;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

/** Curios phone. Upgrade contents are stored directly on the equipped item. */
// @SuppressWarnings("removal")
public class PhoneItem extends Item implements ICurioItem {
    public static final String UPGRADES = "payday_phone_upgrades";
    public static final int BASIC_SLOTS = 18;
    public static final int SPECIAL_SLOTS = 9;
    public static final int SLOTS = BASIC_SLOTS + SPECIAL_SLOTS;

    public PhoneItem() { super(new Item.Properties().stacksTo(1)); }

    @Override
    public void curioTick(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity.level().isClientSide) {
            return;
        }
        if (!(stack.getItem() instanceof PhoneItem)) {
            return;
        }
        // 每 tick 重新应用手机内升级的效果，拆除升级后会在同一轮重算中移除旧效果。
        refreshEffects(livingEntity);
    }

    /**
     * Curios 新版回调：手机被卸下时立即清除属性和护甲插槽修饰。
     *
     * @param slotContext 手机所在的 Curios 槽位上下文
     * @param oldStack 被卸下的手机
     * @param newStack 替换进槽位的新物品
     */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack oldStack, ItemStack newStack) {
        clearEffects(slotContext.entity());
    }

    /**
     * Curios 旧版回调：兼容仍使用字符串槽位接口的调用路径。
     *
     * @param identifier 槽位标识
     * @param index 槽位索引
     * @param livingEntity 手机拥有者
     * @param stack 被卸下的手机
     */
    @Override
    public void onUnequip(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        clearEffects(livingEntity);
    }

    /**
     * 重新计算实体当前装备的所有手机升级效果。
     *
     * @param livingEntity 要刷新效果的实体
     */
    public static void refreshEffects(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide) {
            return;
        }

        PhoneUpgradeAction.clearAttributeModifiers(livingEntity);
        ArmorModifier.begin(livingEntity);
        for (CurioHelper.SlotStack slot : CurioHelper.slots(livingEntity, CurioHelper.PHONE_SLOT)) {
            if (!(slot.stack().getItem() instanceof PhoneItem phone)) {
                continue;
            }
            List<ItemStack> upgrades = phone.getUpgradeStacks(slot.stack());
            for (int i = 0; i < upgrades.size(); i++) {
                ItemStack upgradeStack = upgrades.get(i);
                if (upgradeStack.getItem() instanceof PhoneUpgradeItem upgrade) {
                    upgrade.action().apply(CurioHelper.PHONE_SLOT, i, livingEntity, upgradeStack, slot.stack());
                }
            }
        }
        // 只刷新最终弹匣容量，不因为装备扩容直接增加当前弹药。
        PhoneUpgradeAction.refreshAmmoExpansion(livingEntity);
    }

    /**
     * 清除实体当前由手机提供的效果。
     *
     * @param livingEntity 要清除效果的实体
     */
    public static void clearEffects(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide) {
            return;
        }
        PhoneUpgradeAction.clearAttributeModifiers(livingEntity);
        ArmorModifier.clear(livingEntity);
        PhoneUpgradeAction.refreshAmmoExpansion(livingEntity);
    }

    /** 重新计算实体当前手机提供的护甲插槽效果，供护甲事件读取。 */
    public static void refreshArmorSlotModifiers(LivingEntity livingEntity) {
        ArmorModifier.begin(livingEntity);
        for (ItemStack phoneStack : CurioHelper.stacks(livingEntity, CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgradeStack : phone.getUpgradeStacks(phoneStack)) {
                if (upgradeStack.getItem() instanceof PhoneUpgradeItem upgrade) {
                    upgrade.action().applyArmorSoltModifier(phoneStack, livingEntity);
                }
            }
        }
    }

    public List<ItemStack> getUpgradeStacks(ItemStack phone) {
        List<ItemStack> result = new ArrayList<>();
        if (phone.hasTag()) {
            var list = phone.getTag().getList(UPGRADES, 10);
            if (list.size() < SLOTS) {
                // Migrate old saves that stored only non-empty slots and lost the category boundary.
                List<ItemStack> basic = new ArrayList<>();
                List<ItemStack> special = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    ItemStack upgrade = ItemStack.of(list.getCompound(i));
                    if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                            && phoneUpgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
                        special.add(upgrade);
                    } else if (!upgrade.isEmpty()) {
                        basic.add(upgrade);
                    }
                }
                result.addAll(basic);
                while (result.size() < BASIC_SLOTS) result.add(ItemStack.EMPTY);
                result.addAll(special);
                while (result.size() < SLOTS) result.add(ItemStack.EMPTY);
            } else {
                for (int i = 0; i < SLOTS; i++) {
                    result.add(ItemStack.of(list.getCompound(i)));
                }
            }
        }
        while (result.size() < SLOTS) result.add(ItemStack.EMPTY);
        return result;
    }

    /** Returns whether the entity has a phone equipped with the requested upgrade key. */
    public static boolean hasInstalledUpgrade(LivingEntity entity, String effectKey) {
        for (ItemStack phoneStack : CurioHelper.stacks(entity, CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgrade : phone.getUpgradeStacks(phoneStack)) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                        && effectKey.equals(phoneUpgrade.effectKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 返回玩家安装的指定系列最高等级。 */
    public static int installedSeriesLevel(LivingEntity entity, String series) {
        int level = 0;
        for (ItemStack phoneStack : CurioHelper.stacks(entity, CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgrade : phone.getUpgradeStacks(phoneStack)) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                        && phoneUpgrade.category() == PhoneUpgradeItem.Category.SPECIAL
                        && series.equals(phoneUpgrade.series())) {
                    level = Math.max(level, phoneUpgrade.level());
                }
            }
        }
        return level;
    }

    /** 返回玩家安装的指定系列指定等级数量，用于累计型系列效果。 */
    public static int installedSeriesCount(LivingEntity entity, String series, int maxLevel) {
        int count = 0;
        for (ItemStack phoneStack : CurioHelper.stacks(entity, CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgrade : phone.getUpgradeStacks(phoneStack)) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                        && phoneUpgrade.category() == PhoneUpgradeItem.Category.SPECIAL
                        && series.equals(phoneUpgrade.series())
                        && phoneUpgrade.level() <= maxLevel) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Returns every action currently installed in the entity's equipped phone. */
    public static List<PhoneUpgradeAction> installedActions(LivingEntity entity) {
        List<PhoneUpgradeAction> result = new ArrayList<>();
        for (ItemStack phoneStack : CurioHelper.stacks(entity, CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgrade : phone.getUpgradeStacks(phoneStack)) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade) {
                    result.add(phoneUpgrade.action());
                }
            }
        }
        return result;
    }

    public boolean addUpgrade(ItemStack phone, ItemStack upgrade) {
        if (!(upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade)) return false;
        List<ItemStack> list = getUpgradeStacks(phone);
        if (phoneUpgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
            for (int i = BASIC_SLOTS; i < SLOTS; i++) {
                if (!list.get(i).isEmpty() && ItemStack.isSameItem(list.get(i), upgrade)) return false;
            }
        }
        int start = phoneUpgrade.category() == PhoneUpgradeItem.Category.BASIC ? 0 : BASIC_SLOTS;
        int end = phoneUpgrade.category() == PhoneUpgradeItem.Category.BASIC ? BASIC_SLOTS : SLOTS;
        int index = -1;
        for (int i = start; i < end; i++) {
            if (list.get(i).isEmpty()) {
                index = i;
                break;
            }
        }
        if (index < 0) return false;
        list.set(index, upgrade.copyWithCount(1));
        setUpgradeStacks(phone, list);
        return true;
    }

    public void setUpgradeStacks(ItemStack phone, List<ItemStack> upgrades) {
        var list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack upgrade = i < upgrades.size() ? upgrades.get(i) : ItemStack.EMPTY;
            list.add(upgrade.save(new net.minecraft.nbt.CompoundTag()));
        }
        phone.getOrCreateTag().put(UPGRADES, list);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) { return CurioHelper.PHONE_SLOT.equals(context.identifier()); }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.payday.phone_open",
                DistExecutor.unsafeRunForDist(
                        () -> () -> com.yueyingiop.payday.init.PayDayClient.OPEN_PHONE.getTranslatedKeyMessage(),
                        () -> () -> Component.translatable("key.payday.open_phone"))));
        tooltip.add(Component.translatable("tooltip.payday.phone_upgrades_hint")
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        List<ItemStack> upgrades = getUpgradeStacks(stack);
        int basicCount = 0;
        int specialCount = 0;
        for (ItemStack upgrade : upgrades) {
            if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade) {
                if (phoneUpgrade.category() == PhoneUpgradeItem.Category.BASIC) basicCount++;
                else specialCount++;
            }
        }

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.payday.phone_basic_upgrades", basicCount, BASIC_SLOTS));
            for (ItemStack upgrade : upgrades) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                        && phoneUpgrade.category() == PhoneUpgradeItem.Category.BASIC) {
                    tooltip.add(Component.literal("  ").append(upgrade.getHoverName()));
                }
            }
        }

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.payday.phone_special_upgrades", specialCount, SPECIAL_SLOTS));
            for (ItemStack upgrade : upgrades) {
                if (upgrade.getItem() instanceof PhoneUpgradeItem phoneUpgrade
                        && phoneUpgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
                    tooltip.add(Component.literal("  ").append(upgrade.getHoverName()));
                }
            }
        }
    }

}
