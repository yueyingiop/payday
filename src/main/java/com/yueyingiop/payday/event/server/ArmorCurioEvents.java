package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.network.PayDayNetwork;
import com.yueyingiop.payday.network.SyncArmorStatePacket;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.event.CurioUnequipEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 处理护甲饰品的更换 CD，以及 Curios 装备事件拦截。 */
public final class ArmorCurioEvents {
    private static final Map<LivingEntity, ForcedArmorChange> FORCED_ARMOR_CHANGES =
            new IdentityHashMap<>();
    private static final Map<UUID, Long> CLIENT_COOLDOWN_ENDS = new java.util.HashMap<>();
    private static final Map<UUID, Long> CLIENT_EQUIPMENT_BOX_CHANGE_COOLDOWN_ENDS = new java.util.HashMap<>();

    private ArmorCurioEvents() {
    }

    /**
     * 拦截 CD 期间通过 Curios 装备护甲饰品的操作。
     *
     * @param event Curios 装备事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void curioEquip(CurioEquipEvent event) {
        LivingEntity entity = event.getSlotContext().entity();
        String identifier = event.getSlotContext().identifier();
        int index = event.getSlotContext().index();
        ItemStack stack = event.getStack();
        if (stack.getItem() instanceof ArmorCurioItem
                && isArmorChangeLocked(entity, identifier, index)) {
            sendCooldownMessage(entity);
            event.setResult(Event.Result.DENY);
        } else if (stack.getItem() instanceof EquipmentBoxItem
                && isEquipmentBoxChangeLocked(entity, identifier, index)) {
            sendEquipmentBoxChangeCooldownMessage(entity);
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * 拦截 CD 期间通过 Curios 卸下护甲饰品的操作。
     *
     * @param event Curios 卸下事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void curioUnequip(CurioUnequipEvent event) {
        LivingEntity entity = event.getSlotContext().entity();
        String identifier = event.getSlotContext().identifier();
        int index = event.getSlotContext().index();
        ItemStack stack = event.getStack();
        if (stack.getItem() instanceof ArmorCurioItem
                && isArmorChangeLocked(entity, identifier, index)) {
            sendCooldownMessage(entity);
            event.setResult(Event.Result.DENY);
        } else if (stack.getItem() instanceof EquipmentBoxItem
                && isEquipmentBoxChangeLocked(entity, identifier, index)) {
            sendEquipmentBoxChangeCooldownMessage(entity);
            event.setResult(Event.Result.DENY);
        }
    }

    /** 在 Curios 处理右键快捷装备前拦截 CD 中的护甲饰品，防止客户端先进行本地替换。 */
    /**
     * 执行 <code>rightClickArmor</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickArmor(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (event.getItemStack().getItem() instanceof ArmorCurioItem
                && isArmorChangeLocked(player, ServerEventUtil.ARMOR, -1)) {
            if (!player.level().isClientSide) {
                sendCooldownMessage(player);
            }
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        } else if (event.getItemStack().getItem() instanceof EquipmentBoxItem
                && isEquipmentBoxChangeLocked(player, ServerEventUtil.EQUIPMENT_BOX, -1)) {
            if (!player.level().isClientSide) {
                sendEquipmentBoxChangeCooldownMessage(player);
            }
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    /**
     * 在护甲饰品实际发生变化后设置或清理更衣 CD。
     *
     * @param event Curios 饰品变化事件
     */
    @SubscribeEvent
    public static void curioChange(CurioChangeEvent event) {
        if (!ServerEventUtil.ARMOR.equals(event.getIdentifier())
                && !ServerEventUtil.EQUIPMENT_BOX.equals(event.getIdentifier())) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (ServerEventUtil.EQUIPMENT_BOX.equals(event.getIdentifier())) {
            if (!(event.getFrom().getItem() instanceof EquipmentBoxItem)
                    && !(event.getTo().getItem() instanceof EquipmentBoxItem)) {
                return;
            }

            if (event.getFrom().isEmpty() || event.getTo().isEmpty()) {
                entity.getPersistentData().remove(ServerEventUtil.EQUIPMENT_BOX_CHANGE_COOLDOWN);
            } else if (entity instanceof Player player && player.getAbilities().instabuild) {
                // 创造模式可以直接更换装备箱，不记录更换冷却。
                entity.getPersistentData().remove(ServerEventUtil.EQUIPMENT_BOX_CHANGE_COOLDOWN);
            } else {
                entity.getPersistentData().putLong(ServerEventUtil.EQUIPMENT_BOX_CHANGE_COOLDOWN,
                        entity.level().getGameTime()
                                + ServerEventUtil.cooldownTicks(
                                Config.EQUIPMENT_BOX_CHANGE_COOLDOWN_SECONDS.get()));
            }
            syncClientState(entity);
            return;
        }

        if (!(event.getFrom().getItem() instanceof ArmorCurioItem)
                && !(event.getTo().getItem() instanceof ArmorCurioItem)) {
            return;
        }

        ForcedArmorChange forcedChange = FORCED_ARMOR_CHANGES.remove(entity);
        boolean forced = forcedChange != null
                && ItemStack.isSameItemSameTags(forcedChange.stack(), event.getTo());

        if (!forced && (event.getFrom().isEmpty() || event.getTo().isEmpty())) {
            entity.getPersistentData().remove(ServerEventUtil.ARMOR_CHANGE_COOLDOWN);
            syncClientState(entity);
            return;
        }

        if (forced) {
            entity.getPersistentData().remove(ServerEventUtil.ARMOR_CHANGE_COOLDOWN);
            entity.getPersistentData().putLong(ServerEventUtil.CLOTHES_BOX_COOLDOWN,
                    entity.level().getGameTime()
                            + ServerEventUtil.cooldownTicks(Config.CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS.get()));
        } else {
            entity.getPersistentData().putLong(ServerEventUtil.ARMOR_CHANGE_COOLDOWN,
                    entity.level().getGameTime()
                            + ServerEventUtil.cooldownTicks(Config.ARMOR_CHANGE_COOLDOWN_SECONDS.get()));
        }
        syncClientState(entity);
    }

