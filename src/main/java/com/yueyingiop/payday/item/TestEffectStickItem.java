package com.yueyingiop.payday.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Temporary test stick that applies effects stored in its item NBT. */
public class TestEffectStickItem extends Item {
    public static final String EFFECTS_TAG = "PayDayEffects";
    private static final String ID_TAG = "Id";
    private static final String DURATION_TAG = "Duration";
    private static final String AMPLIFIER_TAG = "Amplifier";

    public TestEffectStickItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static List<MobEffectInstance> getEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (!stack.hasTag()) {
            return effects;
        }
        ListTag list = stack.getTag().getList(EFFECTS_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID_TAG));
            MobEffect effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (effect == null) {
                continue;
            }
            effects.add(new MobEffectInstance(effect,
                    Math.max(1, tag.getInt(DURATION_TAG)),
                    Math.max(0, tag.getInt(AMPLIFIER_TAG)),
                    tag.getBoolean("Ambient"),
                    tag.getBoolean("Visible"),
                    tag.getBoolean("ShowIcon")));
        }
        return effects;
    }

    public static void applyEffects(ItemStack stack, LivingEntity target) {
        for (MobEffectInstance effect : getEffects(stack)) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        List<MobEffectInstance> effects = getEffects(stack);
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.payday.test_stick.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.payday.test_stick.effects")
                .withStyle(ChatFormatting.GRAY));
        for (MobEffectInstance effect : effects) {
            tooltip.add(Component.literal("  ")
                    .append(effect.getEffect().getDisplayName())
                    .append(" ")
                    .append(Component.literal("级" + (effect.getAmplifier() + 1)
                            + "，" + effect.getDuration() + " ticks"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
