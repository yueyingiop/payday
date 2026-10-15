package com.yueyingiop.payday.api;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.CooldownHud.Type;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * 护甲饰品和手机升级共用的护甲修改上下文。
 *
 * <p>旧的字符串构造方法继续服务于 KubeJS 的静态属性修改；带上下文的构造方法
 * 用于手机升级，为每个实体单独记录护甲上限、恢复间隔和损坏无敌效果。</p>
 */
public class ArmorModifier {
    public static final String FULL_IMMUNITY_END = "payday_full_damage_immunity_end";
    public static final String FULL_IMMUNITY_COOLDOWN_END = "payday_full_damage_immunity_cooldown_end";
    private static final String BREAK_IMMUNITY_HUD_ID = "payday_armor_break_immunity";
    private static final String BREAK_IMMUNITY_TEXTURE =
            "payday:item/phone_upgrade/generated/special_armorer_4";
    private static final String ANARCHIST_RECOVERY_HUD_ID = "payday_anarchist_recovery";
    private static final String ANARCHIST_RECOVERY_TEXTURE =
            "payday:item/phone_upgrade/generated/special_anarchist_1";

    private static final Map<LivingEntity, RuntimeState> ACTIVE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final ArmorCurioItem item;
    private final ItemStack phoneStack;
    private final ItemStack armorStack;
    private final LivingEntity owner;
    private final RuntimeState runtimeState;

    /** 创建一个用于 KubeJS 静态属性修改的护甲修改器。 */
    public ArmorModifier(String itemId) {
        Item found = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (!(found instanceof ArmorCurioItem armor)) {
            PayDay.LOGGER.error("{} is not a registered ArmorCurioItem", itemId);
            throw new IllegalArgumentException(itemId);
        }
        this.item = armor;
        this.phoneStack = ItemStack.EMPTY;
        this.armorStack = ItemStack.EMPTY;
        this.owner = null;
        this.runtimeState = null;
    }

    /** 创建一个带手机、护甲饰品和代理实体上下文的运行时修改器。 */
    public ArmorModifier(ItemStack phoneStack, ItemStack armorStack,
                         ArmorCurioItem armorCurio, LivingEntity owner) {
        this.item = armorCurio;
        this.phoneStack = phoneStack;
        this.armorStack = armorStack;
        this.owner = owner;
        this.runtimeState = ACTIVE.computeIfAbsent(owner, ignored -> new RuntimeState());
    }

    /** 开始重新计算一个实体当前装备的手机护甲效果。 */
    public static void begin(LivingEntity owner) {
        ACTIVE.put(owner, new RuntimeState());
    }

    /** 清除一个实体的手机护甲效果。 */
    public static void clear(LivingEntity owner) {
        ACTIVE.remove(owner);
    }

    public ArmorCurioItem armorCurio() {
        return item;
    }

    public ItemStack phoneStack() {
        return phoneStack;
    }

    public ItemStack armorStack() {
        return armorStack;
    }

    public LivingEntity owner() {
        return owner;
    }

    /** 获取当前护甲饰品中的护甲升级列表。 */
    public List<ItemStack> upgradeStacks() {
        if (armorStack.isEmpty()) {
            return List.of();
        }
        return item.getUpgradeStacks(armorStack);
    }

    /** 遍历当前护甲饰品中的所有 ArmorSlotItem。 */
    public ArmorModifier forEachArmorSlot(BiConsumer<ItemStack, ArmorSlotItem> callback) {
        for (ItemStack stack : upgradeStacks()) {
            if (stack.getItem() instanceof ArmorSlotItem armorSlot) {
                callback.accept(stack, armorSlot);
            }
        }
        return this;
    }

    /** 每个护甲升级插槽的护甲上限增加指定百分比。 */
    public ArmorModifier addArmorMaxMultiplier(double amount) {
        requireRuntimeContext();
        runtimeState.maxMultiplier.merge(armorStack, amount, Double::sum);
        return this;
    }

    /** 每个护甲定义的护甲上限额外增加固定数值。 */
    public ArmorModifier addArmorMax(double amount) {
        requireRuntimeContext();
        if (Double.isFinite(amount) && amount > 0.0D) {
            runtimeState.maxFlat.merge(armorStack, amount, Double::sum);
        }
        return this;
    }

    /** 将每个护甲升级插槽的恢复间隔缩短指定百分比。 */
    public ArmorModifier reduceRecoveryInterval(double reduction) {
        requireRuntimeContext();
        runtimeState.recoveryMultiplier.merge(armorStack,
                Math.max(0.0D, 1.0D - reduction), (oldValue, newValue) -> oldValue * newValue);
        return this;
    }