    /** 玩家进入世界时同步护甲饰品和更衣 CD，避免客户端沿用旧的快捷装备状态。 */
    /**
     * 执行 <code>playerLoggedIn</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncClientState(event.getEntity());
        EquipmentBoxEvents.syncClientState(event.getEntity());
    }

    /** 判断玩家是否存在护甲饰品更换 CD。 */
    /**
     * 执行 <code>hasArmorChangeCooldown</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    public static boolean hasArmorChangeCooldown(LivingEntity entity) {
        return cooldownRemainingTicks(entity) > 0;
    }

    /** 判断玩家是否处于装备箱饰品更换冷却中。 */
    /**
     * 执行 <code>hasEquipmentBoxChangeCooldown</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    public static boolean hasEquipmentBoxChangeCooldown(LivingEntity entity) {
        return equipmentBoxChangeCooldownRemainingTicks(entity) > 0L;
    }

    /** 获取装备箱饰品更换冷却剩余 tick。 */
    /**
     * 执行 <code>equipmentBoxChangeCooldownRemainingTicks</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    public static long equipmentBoxChangeCooldownRemainingTicks(LivingEntity entity) {
        long end = entity.getPersistentData().getLong(ServerEventUtil.EQUIPMENT_BOX_CHANGE_COOLDOWN);
        if (entity.level().isClientSide) {
            end = Math.max(end, CLIENT_EQUIPMENT_BOX_CHANGE_COOLDOWN_ENDS
                    .getOrDefault(entity.getUUID(), 0L));
        }
        return Math.max(0L, end - entity.level().getGameTime());
    }

    /** 向玩家提示装备箱饰品更换冷却剩余时间。 */
    /**
     * 执行 <code>sendEquipmentBoxChangeCooldownMessage</code> 对应的操作。
     * @param entity 参数值。
     */
    public static void sendEquipmentBoxChangeCooldownMessage(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        long seconds = Math.max(1L, (equipmentBoxChangeCooldownRemainingTicks(entity) + 19L) / 20L);
        player.displayClientMessage(Component.translatable("message.payday.equipment_box_change_cooldown", seconds)
                .withStyle(ChatFormatting.RED), true);
    }

    /** 判断衣帽箱的强制换装冷却是否仍在生效。 */
    /**
     * 执行 <code>hasClothesBoxCooldown</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    public static boolean hasClothesBoxCooldown(LivingEntity entity) {
        return clothesBoxCooldownRemainingTicks(entity) > 0L;
    }

    /** 获取衣帽箱强制换装冷却剩余 tick。 */
    /**
     * 执行 <code>clothesBoxCooldownRemainingTicks</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    public static long clothesBoxCooldownRemainingTicks(LivingEntity entity) {
        long end = entity.getPersistentData().getLong(ServerEventUtil.CLOTHES_BOX_COOLDOWN);
        return Math.max(0L, end - entity.level().getGameTime());
    }

    /** 向玩家提示衣帽箱强制换装冷却剩余时间。 */
    /**
     * 执行 <code>sendClothesBoxCooldownMessage</code> 对应的操作。
     * @param entity 参数值。
     */
    public static void sendClothesBoxCooldownMessage(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        long seconds = Math.max(1L, (clothesBoxCooldownRemainingTicks(entity) + 19L) / 20L);
        player.displayClientMessage(Component.translatable("message.payday.clothes_box_cooldown", seconds)
                .withStyle(ChatFormatting.RED), true);
    }

