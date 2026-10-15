package com.yueyingiop.payday.init;

import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.CooldownHud;
import com.yueyingiop.payday.api.CooldownHudManager;
import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import com.yueyingiop.payday.event.server.phone.AnarchistEvents;
import com.yueyingiop.payday.event.server.phone.AntisocialEvents;
import com.yueyingiop.payday.event.server.phone.HackerEvents;
import com.yueyingiop.payday.event.server.phone.AssassinEvents;
import com.yueyingiop.payday.event.server.phone.LeaderEvents;
import com.yueyingiop.payday.event.server.phone.MuscleEvents;
import com.yueyingiop.payday.event.server.phone.ThiefEvents;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.item.ArmorCurioItem;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import java.util.EnumMap;
import java.util.Map;

/** Central configuration for PhoneUpgradeItem actions. */
public final class PhoneUpgradeConfig {
    public static final PhoneUpgradeAction EMPTY = PhoneUpgradeAction.builder().build();
    private static final String HACKER1_COOLDOWN_END = "payday_hacker1_cooldown_end";

    public static PhoneUpgradeAction basic(String series, int level) {
        return switch (series) {
            case "attack" -> PhoneUpgradeAction.builder()
                    .attribute(Attributes.ATTACK_DAMAGE,
                            switch (level) {
                                case 1 -> 1.0D;
                                case 2 -> 2.0D;
                                case 3 -> 4.0D;
                                default -> 8.0D;
                            },
                            AttributeModifier.Operation.ADDITION)
                    .build();
            case "ranged" -> PhoneUpgradeAction.builder()
                    .attribute(RegistryAttributes.RANGED_DAMAGE,
                            switch (level) {
                                case 1 -> 0.01D;
                                case 2 -> 0.04D;
                                case 3 -> 0.08D;
                                default -> 0.16D;
                            },
                            AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .build();
            case "armor" -> PhoneUpgradeAction.builder()
                    .attribute(Attributes.ARMOR,
                            switch (level) {
                                case 1 -> 1.0D;
                                case 2 -> 2.0D;
                                case 3 -> 4.0D;
                                default -> 8.0D;
                            },
                            AttributeModifier.Operation.ADDITION)
                    .attribute(Attributes.ARMOR_TOUGHNESS, 1.0D,
                            AttributeModifier.Operation.ADDITION)
                    .build();
            case "health" -> PhoneUpgradeAction.builder()
                    .attribute(Attributes.MAX_HEALTH,
                            switch (level) {
                                case 1 -> 2.0D;
                                case 2 -> 4.0D;
                                case 3 -> 8.0D;
                                default -> 16.0D;
                            },
                            AttributeModifier.Operation.ADDITION)
                    .build();
            case "speed" -> PhoneUpgradeAction.builder()
                    .attribute(Attributes.MOVEMENT_SPEED,
                            switch (level) {
                                case 1 -> 0.05D;
                                case 2 -> 0.075D;
                                case 3 -> 0.10D;
                                default -> 0.15D;
                            },
                            AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .build();
            default -> PhoneUpgradeAction.builder().build();
        };
    }

    //#region 杂项
    // 头盔飞舞
    public static final PhoneUpgradeAction MISC_HELMET_DANCE = PhoneUpgradeAction.builder()
        // 1.爆头伤害+25%〔来自于TACZ〕
        .addProxyEvent(EntityHurtByGunEvent.Pre.class, (event, owner) -> {
            if (!(owner instanceof Player player) || event.getLogicalSide().isClient()
                    || event.getAttacker() != player || !event.isHeadShot()) {
                return;
            }
            event.setHeadshotMultiplier(event.getHeadshotMultiplier() * 1.25F);
        })
        .build();

    // 叶蔽于林
    public static final PhoneUpgradeAction MISC_FOREST = PhoneUpgradeAction.builder()
        // 1.暴露风险-10
        .attribute(RegistryAttributes.EXPOSURE_RISK, -10.0D, AttributeModifier.Operation.ADDITION)
        // 2.佩戴护甲饰品(ArmorCurioItem)时,并且处于移动状态+15%移速
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            // 没有装备护甲饰品时，不应用叶蔽于林的条件移速效果。
            if (!CurioHelper.hasStack(livingEntity, CurioHelper.ARMOR_SLOT,
                    stack -> stack.getItem() instanceof ArmorCurioItem)) {
                return;
            }

            AttributeInstance movement = livingEntity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement == null) {
                return;
            }

            // 使用一个动态的原版移速修饰符，根据当前移动状态在每次 curioTick 中刷新。
            UUID modifierId = UUID.nameUUIDFromBytes(
                    "payday_phone_upgrade_dynamic_forest_speed".getBytes(StandardCharsets.UTF_8));
            movement.removeModifier(modifierId);
            double amount = livingEntity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D
                    ? 0.15D : -0.075D;
            movement.addTransientModifier(new AttributeModifier(
                    modifierId,
                    "payday_phone_upgrade_dynamic_forest_speed",
                    amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        })
        .build();

    // 衣帽间
    public static final PhoneUpgradeAction MISC_WARDROBE = PhoneUpgradeAction.builder()
        // 1.允许佩戴衣帽箱：由 EquipmentBoxItem.canEquip/canEquipFromUse 根据本升级判断。
        // 2.击杀生物掉落物、经验等掉落数量+35%，不足1时向上取整。
        .addProxyEvent(LivingDropsEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player) || event.getSource().getEntity() != player) {
                return;
            }
            event.getDrops().forEach(drop -> drop.getItem().setCount((int) Math.ceil(drop.getItem().getCount() * 1.35D)));
        })
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();

    // 玩命关头
    public static final PhoneUpgradeAction MISC_LAST_STAND = PhoneUpgradeAction.builder()
        // 1.对其他生物造成的初始伤害+5%。
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player) || event.getEntity() == player) {
                return;
            }
            // 同时支持玩家直接攻击，以及弓、弩、三叉戟、TACZ 子弹等玩家拥有的投射物。
            Player attacker = event.getSource().getEntity() instanceof Player directPlayer
                    ? directPlayer
                    : event.getSource().getDirectEntity() instanceof Projectile projectile
                    && projectile.getOwner() instanceof Player projectileOwner
                    ? projectileOwner : null;
            if (attacker != player) {
                return;
            }
            event.setAmount(event.getAmount() * 1.05F);
        })
        // 2.玩家回血量/回血效果+20%。
        .addProxyEvent(LivingHealEvent.class, (event, owner) -> {
            if (!(owner instanceof Player) || owner != event.getEntity()) {
                return;
            }
            event.setAmount(event.getAmount() * 1.20F);
        })
        .build();
    //#endregion

    //#region 肌肉男
    public static final PhoneUpgradeAction MUSCLE1 = PhoneUpgradeAction.builder()
        // 1.最大生命值+10%。
        .attribute(Attributes.MAX_HEALTH, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        .build();

    public static final PhoneUpgradeAction MUSCLE2 = PhoneUpgradeAction.builder()
         // 1.最大生命值+10%。   
        .attribute(Attributes.MAX_HEALTH, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        // 半径8格内存在其他玩家时，替其他玩家承担5%最终伤害；由战斗事件最低优先级处理。
        .build();

    public static final PhoneUpgradeAction MUSCLE3 = PhoneUpgradeAction.builder()
        // 1.最大生命值+20%。
        .attribute(Attributes.MAX_HEALTH, 0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        .build();

    public static final PhoneUpgradeAction MUSCLE4 = PhoneUpgradeAction.builder()
        // 使用TACZ枪械开火时，有10%概率给予半径16格内的非玩家生物1秒恐惧。
        .addProxyEvent(GunFireEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player) || event.getLogicalSide().isClient()
                    || event.getShooter() != player || player.getRandom().nextFloat() >= 0.10F) {
                return;
            }
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(16.0D), entity -> entity != player
                            && !(entity instanceof Player) && entity.isAlive()
                            && entity.distanceToSqr(player) <= 256.0D)) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        RegistryEffects.FEAR.get(), 20, 0, false, true, true));
            }
        })
        .build();

    public static final PhoneUpgradeAction MUSCLE5 = PhoneUpgradeAction.builder()
        // 1.最大生命值+60%。
        .attribute(Attributes.MAX_HEALTH, 0.60D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        // 2.每5秒恢复玩家最大生命值的3%生命值
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (MuscleEvents.tickMuscle5(livingEntity)
                    && livingEntity instanceof Player player) {
                CooldownHudManager.startTimer(player, "payday_muscle5_heal",
                        CooldownHud.builder()
                                .type(CooldownHud.Type.TIMER)
                                .texture("payday:item/phone_upgrade/generated/special_muscle_5")
                                .value(5.0D)
                                .build());
            }
        })
        // 3.经验+25%
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion
    
    //#region 领队
    public static final PhoneUpgradeAction LEADER1 = PhoneUpgradeAction.builder()
        // 半径16格内的自己及其他玩家提供8%伤害减免；自身生命值低于50%时变为16%。
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (!(owner instanceof Player leader) || !(event.getEntity() instanceof Player target)
                    || leader.distanceToSqr(target) > 256.0D) {
                return;
            }
            event.setAmount(LeaderEvents.applyLeaderReduction(
                    leader, target, event.getAmount()));
        })
        .build();
    public static final PhoneUpgradeAction LEADER2 = PhoneUpgradeAction.builder()
        // 饥饿值消耗降低75%；自身伤害减免再次+5%。由团队刷新和LEADER1统一处理。
        .build();
    public static final PhoneUpgradeAction LEADER3 = PhoneUpgradeAction.builder()
        // 自身+25%最大生命值; 半径16格内其他玩家+10%最大生命值
        .build();
    public static final PhoneUpgradeAction LEADER4 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽额外+15%护甲值; 范围内其他玩家再次+5%最大生命值
        .build();
    public static final PhoneUpgradeAction LEADER5 = PhoneUpgradeAction.builder()
        // 自身伤害减免再次+10%；范围玩家人数加成；获取经验数量+25%。
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 军械师
    public static final PhoneUpgradeAction ARMORER1 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽额外+10%护甲值
        .armorSoltModifier(modifier -> modifier.addArmorMaxMultiplier(0.10D))
        .build();

    public static final PhoneUpgradeAction ARMORER2 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽额外+10%护甲值
        .armorSoltModifier(modifier -> modifier.addArmorMaxMultiplier(0.10D))
        .build();

    public static final PhoneUpgradeAction ARMORER3 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽额外+10%护甲值
        .armorSoltModifier(modifier -> modifier.addArmorMaxMultiplier(0.10D))
        .build();

    public static final PhoneUpgradeAction ARMORER4 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短10%；护甲插槽损坏时提供2秒全伤害免疫，15秒冷却。
        .armorSoltModifier(
            modifier -> modifier
                .reduceRecoveryInterval(0.10D)
                .breakImmunity(2.0D, 15.0D)
        )
        .build();
    public static final PhoneUpgradeAction ARMORER5 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽额外+5%护甲值；恢复间隔再次缩短25%；获取经验数量+25%。
        .armorSoltModifier(
            modifier -> modifier
                .addArmorMaxMultiplier(0.05D)
                .reduceRecoveryInterval(0.25D)
        )
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    // #region 浪人
    public static final PhoneUpgradeAction ROGUE1 = PhoneUpgradeAction.builder()
        .attribute(RegistryAttributes.DODGE_CHANCE, 1500.0D, AttributeModifier.Operation.ADDITION)
        .build();
    public static final PhoneUpgradeAction ROGUE2 = PhoneUpgradeAction.builder()
        // 半径8格内存在其他玩家时，闪避几率+15%。
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (!(livingEntity instanceof Player player)) {
                return;
            }
            if (player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0D),
                    other -> other != player && other.isAlive() && !other.isSpectator()).isEmpty()) {
                return;
            }
            AttributeInstance dodge = player.getAttribute(RegistryAttributes.DODGE_CHANCE.get());
            if (dodge != null) {
                UUID id = UUID.nameUUIDFromBytes("payday_phone_upgrade_rogue2_nearby_player".getBytes(StandardCharsets.UTF_8));
                dodge.addTransientModifier(
                    new AttributeModifier(
                        id,
                        "payday_phone_upgrade_rogue2_nearby_player", 
                        1500.0D,
                        AttributeModifier.Operation.ADDITION
                    )
                );
            }
        })
        .build();
    public static final PhoneUpgradeAction ROGUE3 = PhoneUpgradeAction.builder()
        .attribute(RegistryAttributes.DODGE_CHANCE, 1500.0D, AttributeModifier.Operation.ADDITION)
        .build();
    public static final PhoneUpgradeAction ROGUE4 = PhoneUpgradeAction.builder()
        .attribute(RegistryAttributes.DODGE_CHANCE, 2000.0D, AttributeModifier.Operation.ADDITION)
        .build();
    public static final PhoneUpgradeAction ROGUE5 = PhoneUpgradeAction.builder()
        // 25%概率无视本模组伤害减免；武器攻击间隔-80%；获取经验数量+25%。
        .pierce(0.25D)
        .attribute(Attributes.ATTACK_SPEED, 4.0D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 杀手
    public static final PhoneUpgradeAction ASSASSIN1 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短5%
        .armorSoltModifier(modifier -> modifier.reduceRecoveryInterval(0.05D))
        .build();

    public static final PhoneUpgradeAction ASSASSIN2 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短5%
        .armorSoltModifier(modifier -> modifier.reduceRecoveryInterval(0.05D))
        // 当前枪械弹药容量+50%
        .ammoExpansion(0.50D, PhoneUpgradeAction.Operation.MULTIPLY)
        // 枪械精准度-16
        .addProxyEvent(AttachmentPropertyEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player) || event.getGunItem() != player.getMainHandItem()) {
                return;
            }
            Map<InaccuracyType, Float> current = event.getCacheProperty().getCache(GunProperties.INACCURACY);
            if (current == null) {
                return;
            }
            Map<InaccuracyType, Float> adjusted = new EnumMap<>(InaccuracyType.class);
            current.forEach((type, value) -> adjusted.put(
                type,
                type == InaccuracyType.STAND || 
                type == InaccuracyType.MOVE || 
                type == InaccuracyType.LIE ? value + 1.6F : value
            ));
            event.getCacheProperty().setCache(GunProperties.INACCURACY, adjusted);
        })
        .build();

    public static final PhoneUpgradeAction ASSASSIN3 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短10%
        .armorSoltModifier(modifier -> modifier.reduceRecoveryInterval(0.10D))
        .build();

    public static final PhoneUpgradeAction ASSASSIN4 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短10%
        .armorSoltModifier(modifier -> modifier.reduceRecoveryInterval(0.10D))
        .build();

    public static final PhoneUpgradeAction ASSASSIN5 = PhoneUpgradeAction.builder()
        // 护甲恢复间隔缩短10%；每1.5秒恢复一个未损坏护甲; 获取经验数量+25%。
        .armorSoltModifier(modifier -> modifier.reduceRecoveryInterval(0.10D))
        .curioTick((identifier, index, livingEntity, itemStack) -> AssassinEvents.tickAssassin5(livingEntity))
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 窃贼
    public static final PhoneUpgradeAction THIEF1 = PhoneUpgradeAction.builder()
        .attribute(RegistryAttributes.DODGE_CHANCE, 2000.0D, AttributeModifier.Operation.ADDITION)
        .build();

    public static final PhoneUpgradeAction THIEF2 = PhoneUpgradeAction.builder()
        // 停止移动时, 闪避几率+20%
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (!(livingEntity instanceof Player player)) {
                return;
            }
            if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D) {
                return;
            }
            AttributeInstance dodge = player.getAttribute(RegistryAttributes.DODGE_CHANCE.get());
            if (dodge != null) {
                UUID id = UUID.nameUUIDFromBytes(
                        "payday_phone_upgrade_thief2_stationary_dodge".getBytes(StandardCharsets.UTF_8));
                dodge.addTransientModifier(new AttributeModifier(id,
                        "payday_phone_upgrade_thief2_stationary_dodge", 2000.0D,
                        AttributeModifier.Operation.ADDITION));
            }
        })
        .build();

    public static final PhoneUpgradeAction THIEF3 = PhoneUpgradeAction.builder()
        // 右键方块提供红石信号
        .addProxyEvent(PlayerInteractEvent.RightClickBlock.class, (event, owner) -> {
            if (!(owner instanceof Player player)
                    || event.getEntity() != player
                    || event.getHand() != InteractionHand.MAIN_HAND
                    || event.getLevel().isClientSide()
                    || !(event.getLevel() instanceof ServerLevel level)
                    || !player.isShiftKeyDown()
                ) {
                return;
            }
            if (ThiefEvents.emitTemporaryRedstoneSignal(level, event.getPos())) {
                // 阻止方块原本的右键逻辑再次切换铁门等方块的状态。
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        })
        .build();

    public static final PhoneUpgradeAction THIEF4 = PhoneUpgradeAction.builder()
        // 玩家交互距离翻倍
        .attribute(ForgeMod.BLOCK_REACH, 1.0D, AttributeModifier.Operation.MULTIPLY_BASE)
        .attribute(ForgeMod.ENTITY_REACH, 1.0D, AttributeModifier.Operation.MULTIPLY_BASE)
        .build();
        
    public static final PhoneUpgradeAction THIEF5 = PhoneUpgradeAction.builder()
        // 停止移动时护甲恢复间隔缩短25%; 潜行移动速度翻倍; 获取经验数量+25%
        .armorSoltModifier(modifier -> {
            if (modifier.owner() instanceof Player player
                    && player.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6D) {
                modifier.reduceRecoveryInterval(0.25D);
            }
        })
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (!(livingEntity instanceof Player player)
                    || !player.isCrouching()
                    || player.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6D) {
                return;
            }
            AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement == null) {
                return;
            }
            UUID id = UUID.nameUUIDFromBytes(
                    "payday_phone_upgrade_thief5_sneak_speed".getBytes(StandardCharsets.UTF_8));
            movement.addTransientModifier(new AttributeModifier(id,
                    "payday_phone_upgrade_thief5_sneak_speed", 1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        })
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 渗透者
    public static final PhoneUpgradeAction INFILTRATOR1 = PhoneUpgradeAction.builder()
        // 半径8格内存在3个及以上非友善生物时, 给予12%伤害减免
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (owner instanceof Player player && event.getEntity() == player
                    && !PhoneUpgradeHelper.isPiercing(event)
                    && PhoneUpgradeHelper.countUnfriendly(player, 8.0D) >= 3) {
                event.setAmount(event.getAmount() * 0.88F);
            }
        })
        .build();

    public static final PhoneUpgradeAction INFILTRATOR2 = PhoneUpgradeAction.builder()
        // 半径16格内存在非友善生物时, 给予8%伤害减免
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (owner instanceof Player player && event.getEntity() == player
                    && !PhoneUpgradeHelper.isPiercing(event)
                    && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1) {
                event.setAmount(event.getAmount() * 0.92F);
            }
        })
        .build();

    public static final PhoneUpgradeAction INFILTRATOR3 = PhoneUpgradeAction.builder()
        // 伤害减免; 近战命中后给予1秒渗透
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)) {
                return;
            }
            if (event.getEntity() == player && !PhoneUpgradeHelper.isPiercing(event)
                    && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1) {
                event.setAmount(event.getAmount() * 0.92F);
            }
            PhoneUpgradeHelper.applyInfiltrationOnMeleeHit(event, player, 20);
        })
        .build();

    public static final PhoneUpgradeAction INFILTRATOR4 = PhoneUpgradeAction.builder()
        // 伤害减免; 近战命中后的渗透持续7.5秒
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)) {
                return;
            }
            if (event.getEntity() == player && !PhoneUpgradeHelper.isPiercing(event)
                    && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1) {
                event.setAmount(event.getAmount() * 0.92F);
            }
            PhoneUpgradeHelper.applyInfiltrationOnMeleeHit(event, player, 150);
        })
        .build();

    public static final PhoneUpgradeAction INFILTRATOR5 = PhoneUpgradeAction.builder()
        // 近战攻击恢复20%血量, 每10秒最多一次; 获取经验数量+25%
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (owner instanceof Player player) {
                if (PhoneUpgradeHelper.tryMeleeHealAndReport(event, player, 0.20D,
                        PhoneUpgradeHelper.HealingMode.PERCENTAGE, 200L,
                        "payday_infiltrator5_lifesteal_cooldown")) {
                    CooldownHudManager.startTimer(player, "payday_infiltrator5_lifesteal",
                            CooldownHud.builder()
                                    .type(CooldownHud.Type.TIMER)
                                    .texture("payday:item/phone_upgrade/generated/special_infiltrator_5")
                                    .value(10.0D)
                                    .build());
                }
            }
        })
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 反社会者
    public static final PhoneUpgradeAction ANTISOCIAL1 = PhoneUpgradeAction.builder()
        // 半径8格内有至少3个非友善生物时, 近战命中非友善生物后给予1秒渗透。
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)
                    || !AntisocialEvents.isQualifiedMeleeHit(event, player, 8.0D, 3)) {
                return;
            }
            event.getEntity().addEffect(new MobEffectInstance(
                    RegistryEffects.INFILTRATION.get(), 20, 0, false, true, true));
        })
        .build();
    public static final PhoneUpgradeAction ANTISOCIAL2 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽的护甲上限额外+10%。
        .armorSoltModifier(modifier -> modifier.addArmorMaxMultiplier(0.10D))
        .build();
    public static final PhoneUpgradeAction ANTISOCIAL3 = PhoneUpgradeAction.builder()
        // 近战击杀恢复10%最大生命值, 每1秒至多生效一次。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)
                    || !PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, player)) {
                return;
            }
            long now = player.level().getGameTime();
            String key = "payday_antisocial3_next_heal";
            if (now < player.getPersistentData().getLong(key)) {
                return;
            }
            player.heal(player.getMaxHealth() * 0.10F);
            player.getPersistentData().putLong(key, now + 20L);
        })
        // 半径16格内有至少1个非友善生物时, 受到伤害降低8%。
        .addProxyEvent(LivingHurtEvent.class, (event, owner) -> {
            if (owner instanceof Player player && event.getEntity() == player
                    && !PhoneUpgradeHelper.isPiercing(event)
                    && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1) {
                event.setAmount(event.getAmount() * 0.92F);
            }
        })
        .build();
    public static final PhoneUpgradeAction ANTISOCIAL4 = PhoneUpgradeAction.builder()
        // 每个护甲升级插槽的护甲上限额外+10%。
        .armorSoltModifier(modifier -> modifier.addArmorMaxMultiplier(0.10D))
        // 击杀非友善生物后恢复30点护甲, 每0.5秒至多生效一次。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (owner instanceof Player player) {
                PhoneUpgradeHelper.restoreArmor(event, player, 30.0D, 10L,
                        "payday_antisocial4_next_repair");
            }
        })
        .build();
    public static final PhoneUpgradeAction ANTISOCIAL5 = PhoneUpgradeAction.builder()
        // 击杀非友善生物后恢复30点护甲, 每1秒至多生效一次。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (owner instanceof Player player) {
                PhoneUpgradeHelper.restoreArmor(event, player, 30.0D, 20L,
                        "payday_antisocial5_next_repair");
            }
        })
        // 半径16格内击杀非玩家生物后, 75%概率给予范围内所有非玩家生物1秒恐惧。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)
                    || !PhoneUpgradeHelper.isPlayerKillOfNonPlayer(event, player)
                    || player.getRandom().nextDouble() >= 0.75D) {
                return;
            }
            for (LivingEntity target : player.level().getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(16.0D),
                    target -> target != player && !(target instanceof Player)
                            && target.isAlive()
                            && target.distanceToSqr(player) <= 256.0D)) {
                target.addEffect(new MobEffectInstance(
                        RegistryEffects.FEAR.get(), 20, 0, false, true, true));
            }
        })
        // 玩家获取经验数量+25%。
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 无政府主义者
    public static final PhoneUpgradeAction ANARCHIST1 = PhoneUpgradeAction.builder()
        // 禁用普通护甲恢复间隔，改为按 ArmorModifier 的固定规则恢复；损坏时提供2秒无敌，15秒冷却。
        .armorSoltModifier(modifier -> modifier
                .enableAnarchistRecovery()
                .breakImmunity(2.0D, 15.0D,
                        "payday:item/phone_upgrade/generated/special_anarchist_1"))
        .build();
    public static final PhoneUpgradeAction ANARCHIST2 = PhoneUpgradeAction.builder()
        // 最大生命值按当前无政府主义者等级缩放，并按公式增加每个 ArmorDefinition 的护甲上限。
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (livingEntity instanceof Player player) {
                AnarchistEvents.applyAnarchistHealth(player);
            }
        })
        .armorSoltModifier(modifier -> {
            if (!(modifier.owner() instanceof Player player)) {
                return;
            }
            int level = PhoneItem.installedSeriesLevel(player, "anarchist");
            if (level < 2) {
                return;
            }
            double currentHealth = player.getMaxHealth();
            double originalHealth = currentHealth / Math.pow(0.5D, level - 1);
            modifier.addArmorMax(AnarchistEvents.anarchistArmorBonus(originalHealth, level));
        })
        .build();
