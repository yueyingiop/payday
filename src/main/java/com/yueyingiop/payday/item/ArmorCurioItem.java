package com.yueyingiop.payday.item;

import com.yueyingiop.payday.event.server.ArmorCurioEvents;
import com.yueyingiop.payday.api.ArmorModifierRegistry;
import com.yueyingiop.payday.tooltip.ArmorSlotTooltip;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ArmorCurioItem extends Item implements ICurioItem, GeoItem {
    private static final String UPGRADES = "payday_upgrades";
    private final int upgradeSlots;
    private final List<AttributeDefinition> attributes;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public ArmorCurioItem(int upgradeSlots) {
        this(upgradeSlots, List.of(), new Item.Properties());
    }

    public ArmorCurioItem(int upgradeSlots, Item.Properties properties) {
        this(upgradeSlots, List.of(), properties);
    }

    public ArmorCurioItem(int upgradeSlots, List<AttributeDefinition> attributes,
                          Item.Properties properties) {
        super(properties.stacksTo(1));
        if (upgradeSlots < 0) {
            throw new IllegalArgumentException("upgradeSlots must be non-negative");
        }
        this.upgradeSlots = upgradeSlots;
        this.attributes = List.copyOf(attributes);
        GeoItem.registerSyncedAnimatable(this);
    }

    /**
     * 获取升级插槽数量。
     * @return 对应的执行结果。
     */
    public int upgradeSlots() { return upgradeSlots; }

    /** 获取护甲饰品自身提供的 Java 属性修饰符。 */
    public List<AttributeDefinition> attributes() { return attributes; }

    /**
     * 读取护甲物品内保存的升级列表。
     * @param armorStack 护甲物品。
     * @return 对应的执行结果。
     */
    public java.util.List<ItemStack> getUpgradeStacks(ItemStack armorStack) {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        if (!armorStack.hasTag()) return result;
        ListTag list = armorStack.getTag().getList(UPGRADES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack upgrade = ItemStack.of(list.getCompound(i));
            if (upgrade.getItem() instanceof ArmorSlotItem) {
                ArmorSlotItem.setUpgradeIndex(upgrade, i);
            }
            result.add(upgrade);
        }
        return result;
    }

    /**
     * 向护甲物品添加升级并更新其 NBT。
     * @param armorStack 护甲物品。
     * @param upgradeStack 升级物品。
     * @return 对应的执行结果。
     */
    public boolean addUpgrade(ItemStack armorStack, ItemStack upgradeStack) {
        if (!(upgradeStack.getItem() instanceof ArmorSlotItem)) return false;
        java.util.List<ItemStack> upgrades = getUpgradeStacks(armorStack);
        if (upgrades.size() >= upgradeSlots()) return false;
        ItemStack copy = upgradeStack.copy();
        copy.setCount(1);
        upgrades.add(copy);
        setUpgradeStacks(armorStack, upgrades);
        return true;
    }

    /**
     * 将升级列表写入护甲物品 NBT。
     * @param armorStack 护甲物品。
     * @param upgrades 对应参数。
     */
    public void setUpgradeStacks(ItemStack armorStack, java.util.List<ItemStack> upgrades) {
        ListTag list = new ListTag();
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack upgrade = upgrades.get(i);
            CompoundTag tag = upgrade.save(new CompoundTag());
            if (upgrade.getItem() instanceof ArmorSlotItem) {
                tag.putInt(ArmorSlotItem.UPGRADE_INDEX, i);
            }
            list.add(tag);
        }
        armorStack.getOrCreateTag().put(UPGRADES, list);
    }

    /**
     * 向物品提示添加护甲信息。
     * @param stack 物品堆。
     * @param level 世界。
     * @param tooltip 对应参数。
     * @param flag 对应参数。
     */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int loaded = 0;
        for (ItemStack upgrade : getUpgradeStacks(stack)) {
            if (upgrade.getItem() instanceof ArmorSlotItem) {
                loaded++;
            }
        }
        tooltip.add(Component.translatable("tooltip.payday.armor_slots", loaded, upgradeSlots()));
    }

    /**
     * 创建护甲物品的自定义提示图像。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<ItemStack> loaded = new ArrayList<>();
        for (ItemStack upgrade : getUpgradeStacks(stack)) {
            if (upgrade.getItem() instanceof ArmorSlotItem) {
                loaded.add(upgrade.copyWithCount(1));
            }
        }
        return loaded.isEmpty() ? Optional.empty() : Optional.of(new ArmorSlotTooltip(loaded));
    }
    /**
     * 组合护甲本体及升级提供的属性修饰符。
     * @param context 上下文。
     * @param uuid UUID。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override 
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> out = ImmutableMultimap.builder();

        for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
            AttributeDefinition definition = attributes.get(attributeIndex);
            String attributeId = attributeId(definition.attribute());
            String operation = definition.operation().name();
            String key = attributeId + "|" + operation;
            if (ArmorModifierRegistry.overrides(this, key)) {
                continue;
            }
            UUID modifierId = UUID.nameUUIDFromBytes((uuid + "|payday_armor|" + attributeIndex
                    + "|" + key)
                    .getBytes(StandardCharsets.UTF_8));
            out.put(definition.attribute(), new AttributeModifier(modifierId,
                    "payday.armor." + attributeId, definition.value(), definition.operation()));
        }

        List<ItemStack> upgrades = getUpgradeStacks(stack);
        for (int upgradeIndex = 0; upgradeIndex < upgrades.size(); upgradeIndex++) {
            ItemStack upgradeStack = upgrades.get(upgradeIndex);
            if (!(upgradeStack.getItem() instanceof ArmorSlotItem armorSlot)) {
                continue;
            }

            List<ArmorSlotItem.ProvidedAttribute> attributes = armorSlot.providedAttributes();
            for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
                ArmorSlotItem.ProvidedAttribute provided = attributes.get(attributeIndex);
                UUID modifierId = UUID.nameUUIDFromBytes((uuid + "|payday_slot|" + upgradeIndex
                        + "|" + attributeIndex + "|" + provided.attributeId()
                        + "|" + provided.operation().name()).getBytes(StandardCharsets.UTF_8));
                out.put(provided.attribute(), new AttributeModifier(modifierId,
                        "payday.slot." + upgradeIndex + "." + attributeIndex,
                        provided.value(), provided.operation()));
            }
        }
        for (var entry : ArmorModifierRegistry.entries(this)) {
            try {
                String attributeId = attributeId(entry.attribute());
                String type = entry.type().toUpperCase(java.util.Locale.ROOT);
                UUID modifierId = UUID.nameUUIDFromBytes((uuid + "|payday_custom|" + attributeId
                        + "|" + type).getBytes(StandardCharsets.UTF_8));
                out.put(entry.attribute(), new AttributeModifier(modifierId,
                        "payday.custom." + attributeId + "." + type,
                        entry.value(), AttributeModifier.Operation.valueOf(type)));
            } catch (IllegalArgumentException ignored) { }
        }
        return out.build();
    }

    /**
     * 获取属性的注册表标识。
     * @param attribute 属性。
     * @return 对应的执行结果。
     */
    private static String attributeId(Attribute attribute) {
        var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id == null ? attribute.getDescriptionId() : id.toString();
    }

    /** 判断护甲饰品自身是否定义了指定属性和操作类型。 */
    public boolean hasBaseAttribute(String key) {
        for (AttributeDefinition definition : attributes) {
            if ((attributeId(definition.attribute()) + "|" + definition.operation().name()).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** 在佩戴期间持续刷新护甲插槽提供的永久药水效果。 */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide) {
            return;
        }

        Map<MobEffect, ArmorSlotItem.ProvidedEffect> effects = new HashMap<>();
        for (ItemStack upgradeStack : getUpgradeStacks(stack)) {
            if (!(upgradeStack.getItem() instanceof ArmorSlotItem armorSlot)) {
                continue;
            }

            for (ArmorSlotItem.ProvidedEffect provided : armorSlot.providedEffects()) {
                ArmorSlotItem.ProvidedEffect previous = effects.get(provided.effect());
                if (previous == null || provided.amplifier() > previous.amplifier()) {
                    effects.put(provided.effect(), provided);
                }
            }
        }

        for (ArmorSlotItem.ProvidedEffect provided : effects.values()) {
            slotContext.entity().addEffect(new MobEffectInstance(provided.effect(), 5,
                    provided.amplifier(), provided.ambient(), provided.showParticles(),
                    provided.showIcon()));
        }
    }

    /**
     * 检查物品是否可放入指定饰品槽。
     * @param slotContext 槽位上下文。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "armor".equals(slotContext.identifier());
    }

    // @Override
    // public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    //     ICurioItem.super.onEquip(slotContext, prevStack, stack);
    // }

    /**
     * 检查物品是否可通过使用快捷装备。
     * @param slotContext 槽位上下文。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return "armor".equals(slotContext.identifier())
                && !ArmorCurioEvents.isArmorChangeLocked(slotContext.entity(),
                slotContext.identifier(), slotContext.index());
    }

    /**
     * 注册护甲饰品的动画控制器；静态模型不需要额外控制器。
     * @param controllers 动画控制器。
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 当前护甲模型为静态模型，因此不需要动画控制器。
    }

    /**
     * 获取护甲饰品的动画实例缓存。
     * @return 对应的执行结果。
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /** 护甲饰品自身提供的属性修饰符定义。 */
    public record AttributeDefinition(Attribute attribute, AttributeModifier.Operation operation,
                                      double value) {
    }
}