    /** 更新客户端缓存的更衣 CD 结束时间。 */
    /**
     * 执行 <code>setClientCooldown</code> 对应的操作。
     * @param entityId 参数值。
     * @param endTick 参数值。
     */
    public static void setClientCooldown(UUID entityId, long endTick) {
        if (endTick <= 0L) {
            CLIENT_COOLDOWN_ENDS.remove(entityId);
        } else {
            CLIENT_COOLDOWN_ENDS.put(entityId, endTick);
        }
    }

    /** 更新客户端缓存的装备箱饰品更换冷却结束时间。 */
    /**
     * 执行 <code>setClientEquipmentBoxChangeCooldown</code> 对应的操作。
     * @param entityId 参数值。
     * @param endTick 参数值。
     */
    public static void setClientEquipmentBoxChangeCooldown(UUID entityId, long endTick) {
        if (endTick <= 0L) {
            CLIENT_EQUIPMENT_BOX_CHANGE_COOLDOWN_ENDS.remove(entityId);
        } else {
            CLIENT_EQUIPMENT_BOX_CHANGE_COOLDOWN_ENDS.put(entityId, endTick);
        }
    }

    /** 为衣帽箱发起的强制换装登记预期物品，避免被普通 CD 拦截。 */
    /**
     * 执行 <code>markForcedArmorChange</code> 对应的操作。
     * @param entity 参数值。
     * @param expected 参数值。
     */
    public static void markForcedArmorChange(LivingEntity entity, ItemStack expected) {
        FORCED_ARMOR_CHANGES.put(entity,
                new ForcedArmorChange(expected.copyWithCount(1), entity.level().getGameTime() + 2L));
    }

    /** 设置衣帽箱专用的更衣 CD。 */
    /**
     * 执行 <code>setClothesBoxCooldown</code> 对应的操作。
     * @param player 参数值。
     */
    public static void setClothesBoxCooldown(Player player) {
        long end = player.level().getGameTime()
                + ServerEventUtil.cooldownTicks(Config.CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS.get());
        player.getPersistentData().putLong(ServerEventUtil.CLOTHES_BOX_COOLDOWN, end);
        syncClientState(player);
    }