public static final PhoneUpgradeAction ANARCHIST3 = PhoneUpgradeAction.builder()
        // 该等级本身没有额外独立效果；公式由 ANARCHIST2 根据当前系列等级统一计算。
        .build();
    public static final PhoneUpgradeAction ANARCHIST4 = PhoneUpgradeAction.builder()
        // 该等级本身没有额外独立效果；公式由 ANARCHIST2 根据当前系列等级统一计算。
        .build();
    public static final PhoneUpgradeAction ANARCHIST5 = PhoneUpgradeAction.builder()
        // 造成伤害恢复20点护甲, 每15秒至多生效一次。
        .addProxyEvent(net.minecraftforge.event.entity.living.LivingDamageEvent.class,
                (event, owner) -> {
                    if (!(owner instanceof Player player)
                            || event.getAmount() <= 0.0F
                            || !PhoneUpgradeHelper.isPlayerAttacker(event.getSource(), player)) {
                        return;
                    }
                    long now = player.level().getGameTime();
                    String key = "payday_anarchist5_next_repair";
                    if (now < player.getPersistentData().getLong(key)) {
                        return;
                    }
                    PhoneItem.refreshArmorSlotModifiers(player);
                    for (CurioHelper.SlotStack slot : CurioHelper.slots(player,
                            CurioHelper.ARMOR_SLOT)) {
                        if (slot.stack().getItem() instanceof ArmorCurioItem curio) {
                            new ArmorModifier(ItemStack.EMPTY, slot.stack(), curio, player)
                                    .restoreArmor(20.0D);
                        }
                    }
                    player.getPersistentData().putLong(key, now + 300L);
                })
        // 玩家获取经验数量+25%。
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

    //#region 黑客
    public static final PhoneUpgradeAction HACKER1 = PhoneUpgradeAction.builder()
        // Z键激活“电磁干扰”，45秒冷却；击杀敌对生物会减少5秒冷却。
        .bindKeyActivate(InputConstants.KEY_Z, player -> {
            if (player.level().isClientSide) {
                return false;
            }
            long now = player.level().getGameTime();
            if (now < player.getPersistentData().getLong(HACKER1_COOLDOWN_END)) {
                return false;
            }
            player.addEffect(new MobEffectInstance(RegistryEffects.HOWL.get(), 100, 0, false, true, true));
            player.getPersistentData().putLong(HACKER1_COOLDOWN_END, now + 900L);
            return true;
        })
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player)
                    || !PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, player)) {
                return;
            }
            long now = player.level().getGameTime();
            long cooldownEnd = player.getPersistentData().getLong(HACKER1_COOLDOWN_END);
            player.getPersistentData().putLong(HACKER1_COOLDOWN_END,
                    Math.max(now, cooldownEnd - 100L));
        })
        .build();

    public static final PhoneUpgradeAction HACKER2 = PhoneUpgradeAction.builder()
        // 最大生命值+20%。
        .attribute(Attributes.MAX_HEALTH, 0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL)
        .build();

    public static final PhoneUpgradeAction HACKER3 = PhoneUpgradeAction.builder()
        // 啸叫期间击杀敌对生物恢复20点生命。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (owner instanceof Player player && player.hasEffect(RegistryEffects.HOWL.get())
                    && PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, player)) {
                player.heal(20.0F);
            }
        })
        // 啸叫期间提供15%闪避，交由DebuffEvents读取DODGE_CHANCE属性判定。
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (livingEntity instanceof Player player
                    && player.hasEffect(RegistryEffects.HOWL.get())) {
                        HackerEvents.addDodgeChanceModifier(player, HackerEvents.HACKER3_DODGE_MODIFIER,
                        "payday_phone_upgrade_hacker3_dodge", 1500.0D);
            }
        })
        .build();

    public static final PhoneUpgradeAction HACKER4 = PhoneUpgradeAction.builder()
        // 啸叫期间击杀敌对生物获得30秒20%闪避，效果不可叠加。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (!(owner instanceof Player player) || !player.hasEffect(RegistryEffects.HOWL.get())
                    || !PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, player)) {
                return;
            }
            long now = player.level().getGameTime();
            if (now >= player.getPersistentData().getLong(HackerEvents.HACKER4_DODGE_END)) {
                player.getPersistentData().putLong(HackerEvents.HACKER4_DODGE_END, now + 600L);
                HackerEvents.addDodgeChanceModifier(player, HackerEvents.HACKER4_DODGE_MODIFIER,
                        "payday_phone_upgrade_hacker4_dodge", 2000.0D);
            }
        })
        // 临时闪避有效期间提供20%闪避，交由DebuffEvents读取DODGE_CHANCE属性判定。
        .curioTick((identifier, index, livingEntity, itemStack) -> {
            if (!(livingEntity instanceof Player player)
                    || player.level().getGameTime()
                    >= player.getPersistentData().getLong(HackerEvents.HACKER4_DODGE_END)) {
                return;
            }
            HackerEvents.addDodgeChanceModifier(player, HackerEvents.HACKER4_DODGE_MODIFIER,
                    "payday_phone_upgrade_hacker4_dodge", 2000.0D);
        })
        .build();

    public static final PhoneUpgradeAction HACKER5 = PhoneUpgradeAction.builder()
        // 啸叫期间，半径64格内的玩家击杀敌对生物恢复10点生命。
        .addProxyEvent(LivingDeathEvent.class, (event, owner) -> {
            if (!(owner instanceof Player provider)
                    || !provider.hasEffect(RegistryEffects.HOWL.get())) {
                return;
            }
            Player killer = PhoneUpgradeHelper.getPlayerAttacker(event.getSource());
            if (killer != null && PhoneUpgradeHelper.isPlayerKillOfUnfriendly(event, killer)
                    && provider.distanceToSqr(killer) <= 4096.0D) {
                killer.heal(10.0F);
            }
        })
        .addProxyEvent(LivingExperienceDropEvent.class,
                (event, owner) -> PhoneUpgradeHelper.applyExperienceBonus(event, owner, 0.25D))
        .build();
    //#endregion

}
