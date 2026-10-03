package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.record.ArmorTarget;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 处理杀手系列的循环护甲恢复效果。 */
public final class AssassinEvents {
    private static final String ASSASSIN5_NEXT = "payday_assassin5_next_repair";
    private static final String ASSASSIN5_INDEX = "payday_assassin5_repair_index";

    private AssassinEvents() {
    }

    /** 长牙舞爪：每1.5秒回满一个未损坏的 ArmorDefinition。 */
    public static void tickAssassin5(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(ASSASSIN5_NEXT)) {
            return;
        }

        PhoneItem.refreshArmorSlotModifiers(player);
        List<ArmorTarget> targets = armorTargets(player);
        if (targets.isEmpty()) {
            return;
        }

        int start = Math.floorMod(player.getPersistentData().getInt(ASSASSIN5_INDEX),
                targets.size());
        for (int offset = 0; offset < targets.size(); offset++) {
            int index = (start + offset) % targets.size();
            ArmorTarget target = targets.get(index);
            if (!target.armor().isBroken(target.stack(), target.definitionIndex())) {
                ArmorModifier.setCurrent(player, target.armorStack(), target.armor(),
                        target.stack(), target.definitionIndex(),
                        ArmorModifier.effectiveMax(player, target.armorStack(), target.armor(),
                                target.stack(), target.definitionIndex()));
                target.curio().setUpgradeStacks(target.armorStack(), target.upgrades());
                player.getPersistentData().putInt(ASSASSIN5_INDEX, index + 1);
                break;
            }
        }
        player.getPersistentData().putLong(ASSASSIN5_NEXT, now + 30L);
    }

    private static List<ArmorTarget> armorTargets(Player player) {
        List<ArmorTarget> result = new ArrayList<>();
        for (CurioHelper.SlotStack curioSlot : CurioHelper.slots(player, CurioHelper.ARMOR_SLOT)) {
            if (!(curioSlot.stack().getItem() instanceof ArmorCurioItem curio)) {
                continue;
            }
            List<ItemStack> upgrades = curio.getUpgradeStacks(curioSlot.stack());
            for (ItemStack stack : upgrades) {
                if (!(stack.getItem() instanceof ArmorSlotItem armor)) {
                    continue;
                }
                for (int definitionIndex = 0;
                     definitionIndex < armor.armorDefinitions().size(); definitionIndex++) {
                    result.add(new ArmorTarget(curioSlot.stack(), curio, upgrades, armor,
                            stack, definitionIndex));
                }
            }
        }
        return result;
    }
}
