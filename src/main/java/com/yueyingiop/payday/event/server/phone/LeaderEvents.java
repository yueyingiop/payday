package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 处理领袖风范系列的范围筛选、生命值和减伤效果。 */
public final class LeaderEvents {
    private static final String TEAM_HEALTH_PREFIX = "payday_phone_team_health";
    private static final String HUNGER_LAST_EXHAUSTION = "payday_leader2_last_exhaustion";

    private LeaderEvents() {
    }

    /** 每个玩家 tick 刷新领袖系列的团队生命值和饥饿消耗效果。 */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        refreshLeaderHealth(event.player);
        if (PhoneItem.hasInstalledUpgrade(event.player, "special_leader_2")) {
            reduceLeaderExhaustion(event.player);
        }
    }

    /** 找到目标周围16格内实际生效的最高等级领队玩家。 */
    public static Player bestLeaderProvider(LivingEntity target) {
        List<Player> candidates = target.level().getEntitiesOfClass(Player.class,
                target.getBoundingBox().inflate(16.0D), player -> player.isAlive()
                        && !player.isSpectator()
                        && PhoneItem.installedSeriesLevel(player, "leader") > 0);
        return candidates.stream()
                .filter(player -> player.distanceToSqr(target) <= 256.0D)
                .max(Comparator.comparingInt((Player player) ->
                                PhoneItem.installedSeriesLevel(player, "leader"))
                        .thenComparingDouble(player -> -player.distanceToSqr(target))
                        .thenComparing(player -> player.getUUID().toString()))
                .orElse(null);
    }

    /** 判断指定领队玩家是否是目标当前唯一生效的领队。 */
    public static boolean isActiveLeader(Player owner, LivingEntity target) {
        return bestLeaderProvider(target) == owner;
    }

    /** 统计领队周围的其他玩家，最多4人。 */
    public static int otherPlayersInRange(Player owner, double radius) {
        return Math.min(4, owner.level().getEntitiesOfClass(Player.class,
                owner.getBoundingBox().inflate(radius), player -> player != owner
                        && player.isAlive() && !player.isSpectator()
                        && player.distanceToSqr(owner) <= radius * radius).size());
    }

    /** 刷新目标玩家受到领队系列提供的最大生命值修饰符。 */
    public static void refreshLeaderHealth(Player target) {
        AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        List<AttributeModifier> remove = new ArrayList<>();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getName().startsWith(TEAM_HEALTH_PREFIX)) {
                remove.add(modifier);
            }
        }
        remove.forEach(attribute::removeModifier);

        Player leader = bestLeaderProvider(target);
        if (leader == null) {
            target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
            return;
        }

        int level = PhoneItem.installedSeriesLevel(leader, "leader");
        double bonus = 0.0D;
        if (level >= 3) {
            bonus += target == leader ? 0.25D : 0.10D;
        }
        if (level >= 4 && target != leader) {
            bonus += 0.05D;
        }
        if (level >= 5) {
            bonus += otherPlayersInRange(leader, 16.0D) * 0.10D;
        }
        if (bonus != 0.0D) {
            UUID id = UUID.nameUUIDFromBytes((target.getUUID() + "|" + leader.getUUID()
                    + "|leader_health").getBytes(StandardCharsets.UTF_8));
            attribute.addTransientModifier(new AttributeModifier(id, TEAM_HEALTH_PREFIX,
                    bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
    }

    /** 处理领袖系列对伤害减免的加成。 */
    public static float applyLeaderReduction(Player leader, LivingEntity target, float amount) {
        if (!isActiveLeader(leader, target)
                || target.getPersistentData().getBoolean(PhoneUpgradeAction.PIERCE_REDUCTION)) {
            return amount;
        }
        int level = PhoneItem.installedSeriesLevel(leader, "leader");
        double reduction = leader == target && leader.getHealth() <= leader.getMaxHealth() * 0.5D
                ? 0.16D : 0.08D;
        if (leader == target && level >= 2) {
            reduction += 0.05D;
        }
        if (leader == target && level >= 5) {
            reduction += 0.10D;
        }
        return amount * (float) Math.max(0.0D, 1.0D - reduction);
    }

    /** 长跑冠军：将玩家新增的饥饿消耗降低75%。 */
    public static void reduceLeaderExhaustion(Player player) {
        float current = player.getFoodData().getExhaustionLevel();
        float previous = player.getPersistentData().getFloat(HUNGER_LAST_EXHAUSTION);
        if (current > previous) {
            player.getFoodData().addExhaustion(-(current - previous) * 0.75F);
        }
        player.getPersistentData().putFloat(HUNGER_LAST_EXHAUSTION,
                player.getFoodData().getExhaustionLevel());
    }
}
