package com.yueyingiop.payday.api;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.phone.AmmoExpansion;
import com.yueyingiop.payday.api.phone.ArmorSoltModifier;
import com.yueyingiop.payday.api.phone.AttributeDefinition;
import com.yueyingiop.payday.api.phone.CurioTick;
import com.yueyingiop.payday.api.phone.DamageImmunity;
import com.yueyingiop.payday.api.phone.ProxyEvent;
import com.yueyingiop.payday.api.helper.AttributeModifierHelper;
import com.yueyingiop.payday.api.phone.ProxyEventDefinition;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 描述一个手机升级提供的属性、状态、事件代理和护甲处理效果。 */
public final class PhoneUpgradeAction {
    private static final String MODIFIER_PREFIX = "payday_phone_upgrade_";
    /** TACZ 枪械上由 PayDay 维护的最终弹匣容量。该值不是当前弹药数量。 */
    public static final String AMMO_CAPACITY = "payday_ammo_capacity";
    /** 本次伤害跳过本模组减伤和护甲吸收的标记。 */
    public static final String PIERCE_REDUCTION = "payday_ignore_damage_reduction";
    /** 未绑定按键时使用的内部按键值。 */
    private static final int NO_ACTIVATION_KEY = -1;
    /** 所有已经声明过按键激活的按键码，供客户端筛选键盘事件。 */
    private static final Set<Integer> BOUND_ACTIVATION_KEYS = ConcurrentHashMap.newKeySet();
    private final List<AttributeDefinition> attributes;
    private final List<MobEffectInstance> effects;
    private final CurioTick curioTick;
    private final ArmorSoltModifier armorSoltModifier;
    private final List<ProxyEventDefinition<?>> proxyEvents;
    private final double pierceProbability;
    private final List<DamageImmunity> immunities;
    private final List<AmmoExpansion> ammoExpansions;
    private final int activationKey;
    private final KeyActivate keyActivate;

    /**
     * 创建一个不可变的手机升级动作。
     *
     * @param attributes 属性修饰符定义列表
     * @param effects 药水效果列表
     * @param curioTick Curios tick 回调，可以为 {@code null}
     * @param armorSoltModifier 护甲插槽修改回调，可以为 {@code null}
     * @param proxyEvents Forge 事件代理定义列表
     * @param pierceProbability 穿透本模组减伤的概率
     * @param immunities 伤害免疫规则列表
     * @param ammoExpansions TACZ 枪械弹匣扩容定义列表
     * @param activationKey 按键激活按键码；未绑定时为 {@code -1}
     * @param keyActivate 按键激活回调，可以为 {@code null}
     * @throws NullPointerException 当任一列表参数为 {@code null} 时抛出
     */
    private PhoneUpgradeAction(
        List<AttributeDefinition> attributes,
        List<MobEffectInstance> effects,
        CurioTick curioTick,
        ArmorSoltModifier armorSoltModifier,
        List<ProxyEventDefinition<?>> proxyEvents,
        double pierceProbability,
        List<DamageImmunity> immunities,
        List<AmmoExpansion> ammoExpansions,
        int activationKey,
        KeyActivate keyActivate
    ) {
        this.attributes = List.copyOf(attributes);
        this.effects = List.copyOf(effects);
        this.curioTick = curioTick;
        this.armorSoltModifier = armorSoltModifier;
        this.proxyEvents = List.copyOf(proxyEvents);
        this.pierceProbability = pierceProbability;
        this.immunities = List.copyOf(immunities);
        this.ammoExpansions = List.copyOf(ammoExpansions);
        this.activationKey = activationKey;
        this.keyActivate = keyActivate;
        if (activationKey != NO_ACTIVATION_KEY) {
            BOUND_ACTIVATION_KEYS.add(activationKey);
        }
    }

    /**
     * 创建手机升级动作构建器。
     *
     * @return 新的手机升级动作构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 判断客户端是否存在指定按键的升级绑定。
     *
     * @param key 按键码
     * @return 是否有升级声明绑定该按键
     */
    public static boolean hasBoundActivationKey(int key) {
        return BOUND_ACTIVATION_KEYS.contains(key);
    }

