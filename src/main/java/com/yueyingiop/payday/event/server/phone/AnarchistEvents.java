package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 处理无政府主义者系列的生命值转换和护甲公式。 */
public final class AnarchistEvents {
    private static final UUID ANARCHIST_HEALTH_MODIFIER = UUID.nameUUIDFromBytes(
            "payday_phone_upgrade_anarchist_health".getBytes(StandardCharsets.UTF_8));

    private AnarchistEvents() {
    }

    /** 应用无政府主义者的最大生命值缩放。 */
    public static void applyAnarchistHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(ANARCHIST_HEALTH_MODIFIER);
        int level = PhoneItem.installedSeriesLevel(player, "anarchist");
        if (level >= 2) {
            double multiplier = Math.pow(0.5D, level - 1) - 1.0D;
            attribute.addTransientModifier(new AttributeModifier(
                    ANARCHIST_HEALTH_MODIFIER,
                    "payday_phone_upgrade_anarchist_health",
                    multiplier,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    /**
     * 计算无政府主义者公式提供的额外护甲值，并向上取整。
     *
     * @param maximumHealthBeforeConversion 转换前的最大生命值
     * @param level 当前无政府主义者等级
     * @return 应增加的护甲值
     */
    public static double anarchistArmorBonus(double maximumHealthBeforeConversion, int level) {
        double bonus = 0.0D;
        for (int x = 2; x <= level; x++) {
            bonus += maximumHealthBeforeConversion
                    * Math.pow(0.5D, 1.0D / (x - 1))
                    * (2.0D + (x - 2) * 0.1D);
        }
        return Math.ceil(bonus);
    }
}