    /**
     * 启用无政府主义者的固定护甲恢复规则。
     *
     * <p>固定规则会覆盖所有普通恢复间隔修正：每个护甲饰品中的 ArmorSlotItem
     * 数量为 n 时，间隔为 (n + 1) * 0.25 秒，每次恢复最大护甲值的
     * 15% + n * 10%。</p>
     */
    public ArmorModifier enableAnarchistRecovery() {
        requireRuntimeContext();
        int slotCount = 0;
        for (ItemStack stack : upgradeStacks()) {
            if (stack.getItem() instanceof ArmorSlotItem) {
                slotCount++;
            }
        }
        runtimeState.fixedRecoverySeconds.put(armorStack, (slotCount + 1) * 0.25D);
        runtimeState.fixedRecoveryPercent.put(armorStack, 0.15D + slotCount * 0.10D);
        runtimeState.anarchistRecoveryHudTexture = ANARCHIST_RECOVERY_TEXTURE;
        return this;
    }

    /** 为护甲插槽注册“损坏后短暂无敌”的规则。 */
    public ArmorModifier breakImmunity(double durationSeconds, double cooldownSeconds) {
        return breakImmunity(durationSeconds, cooldownSeconds, BREAK_IMMUNITY_TEXTURE);
    }

    /**
     * 为护甲插槽注册“损坏后短暂无敌”的规则，并指定冷却 HUD 图标。
     *
     * @param durationSeconds 无敌持续时间，单位为秒
     * @param cooldownSeconds 冷却时间，单位为秒
     * @param hudTexture 冷却 HUD 图标纹理
     * @return 当前护甲修改器
     */
    public ArmorModifier breakImmunity(double durationSeconds, double cooldownSeconds,
                                       String hudTexture) {
        requireRuntimeContext();
        runtimeState.immunityDurationSeconds = Math.max(runtimeState.immunityDurationSeconds,
                durationSeconds);
        runtimeState.immunityCooldownSeconds = Math.max(runtimeState.immunityCooldownSeconds,
                cooldownSeconds);
        if (hudTexture != null && !hudTexture.isBlank()) {
            runtimeState.immunityHudTexture = hudTexture;
        }
        return this;
    }

    /** 获取实体当前指定护甲饰品的护甲上限倍率。 */
    public static double maxMultiplier(LivingEntity owner, ItemStack armorStack) {
        RuntimeState state = ACTIVE.get(owner);
        return state == null ? 1.0D : 1.0D + state.maxMultiplier.getOrDefault(armorStack, 0.0D);
    }

    /** 获取实体当前指定护甲饰品的恢复间隔倍率。 */
    public static double recoveryMultiplier(LivingEntity owner, ItemStack armorStack) {
        RuntimeState state = ACTIVE.get(owner);
        return state == null ? 1.0D : state.recoveryMultiplier.getOrDefault(armorStack, 1.0D);
    }

    /** 判断指定护甲饰品是否使用无政府主义者的固定恢复规则。 */
    public static boolean hasFixedRecovery(LivingEntity owner, ItemStack armorStack) {
        RuntimeState state = ACTIVE.get(owner);
        return state != null && state.fixedRecoverySeconds.containsKey(armorStack);
    }

    /** 返回手机护甲效果修正后的护甲定义最大值。 */
    public static double effectiveMax(LivingEntity owner, ItemStack armorStack,
                                      ArmorSlotItem armor, ItemStack stack,
                                      int definitionIndex) {
        return Math.max(0.0D, maximumBeforePermanent(owner, armorStack, armor, definitionIndex)
                - permanent(owner, armorStack, armor, stack, definitionIndex));
    }

    /** 返回永久损伤使用的基础护甲上限。永久损伤 NBT 按此上限保存。 */
    public static double maximumBeforePermanent(ArmorSlotItem armor, int definitionIndex) {
        return armor.maxArmor(definitionIndex);
    }

    /** 返回手机护甲效果修正后的永久损伤前上限，用于当前护甲 HUD 比例。 */
    public static double maximumBeforePermanent(LivingEntity owner, ItemStack armorStack,
                                                ArmorSlotItem armor, int definitionIndex) {
        RuntimeState state = ACTIVE.get(owner);
        double flat = state == null ? 0.0D : state.maxFlat.getOrDefault(armorStack, 0.0D);
        return armor.maxArmor(definitionIndex) * maxMultiplier(owner, armorStack) + flat;
    }