    /**
     * 激活玩家已安装的指定按键升级。
     *
     * @param player 发起激活请求的玩家
     * @param key 客户端发送的按键码
     * @return 至少一个升级成功激活时返回 {@code true}
     */
    public static boolean activateBoundKey(Player player, int key) {
        if (player.level().isClientSide) {
            return false;
        }

        boolean activated = false;
        for (PhoneUpgradeAction action : com.yueyingiop.payday.item.PhoneItem.installedActions(player)) {
            if (action.activationKey == key && action.keyActivate != null) {
                activated |= action.keyActivate.activate(player);
            }
        }
        return activated;
    }

    /**
     * 移除实体上由手机升级添加的属性修饰符。
     *
     * @param entity 要清理属性修饰符的实体
     */
    public static void clearAttributeModifiers(LivingEntity entity) {
        // 不只遍历 syncable 属性。攻击力等属性在部分实体属性表中可能不属于
        // getSyncableAttributes() 返回集合，但手机升级仍然可能向其添加修饰符。
        Set<net.minecraft.world.entity.ai.attributes.AttributeInstance> instances =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        instances.addAll(entity.getAttributes().getSyncableAttributes());
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            var instance = entity.getAttribute(attribute);
            if (instance != null) {
                instances.add(instance);
            }
        }
        // 原版攻击力是最容易遗漏的属性，显式加入，确保拆除基础攻击升级后立即恢复。
        var attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            instances.add(attackDamage);
        }

        for (var instance : instances) {
            List<AttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
            for (AttributeModifier modifier : modifiers) {
                if (modifier.getName().startsWith(MODIFIER_PREFIX)) {
                    // 按 UUID 删除，避免不同对象实例导致瞬时修饰符无法移除。
                    instance.removeModifier(modifier.getId());
                }
            }
        }
    }

    /**
     * 应用升级效果，并执行该升级注册的 Curios tick 回调。
     *
     * <p>此重载不提供手机物品上下文，因此不会执行护甲插槽修改回调。</p>
     *
     * @param identifier Curios 槽位标识
     * @param slotIndex 升级在手机中的槽位索引
     * @param entity 应用升级效果的实体
     * @param upgrade 当前升级物品
     */
    public void apply(String identifier, int slotIndex, LivingEntity entity, ItemStack upgrade) {
        apply(identifier, slotIndex, entity, upgrade, null);
    }

    /**
     * 应用升级效果，并将手机上下文传给护甲插槽回调。
     *
     * <p>方法会添加属性修饰符和药水效果，执行 Curios tick 回调；当提供
     * {@code phoneStack} 时，还会遍历实体装备的护甲饰品并执行护甲插槽修改回调。</p>
     *
     * @param identifier Curios 槽位标识
     * @param slotIndex 升级在手机中的槽位索引
     * @param entity 应用升级效果的实体
     * @param upgrade 当前升级物品
     * @param phoneStack 当前装备的手机物品，可以为 {@code null}
     */
    public void apply(String identifier, int slotIndex, LivingEntity entity, ItemStack upgrade, ItemStack phoneStack) {
        String source = MODIFIER_PREFIX + slotIndex + "_" + upgrade.getItem().toString();
        for (AttributeDefinition definition : attributes) {
            var instance = entity.getAttribute(definition.attribute());
            if (instance == null) {
                continue;
            }
            UUID id = UUID.nameUUIDFromBytes((source + definition.attribute().getDescriptionId())
                    .getBytes(StandardCharsets.UTF_8));
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, source, definition.amount(), definition.operation()));
        }
        for (MobEffectInstance effect : effects) {
            entity.addEffect(new MobEffectInstance(effect));
        }
        if (curioTick != null) {
            curioTick.accept(identifier, slotIndex, entity, upgrade);
        }
        if (phoneStack != null) {
            applyArmorSoltModifier(phoneStack, entity);
        }
    }

    /**
     * 为当前实体装备的每件护甲饰品创建 {@link ArmorModifier} 上下文。
     * 回调可以通过上下文访问手机、护甲饰品、护甲升级列表以及代理实体。
     *
     * <p>客户端不会执行该回调，因为护甲状态修改必须在服务端完成。</p>
     *
     * @param phoneStack 当前装备的手机物品
     * @param entity 佩戴护甲饰品并作为修改代理的实体
     */
    public void applyArmorSoltModifier(ItemStack phoneStack, LivingEntity entity) {
        if (armorSoltModifier == null) {
            return;
        }
        for (CurioHelper.SlotStack slot : CurioHelper.slots(entity, CurioHelper.ARMOR_SLOT)) {
            if (slot.stack().getItem() instanceof ArmorCurioItem armorCurio) {
                armorSoltModifier.accept(new ArmorModifier(phoneStack, slot.stack(), armorCurio, entity));
            }
        }
    }

    /**
     * 返回该升级声明的属性修饰符定义。
     *
     * @return 不可变的属性修饰符定义列表
     */
    public List<AttributeDefinition> getAttributes() {
        return attributes;
    }

    /**
     * 返回该升级声明的药水效果。
     *
     * @return 不可变的药水效果列表
     */
    public List<MobEffectInstance> getEffects() {
        return effects;
    }

    /**
     * 根据当前安装的扩容升级同步 TACZ 枪械的最终弹匣容量。
     *
     * <p>这里只写入容量标记，并且只在当前弹药超过新容量时裁剪弹药；不会因为装备升级
     * 直接增加当前弹药。TACZ 的容量计算 Mixin 会读取这个标记，因此换弹时仍由 TACZ
     * 正常计算需求量并从玩家库存消耗弹药。</p>
     *
     * @param owner 要刷新主手枪械容量的实体；非玩家或客户端实体会被忽略
     */
    public static void refreshAmmoExpansion(LivingEntity owner) {
        if (!(owner instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ItemStack heldGun = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(heldGun);
        if (gun == null) {
            return;
        }
        CommonGunIndex gunIndex = CommonAssetsManager.getInstance().getGunIndex(gun.getGunId(heldGun));
        if (gunIndex == null || gunIndex.getGunData() == null) {
            return;
        }

        // 先暂时移除 PayDay 标记，得到 TACZ 原始容量，避免重复套用扩容。
        Integer previousCapacity = removeCustomAmmoCapacity(heldGun);
        int baseCapacity = AttachmentDataUtils.getAmmoCountWithAttachment(heldGun, gunIndex.getGunData());
        if (baseCapacity <= 0) {
            restoreCustomAmmoCapacity(heldGun, previousCapacity);
            return;
        }
        int capacity = baseCapacity;
        boolean hasExpansion = false;
        for (PhoneUpgradeAction action : com.yueyingiop.payday.item.PhoneItem.installedActions(owner)) {
            for (AmmoExpansion expansion : action.ammoExpansions) {
                if (expansion.supports(gunIndex.getType())) {
                    hasExpansion = true;
                    capacity = expansion.apply(capacity);
                }
            }
        }

        int currentAmmo = gun.getCurrentAmmoCount(heldGun);
        if (hasExpansion) {
            heldGun.getOrCreateTag().putInt(AMMO_CAPACITY, capacity);
        } else {
            removeCustomAmmoCapacity(heldGun);
        }

        // 扩容升级不会补充当前弹药；拆除扩容后，超出新容量的弹药才会被裁剪。
        if (currentAmmo > capacity) {
            gun.setCurrentAmmoCount(heldGun, capacity);
        }
    }

    /**
     * 移除枪械物品上的 PayDay 自定义弹匣容量标记。
     *
     * @param gunStack 要处理的枪械物品
     * @return 移除前的自定义容量；不存在标记时返回 {@code null}
     */
    private static Integer removeCustomAmmoCapacity(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();
        if (tag == null || !tag.contains(AMMO_CAPACITY, Tag.TAG_INT)) {
            return null;
        }
        int previous = tag.getInt(AMMO_CAPACITY);
        tag.remove(AMMO_CAPACITY);
        return previous;
    }

    /**
     * 恢复枪械物品上的 PayDay 自定义弹匣容量标记。
     *
     * @param gunStack 要处理的枪械物品
     * @param capacity 要恢复的容量；为 {@code null} 时不执行任何操作
     */
    private static void restoreCustomAmmoCapacity(ItemStack gunStack, Integer capacity) {
        if (capacity != null) {
            gunStack.getOrCreateTag().putInt(AMMO_CAPACITY, capacity);
        }
    }

    /**
     * 将当前升级注册的事件代理分发给指定手机持有者。
     *
     * <p>只有事件类型与代理定义匹配时才会执行对应回调。</p>
     *
     * @param event 当前 Forge 事件
     * @param owner 持有该升级的实体
     */
    @SuppressWarnings("unchecked")
    public void dispatchProxyEvent(Event event, LivingEntity owner) {
        for (ProxyEventDefinition<?> definition : proxyEvents) {
            if (definition.eventType().isInstance(event)) {
                ((ProxyEventDefinition<Event>) definition).callback().accept(event, owner);
            }
        }
    }

    /**
     * 应用升级声明的通用伤害规则。
     *
     * <p>pierce 由攻击者持有的升级触发，immune 由受到伤害的实体持有的升级触发。</p>
     *
     * @param event 当前 Forge 事件；目前处理 {@link LivingHurtEvent} 和
     *              {@link LivingAttackEvent}
     * @param owner 当前正在应用规则的手机升级持有者
     * @implNote {@code pierce} 规则会在受击实体的持久化数据中写入临时标记，
     *           该标记由伤害处理流程负责在本次伤害结束后清理。
     */
    public void applyDamageRules(Event event, LivingEntity owner) {
        if (event instanceof LivingHurtEvent hurt && pierceProbability > 0.0D
                && resolveAttacker(hurt.getSource()) == owner
                && roll(owner, pierceProbability)) {
            hurt.getEntity().getPersistentData().putBoolean(PIERCE_REDUCTION, true);
        }

        if (event instanceof LivingAttackEvent attack
                && attack.getEntity() == owner && !attack.isCanceled()) {
            for (DamageImmunity immunity : immunities) {
                if (immunity.matches(attack.getSource())
                        && roll(owner, immunity.probability())) {
                    attack.setCanceled(true);
                    return;
                }
            }
        }
    }

    /**
     * 根据指定概率使用实体的随机数生成器进行判定。
     *
     * @param owner 提供随机数生成器的实体
     * @param probability 判定概率，范围为 {@code 0.0~1.0}
     * @return 概率判定是否成功
     */
    private static boolean roll(LivingEntity owner, double probability) {
        return probability >= 1.0D || probability > 0.0D
                && owner.getRandom().nextDouble() < probability;
    }

    /**
     * 从伤害来源解析实际攻击者。
     *
     * <p>该方法同时支持直接由生物造成的伤害，以及由生物拥有的投射物造成的伤害。</p>
     *
     * @param source 要解析的伤害来源
     * @return 解析出的攻击者；无法解析时返回 {@code null}
     */
    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living;
        }
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /** 手机升级动作构建器。 */
    public static final class Builder {
        private final List<AttributeDefinition> attributes = new ArrayList<>();
        private final List<MobEffectInstance> effects = new ArrayList<>();
        private final List<ProxyEventDefinition<?>> proxyEvents = new ArrayList<>();
        private final List<DamageImmunity> immunities = new ArrayList<>();
        private CurioTick curioTick;
        private ArmorSoltModifier armorSoltModifier;
        private double pierceProbability;
        private final List<AmmoExpansion> ammoExpansions = new ArrayList<>();
        private int activationKey = NO_ACTIVATION_KEY;
        private KeyActivate keyActivate;

        /**
         * 添加属性修饰符定义。
         *
         * <p>该方法使用单一的 {@code Object} 参数入口，避免 Rhino/KubeJS 在
         * {@code String}、{@link Attribute} 和 {@link Supplier} 重载之间产生歧义。
         * 字符串属性会延迟到升级实际应用时解析，因此可以安全引用自定义属性。</p>
         *
         * @param attribute 属性对象、属性注册表标识字符串，或返回属性的 {@link Supplier}
         * @param amount 属性修饰数值
         * @param operation {@link AttributeModifier.Operation} 或其名称字符串
         * @return 当前构建器
         */
        public Builder attribute(Object attribute, double amount, Object operation) {
            AttributeModifier.Operation resolvedOperation = resolveOperation(operation);
            Supplier<Attribute> supplier = resolveAttributeSupplier(attribute);
            attributes.add(new AttributeDefinition(supplier, amount, resolvedOperation));
            return this;
        }

        /** 解析属性修饰运算类型。 */
        private static AttributeModifier.Operation resolveOperation(Object operation) {
            if (operation instanceof AttributeModifier.Operation resolved) {
                return resolved;
            }
            if (operation instanceof String name) {
                return AttributeModifierHelper.parseOperation(name);
            }
            throw new IllegalArgumentException(
                    "operation must be an AttributeModifier.Operation or String");
        }

        /** 创建延迟解析属性的提供器。 */
        private static Supplier<Attribute> resolveAttributeSupplier(Object attribute) {
            if (attribute instanceof Attribute resolved) {
                return () -> resolved;
            }
            if (attribute instanceof Supplier<?> supplier) {
                return () -> {
                    Object resolved = supplier.get();
                    if (!(resolved instanceof Attribute)) {
                        throw new IllegalArgumentException(
                                "attribute supplier must return an Attribute");
                    }
                    return (Attribute) resolved;
                };
            }
            if (attribute instanceof String attributeId) {
                return () -> AttributeModifierHelper.resolveAttribute(attributeId);
            }
            throw new IllegalArgumentException(
                    "attribute must be an Attribute, attribute id String, or Supplier");
        }

        /**
         * 添加一个药水效果。
         *
         * @param effect 要添加的药水效果
         * @return 当前构建器
         */
        public Builder effect(MobEffectInstance effect) {
            effects.add(effect);
            return this;
        }

        /**
         * 通过注册表标识添加药水效果，适合 KubeJS 调用。
         *
         * @param effectId 药水效果注册表标识，例如 {@code minecraft:speed}
         * @param durationTicks 持续时间，单位为 tick
         * @param amplifier 效果等级，0 表示一级
         * @return 当前构建器
         */
        public Builder effect(String effectId, int durationTicks, int amplifier) {
            return effect(effectId, durationTicks, amplifier, false, true, true);
        }

        /**
         * 通过注册表标识添加可控制显示参数的药水效果，适合 KubeJS 调用。
         *
         * @param effectId 药水效果注册表标识
         * @param durationTicks 持续时间，单位为 tick
         * @param amplifier 效果等级，0 表示一级
         * @param ambient 是否使用环境效果显示
         * @param showParticles 是否显示粒子
         * @param showIcon 是否显示图标
         * @return 当前构建器
         */
        public Builder effect(String effectId, int durationTicks, int amplifier,
                              boolean ambient, boolean showParticles, boolean showIcon) {
            ResourceLocation id = ResourceLocation.tryParse(effectId);
            var effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (effect == null) {
                throw new IllegalArgumentException("Unknown mob effect: " + effectId);
            }
            if (durationTicks < 0) {
                throw new IllegalArgumentException("durationTicks must be non-negative");
            }
            if (amplifier < 0) {
                throw new IllegalArgumentException("amplifier must be non-negative");
            }
            return effect(new MobEffectInstance(effect, durationTicks, amplifier,
                    ambient, showParticles, showIcon));
        }

        /**
         * 设置手机升级的 Curios tick 回调。
         *
        * @param curioTick Curios tick 回调
        * @return 当前构建器
        */
        public Builder curioTick(CurioTick curioTick) {
            this.curioTick = curioTick;
            return this;
        }

        /**
         * 为手机升级绑定一个按键，并设置按下按键时执行的服务端回调。
         *
         * @param key 按键码，例如 {@code InputConstants.KEY_Z}
         * @param keyActivate 按键激活回调
         * @return 当前构建器
         * @throws IllegalArgumentException 当按键码小于0时抛出
         * @throws NullPointerException 当回调为 {@code null} 时抛出
         */
        public Builder bindKeyActivate(int key, KeyActivate keyActivate) {
            if (key < 0) {
                throw new IllegalArgumentException("key must be non-negative");
            }
            this.activationKey = key;
            this.keyActivate = Objects.requireNonNull(keyActivate, "keyActivate");
            return this;
        }

        /**
         * 设置手机升级的护甲插槽修改回调。
         *
         * @param armorSoltModifier 护甲插槽修改回调
         * @return 当前构建器
         */
        public Builder armorSoltModifier(ArmorSoltModifier armorSoltModifier) {
            this.armorSoltModifier = armorSoltModifier;
            return this;
        }

        /**
         * 通过字符串运算类型添加 TACZ 枪械扩容。
         *
         * @param amount 扩容数值；乘法模式表示比例，加法模式表示直接增加的弹药数量
         * @param operation 运算类型名称：{@code MULTIPLY} 或 {@code ADDITION}
         * @param gunTypes 可选的枪械类型；为空时支持全部枪械
         * @return 当前构建器
         */
        public Builder ammoExpansion(double amount, Operation operation,
                                     String... gunTypes) {
            AmmoExpansion expansion = new AmmoExpansion(amount, operation, gunTypes);
            ammoExpansions.add(expansion);
            return this;
        }

        /**
         * 以指定概率穿透本模组的伤害减免和护甲吸收。
         *
         * @param probability 穿透概率，范围为 {@code 0.0~1.0}
         * @return 当前构建器
         * @throws IllegalArgumentException 当概率不在合法范围内时抛出
         */
        public Builder pierce(double probability) {
            validateProbability(probability);
            this.pierceProbability = probability;
            return this;
        }

        /**
         * 校验概率参数。
         *
         * @param probability 待校验的概率
         * @throws IllegalArgumentException 当概率不是 {@code 0.0~1.0} 之间的有限数值时抛出
         */
        private static void validateProbability(double probability) {
            if (!Double.isFinite(probability) || probability < 0.0D || probability > 1.0D) {
                throw new IllegalArgumentException("probability must be between 0 and 1");
            }
        }

        /**
         * 对指定伤害类型以指定概率免疫。
         *
         * @param damageType 要匹配的伤害类型标签
         * @param probability 免疫概率，范围为 {@code 0.0~1.0}
         * @return 当前构建器
         */
        public Builder immune(TagKey<DamageType> damageType, double probability) {
            return immune(source -> source.is(damageType), probability);
        }

        /**
         * 通过伤害类型标签标识添加伤害免疫，适合 KubeJS 调用。
         *
         * @param damageTypeId 伤害类型标签标识，例如 {@code minecraft:is_fire}
         * @param probability 免疫概率，范围为 {@code 0.0~1.0}
         * @return 当前构建器
         */
        public Builder immune(String damageTypeId, double probability) {
            ResourceLocation id = ResourceLocation.tryParse(damageTypeId);
            if (id == null) {
                throw new IllegalArgumentException("Unknown damage type tag: " + damageTypeId);
            }
            return immune(TagKey.create(Registries.DAMAGE_TYPE, id), probability);
        }

        /**
         * 对自定义伤害判定以指定概率免疫。
         *
         * @param damageType 伤害来源判定器
         * @param probability 免疫概率，范围为 {@code 0.0~1.0}
         * @return 当前构建器
         */
        public Builder immune(Predicate<DamageSource> damageType, double probability) {
            immunities.add(new DamageImmunity(damageType, probability));
            return this;
        }

        /**
         * 为任意 Forge 事件类型添加可复用的代理回调。
         *
         * @param eventType 要代理的 Forge 事件类型
         * @param callback 事件回调
         * @param <E> 事件类型
         * @return 当前构建器
         */
        public <E extends Event> Builder addProxyEvent(Class<E> eventType, ProxyEvent<E> callback) {
            proxyEvents.add(new ProxyEventDefinition<>(eventType, callback));
            return this;
        }

        /**
         * 通过事件类的完整类名添加事件代理，适合 KubeJS 调用。
         *
         * <p>例如：{@code net.minecraftforge.event.entity.living.LivingHealEvent}。</p>
         *
         * @param eventClassName Forge 事件的完整类名
         * @param callback 事件回调，参数为事件对象和手机持有者
         * @return 当前构建器
         * @throws IllegalArgumentException 当类不存在或不是 Forge 事件时抛出
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder addProxyEvent(String eventClassName, ProxyEvent callback) {
            try {
                Class<?> eventClass = Class.forName(eventClassName);
                if (!Event.class.isAssignableFrom(eventClass)) {
                    throw new IllegalArgumentException("Class is not a Forge event: " + eventClassName);
                }
                return addProxyEvent((Class<? extends Event>) eventClass, callback);
            } catch (ClassNotFoundException exception) {
                throw new IllegalArgumentException("Unknown Forge event: " + eventClassName, exception);
            }
        }

        /**
         * 构建不可变的手机升级动作。
         *
         * @return 构建完成的手机升级动作
         */
        public PhoneUpgradeAction build() {
            return new PhoneUpgradeAction(attributes, effects, curioTick,
                    armorSoltModifier, proxyEvents, pierceProbability, immunities, ammoExpansions,
                    activationKey, keyActivate);
        }
    }

    /** TACZ 枪械扩容的计算方式。 */
    public enum Operation {
        MULTIPLY,
        ADDITION
    }

}
