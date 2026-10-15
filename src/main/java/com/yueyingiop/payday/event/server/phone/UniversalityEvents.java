package com.yueyingiop.payday.event.server.phone;

import java.util.ArrayList;

import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import com.yueyingiop.payday.init.RegistryAttributes;
import com.yueyingiop.payday.item.PhoneItem;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.event.CurioChangeEvent;


// 手机通用事件处理器
public final class UniversalityEvents {
    /** 刷新手机提供的团队效果，并在手机取下后清理临时状态。 */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (CurioHelper.stacks(event.player, CurioHelper.PHONE_SLOT).isEmpty()) {
            PhoneItem.clearEffects(event.player);
        }

        PhoneUpgradeAction.refreshAmmoExpansion(event.player);
    }

    /** 手机 Curios 槽位发生变化后立即重算效果，避免属性修饰符残留到下一 tick。 */
    @SubscribeEvent
    public static void phoneChanged(CurioChangeEvent event) {
        if (!CurioHelper.PHONE_SLOT.equals(event.getIdentifier())
                || (!(event.getFrom().getItem() instanceof PhoneItem)
                && !(event.getTo().getItem() instanceof PhoneItem))) {
            return;
        }
        PhoneItem.refreshEffects(event.getEntity());
    }

    // 初始化玩家属性实例，避免在首次使用时出现空引用。
    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, RegistryAttributes.RANGED_DAMAGE.get())) {
            event.add(EntityType.PLAYER, RegistryAttributes.RANGED_DAMAGE.get(), 0.0D);
        }
        if (!event.has(EntityType.PLAYER, RegistryAttributes.DODGE_CHANCE.get())) {
            event.add(EntityType.PLAYER, RegistryAttributes.DODGE_CHANCE.get(), 0.0D);
        }
        if (!event.has(EntityType.PLAYER, RegistryAttributes.EXPOSURE_RISK.get())) {
            event.add(EntityType.PLAYER, RegistryAttributes.EXPOSURE_RISK.get(), 0.0D);
        }
    }

    // 升级代理回调
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void proxyEvent(Event event) {
        if (event instanceof EntityAttributeModificationEvent) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        // This proxy handles server-side upgrade callbacks only. Client render events can
        // also reach the global event bus while an integrated server is running.
        if (server == null || !server.isSameThread()) return;

        if (event instanceof LivingHurtEvent hurt) {
            // 每次新的受伤事件都重新计算穿透规则，避免前一次伤害留下旧标记。
            hurt.getEntity().getPersistentData().remove(PhoneUpgradeAction.PIERCE_REDUCTION);
        }

        // PlayerList exposes a view backed by its mutable internal list. Snapshot it
        // before dispatching callbacks so player join/leave/respawn cannot invalidate iteration.
        var players = new ArrayList<>(server.getPlayerList().getPlayers());

        // 先统一计算穿透/免疫规则，再执行普通代理回调，避免攻击者和受击者的玩家顺序影响穿透结果。
        for (var player : players) {
            for (var action : PhoneItem.installedActions(player)) {
                action.applyDamageRules(event, player);
            }
        }

        // 每个事件都交给当前已装备手机的玩家动作处理；具体升级是否关心该事件由代理回调自行判断。
        for (var player : players) {
            for (var action : PhoneItem.installedActions(player)) {
                action.dispatchProxyEvent(event, player);
            }
        }
    }


    // 远程伤害代理
    @SubscribeEvent
    public static void rangedDamage(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        boolean projectileDamage = event.getSource().is(DamageTypeTags.IS_PROJECTILE) || direct instanceof Projectile;
        // PayDay.LOGGER.info("[RangedDamageDebug] LivingHurtEvent: target={}, amount={}, "
        //         + "damageType={}, source={}, direct={}, projectileDamage={}",
        //         event.getEntity().getName().getString(), event.getAmount(), event.getSource().getMsgId(),
        //         describe(source), describe(direct), projectileDamage);

        if (!projectileDamage) {
            // PayDay.LOGGER.info("[RangedDamageDebug] Ignored: damage source is not a technical projectile");
            return;
        }
        Player player = source instanceof Player directPlayer ? directPlayer
                : direct instanceof Projectile projectile && projectile.getOwner() instanceof Player owner ? owner
                        : null;
        if (player == null) {
            // PayDay.LOGGER.info("[RangedDamageDebug] Ignored: projectile owner is not a player; "
            //         + "source={}, direct={}", describe(source), describe(direct));
            return;
        }
        // PayDay.LOGGER.info("[RangedDamageDebug] Projectile attacker resolved: player={}, uuid={}",
        //         player.getName().getString(), player.getUUID());

        var attribute = player.getAttribute(RegistryAttributes.RANGED_DAMAGE.get());
        if (attribute == null) {
            // PayDay.LOGGER.info("[RangedDamageDebug] Ignored: player has no RANGED_DAMAGE attribute instance");
            return;
        }

        float before = event.getAmount();
        double after = PhoneUpgradeHelper.applyAttributeModifiers(before, attribute);
        if (after != before) {
            event.setAmount((float) after);
            // PayDay.LOGGER.info("[RangedDamageDebug] Applied modifiers: before={}, after={}", before, after);
        }
    }

    // private static String describe(Entity entity) {
    //     return entity == null ? "null" : entity.getClass().getName() + "[" + entity.getName().getString() + "]";
    // }


}