    /** 获取手机护甲效果修正后的永久损伤。 */
    public static double permanent(LivingEntity owner, ItemStack armorStack,
                                   ArmorSlotItem armor, ItemStack stack,
                                   int definitionIndex) {
        return Math.min(armor.storedPermanent(stack, definitionIndex),
                maximumBeforePermanent(owner, armorStack, armor, definitionIndex));
    }

    /** 判断指定护甲定义在手机护甲上限规则下是否损坏。 */
    public static boolean isBroken(LivingEntity owner, ItemStack armorStack,
                                   ArmorSlotItem armor, ItemStack stack,
                                   int definitionIndex) {
        return armor.kind(definitionIndex) != ArmorSlotItem.Kind.IMPACT
                && current(owner, armorStack, armor, stack, definitionIndex) <= 0.0D;
    }

    /** 获取手机护甲效果修正后的当前护甲值。 */
    public static double current(LivingEntity owner, ItemStack armorStack,
                                 ArmorSlotItem armor, ItemStack stack,
                                 int definitionIndex) {
        return armor.current(stack, definitionIndex,
                effectiveMax(owner, armorStack, armor, stack, definitionIndex));
    }

    /** 写入手机护甲效果修正后的当前护甲值。 */
    public static void setCurrent(LivingEntity owner, ItemStack armorStack,
                                  ArmorSlotItem armor, ItemStack stack,
                                  int definitionIndex, double value) {
        armor.setCurrent(stack, definitionIndex, value,
                effectiveMax(owner, armorStack, armor, stack, definitionIndex));
    }

    /** 增加永久损伤，同时保留手机护甲上限修正下允许存在的当前值。 */
    public static void addPermanent(LivingEntity owner, ItemStack armorStack,
                                    ArmorSlotItem armor, ItemStack stack,
                                    int definitionIndex, double value) {
        double before = current(owner, armorStack, armor, stack, definitionIndex);
        double maximum = maximumBeforePermanent(owner, armorStack, armor, definitionIndex);
        double damage = Math.min(maximum,
                permanent(owner, armorStack, armor, stack, definitionIndex)
                        + Math.max(0.0D, value));
        armor.setStoredPermanent(stack, definitionIndex, damage);
        setCurrent(owner, armorStack, armor, stack, definitionIndex, before);
    }

    /** 返回手机护甲效果修正后的护甲恢复间隔 tick。 */
    public static long recoveryTicks(LivingEntity owner, ItemStack armorStack,
                                     ArmorSlotItem armor, int definitionIndex) {
        RuntimeState state = ACTIVE.get(owner);
        if (state != null) {
            Double fixedSeconds = state.fixedRecoverySeconds.get(armorStack);
            if (fixedSeconds != null) {
                return Math.max(1L, Math.round(fixedSeconds * 20.0D));
            }
        }
        return Math.max(1L, Math.round(armor.recoverySeconds(definitionIndex) * 20.0D
                * recoveryMultiplier(owner, armorStack)));
    }

    /** 返回一次恢复应增加的护甲值。 */
    public static double recoveryAmount(LivingEntity owner, ItemStack armorStack,
                                        ArmorSlotItem armor, ItemStack stack,
                                        int definitionIndex) {
        double maximum = effectiveMax(owner, armorStack, armor, stack, definitionIndex);
        RuntimeState state = ACTIVE.get(owner);
        if (state != null) {
            Double fixedPercent = state.fixedRecoveryPercent.get(armorStack);
            if (fixedPercent != null) {
                return maximum * fixedPercent;
            }
        }
        return maximum / recoveryTicks(owner, armorStack, armor, definitionIndex);
    }