    /** 刷新当前实体所有护甲饰品由 ArmorSlotItem 提供的属性修饰符。 */
    /**
     * 执行 <code>refreshArmorAttributes</code> 对应的操作。
     * @param entity 参数值。
     */
    public static void refreshArmorAttributes(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        for (var instance : entity.getAttributes().getSyncableAttributes()) {
            List<AttributeModifier> toRemove = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("payday.slot.")) {
                    toRemove.add(modifier);
                }
            }
            for (AttributeModifier modifier : toRemove) {
                instance.removeModifier(modifier);
            }
        }

        for (CurioHelper.SlotStack slot : CurioHelper.slots(entity, CurioHelper.ARMOR_SLOT)) {
            int i = slot.index();
            ItemStack armorStack = slot.stack();
            if (!(armorStack.getItem() instanceof ArmorCurioItem armorCurio)) {
                continue;
            }

            List<ItemStack> upgrades = armorCurio.getUpgradeStacks(armorStack);
            ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
            for (int upgradeIndex = 0; upgradeIndex < upgrades.size(); upgradeIndex++) {
                ItemStack upgradeStack = upgrades.get(upgradeIndex);
                if (!(upgradeStack.getItem() instanceof ArmorSlotItem armorSlot)) {
                    continue;
                }
                List<ArmorSlotItem.ProvidedAttribute> attributes = armorSlot.providedAttributes();
                for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
                    ArmorSlotItem.ProvidedAttribute provided = attributes.get(attributeIndex);
                    UUID modifierId = UUID.nameUUIDFromBytes((CuriosApi.getSlotUuid(
                            new top.theillusivec4.curios.api.SlotContext(ServerEventUtil.ARMOR,
                                    entity, i, false, true)) + "|payday_slot|" + upgradeIndex
                            + "|" + attributeIndex + "|" + provided.attributeId()
                            + "|" + provided.operation().name()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    modifiers.put(provided.attribute(), new AttributeModifier(modifierId,
                            "payday.slot." + upgradeIndex + "." + attributeIndex,
                            provided.value(), provided.operation()));
                }
            }
            entity.getAttributes().addTransientAttributeModifiers(modifiers.build());
        }
    }

    /** 判断指定护甲饰品槽位是否因为更衣 CD 被锁定。 */
    /**
     * 执行 <code>isArmorChangeLocked</code> 对应的操作。
     * @param entity 参数值。
     * @param identifier 参数值。
     * @param index 参数值。
     * @return 方法执行结果。
     */
    public static boolean isArmorChangeLocked(LivingEntity entity, String identifier, int index) {
        if (!ServerEventUtil.ARMOR.equals(identifier)) {
            return false;
        }

        ForcedArmorChange forcedChange = FORCED_ARMOR_CHANGES.get(entity);
        if (forcedChange != null) {
            if (entity.level().getGameTime() <= forcedChange.expiresAt()) {
                return false;
            }
            FORCED_ARMOR_CHANGES.remove(entity);
        }

        boolean occupied;
        if (index >= 0) {
            occupied = !CurioHelper.stackAt(entity, identifier, index).isEmpty();
        } else {
            occupied = hasEquippedArmor(entity);
        }
        return occupied && cooldownRemainingTicks(entity) > 0;
    }

    /** 判断指定装备箱饰品槽位是否因为装备箱更换冷却被锁定。 */
    /**
     * 执行 <code>isEquipmentBoxChangeLocked</code> 对应的操作。
     * @param entity 参数值。
     * @param identifier 参数值。
     * @param index 参数值。
     * @return 方法执行结果。
     */
    public static boolean isEquipmentBoxChangeLocked(LivingEntity entity, String identifier, int index) {
        if (!ServerEventUtil.EQUIPMENT_BOX.equals(identifier)) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            // 创造模式下允许直接摘下或替换 EquipmentBoxItem。
            return false;
        }

        boolean occupied = index >= 0
                ? !CurioHelper.stackAt(entity, identifier, index).isEmpty()
                : hasEquippedEquipmentBox(entity);
        return occupied && equipmentBoxChangeCooldownRemainingTicks(entity) > 0L;
    }

    /** 判断指定实体是否至少装备了一件护甲饰品。 */
    /**
     * 执行 <code>hasEquippedArmor</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    private static boolean hasEquippedArmor(LivingEntity entity) {
        return CurioHelper.hasStack(entity, CurioHelper.ARMOR_SLOT,
                stack -> stack.getItem() instanceof ArmorCurioItem);
    }

    /** 判断指定实体是否至少装备了一件装备箱饰品。 */
    /**
     * 执行 <code>hasEquippedEquipmentBox</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    private static boolean hasEquippedEquipmentBox(LivingEntity entity) {
        return CurioHelper.hasStack(entity, CurioHelper.EQUIPMENT_BOX_SLOT,
                stack -> stack.getItem() instanceof EquipmentBoxItem);
    }

    /** 获取剩余更衣 CD tick。 */
    /**
     * 执行 <code>cooldownRemainingTicks</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    private static long cooldownRemainingTicks(LivingEntity entity) {
        long end = entity.getPersistentData().getLong(ServerEventUtil.ARMOR_CHANGE_COOLDOWN);
        if (entity.level().isClientSide) {
            end = Math.max(end, CLIENT_COOLDOWN_ENDS.getOrDefault(entity.getUUID(), 0L));
        }
        return Math.max(0L, end - entity.level().getGameTime());
    }

    /** 向玩家提示当前还剩多少秒才能更换护甲。 */
    /**
     * 执行 <code>sendCooldownMessage</code> 对应的操作。
     * @param entity 参数值。
     */
    private static void sendCooldownMessage(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }

        long seconds = Math.max(1L, (cooldownRemainingTicks(entity) + 19L) / 20L);
        player.displayClientMessage(Component.translatable("message.payday.armor_change_cooldown", seconds)
                .withStyle(ChatFormatting.RED), true);
        if (!entity.level().isClientSide) {
            syncClientState(player);
        }
    }

    /** 把服务端真实的护甲槽位和 CD 推送给该玩家，修正快捷装备造成的客户端预显示。 */
    /**
     * 执行 <code>syncClientState</code> 对应的操作。
     * @param entity 参数值。
     */
    private static void syncClientState(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        long remaining = cooldownRemainingTicks(entity);
        long equipmentBoxRemaining = equipmentBoxChangeCooldownRemainingTicks(entity);
        for (CurioHelper.SlotStack slot : CurioHelper.slots(entity, CurioHelper.ARMOR_SLOT)) {
            PayDayNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncArmorStatePacket(player.getId(), slot.index(), remaining,
                            equipmentBoxRemaining, slot.stack().copy()));
        }
    }

    private record ForcedArmorChange(ItemStack stack, long expiresAt) {
    }
}