    /** 按当前手机护甲上下文恢复每个 ArmorDefinition 指定的护甲值。 */
    public ArmorModifier restoreArmor(double amount) {
        requireRuntimeContext();
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return this;
        }
        List<ItemStack> upgrades = upgradeStacks();
        for (ItemStack stack : upgrades) {
            if (!(stack.getItem() instanceof ArmorSlotItem armor)) {
                continue;
            }
            for (int definitionIndex = 0;
                 definitionIndex < armor.armorDefinitions().size(); definitionIndex++) {
                double current = current(owner, armorStack, armor, stack, definitionIndex);
                double maximum = effectiveMax(owner, armorStack, armor, stack, definitionIndex);
                setCurrent(owner, armorStack, armor, stack, definitionIndex,
                        Math.min(maximum, current + amount));
                if (current <= 0.0D && maximum > 0.0D) {
                    armor.clearBroken(stack, definitionIndex);
                }
            }
        }
        item.setUpgradeStacks(armorStack, upgrades);
        return this;
    }

    /** 护甲插槽损坏时尝试触发当前实体的无敌效果。 */
    public static void grantBreakImmunity(LivingEntity entity) {
        RuntimeState state = ACTIVE.get(entity);
        if (state == null || state.immunityDurationSeconds <= 0.0D) {
            return;
        }
        long now = entity.level().getGameTime();
        if (now < entity.getPersistentData().getLong(FULL_IMMUNITY_COOLDOWN_END)) {
            return;
        }
        entity.getPersistentData().putLong(FULL_IMMUNITY_END,
                now + Math.max(1L, Math.round(state.immunityDurationSeconds * 20.0D)));
        entity.getPersistentData().putLong(FULL_IMMUNITY_COOLDOWN_END,
                now + Math.max(1L, Math.round(state.immunityCooldownSeconds * 20.0D)));
        if (entity instanceof Player player && state.immunityCooldownSeconds > 0.0D) {
            CooldownHudManager.startTimer(player, BREAK_IMMUNITY_HUD_ID,
                    CooldownHud.builder()
                            .type(Type.TIMER)
                            .texture(state.immunityHudTexture)
                            .value(state.immunityCooldownSeconds)
                            .build());
        }
    }

    /**
     * 启动无政府主义者固定护甲恢复的计时型冷却 HUD。
     *
     * <p>只有实际恢复护甲后才会调用此方法，避免护甲效果每 tick 重算时重置 HUD。</p>
     *
     * @param owner 护甲拥有者
     * @param armorStack 发生恢复的护甲饰品
     */
    public static void startAnarchistRecoveryCooldown(LivingEntity owner, ItemStack armorStack) {
        if (!(owner instanceof Player player)) {
            return;
        }
        RuntimeState state = ACTIVE.get(owner);
        if (state == null) {
            return;
        }
        Double seconds = state.fixedRecoverySeconds.get(armorStack);
        if (seconds == null || seconds <= 0.0D) {
            return;
        }
        CooldownHudManager.startTimer(player, ANARCHIST_RECOVERY_HUD_ID,
                CooldownHud.builder()
                        .type(Type.TIMER)
                        .texture(state.anarchistRecoveryHudTexture)
                        .value(seconds)
                        .build());
    }

    /** 判断实体是否处于护甲损坏提供的全伤害免疫期间。 */
    public static boolean hasFullImmunity(LivingEntity entity) {
        return entity.getPersistentData().getLong(FULL_IMMUNITY_END)
                > entity.level().getGameTime();
    }

    /** 修改护甲属性修饰符。 */
    public ArmorModifier modifyAttribute(String attributeId, String type, float value) {
        return change(attributeId, type, value, true);
    }

    /** 添加护甲属性修饰符。 */
    public ArmorModifier addAttribute(String attributeId, String type, float value) {
        return change(attributeId, type, value, false);
    }

    /** 移除护甲属性修饰符。 */
    public ArmorModifier removeAttribute(String attributeId, String type) {
        return change(attributeId, type, 0, true);
    }

    private ArmorModifier change(String attributeId, String type, float value, boolean replace) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attributeId));
        if (attribute == null) {
            PayDay.LOGGER.error("Unknown attribute {}", attributeId);
            return this;
        }
        ArmorModifierRegistry.change(item, attributeId + "|" + type.toUpperCase(java.util.Locale.ROOT),
                attribute, type, value, replace);
        return this;
    }

    private void requireRuntimeContext() {
        if (runtimeState == null || owner == null || armorStack.isEmpty()) {
            throw new IllegalStateException("This ArmorModifier has no runtime armor context");
        }
    }

    private static final class RuntimeState {
        private final Map<ItemStack, Double> maxMultiplier = new IdentityHashMap<>();
        private final Map<ItemStack, Double> maxFlat = new IdentityHashMap<>();
        private final Map<ItemStack, Double> recoveryMultiplier = new IdentityHashMap<>();
        private final Map<ItemStack, Double> fixedRecoverySeconds = new IdentityHashMap<>();
        private final Map<ItemStack, Double> fixedRecoveryPercent = new IdentityHashMap<>();
        private double immunityDurationSeconds;
        private double immunityCooldownSeconds;
        private String immunityHudTexture = BREAK_IMMUNITY_TEXTURE;
        private String anarchistRecoveryHudTexture = ANARCHIST_RECOVERY_TEXTURE;
    }
}
