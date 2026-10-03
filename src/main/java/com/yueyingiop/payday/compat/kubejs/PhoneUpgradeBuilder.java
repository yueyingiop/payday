package com.yueyingiop.payday.compat.kubejs;

import com.yueyingiop.payday.api.KeyActivate;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.api.phone.ArmorSoltModifier;
import com.yueyingiop.payday.api.phone.CurioTick;
import com.yueyingiop.payday.api.phone.ProxyEvent;
import com.yueyingiop.payday.item.PhoneUpgradeItem;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * KubeJS 用于创建手机升级物品的链式 Builder。
 *
 * <p>该 Builder 同时负责配置手机升级物品的元数据和
 * {@link PhoneUpgradeAction} 的实际效果。普通 ItemBuilder 方法，例如
 * {@code texture}、{@code rarity} 和 {@code maxStackSize}，也可以继续链式调用。</p>
 */
public class PhoneUpgradeBuilder extends ItemBuilder {
    private PhoneUpgradeItem.Category category = PhoneUpgradeItem.Category.SPECIAL;
    private String series = "custom";
    private int level = 1;
    private boolean prerequisite;
    private boolean prerequisiteExplicit;
    private String effectKey;
    private int textureColor = 0xFFFFFFFF;
    private boolean maxStackSizeExplicit;
    private final PhoneUpgradeAction.Builder actionBuilder = PhoneUpgradeAction.builder();
    private PhoneUpgradeAction action;

    /**
     * 创建一个手机升级物品 Builder。
     *
     * @param id 要注册的物品标识
     */
    public PhoneUpgradeBuilder(ResourceLocation id) {
        super(id);
        this.maxStackSize = 1;
        this.effectKey = id.getPath();
    }

    /**
     * 设置手机升级物品的最大堆叠数量。
     *
     * @param maxStackSize 最大堆叠数量
     * @return 当前 Builder
     */
    @Override
    public PhoneUpgradeBuilder maxStackSize(int maxStackSize) {
        super.maxStackSize(maxStackSize);
        this.maxStackSizeExplicit = true;
        return this;
    }

    /**
     * 设置手机升级分类。
     *
     * @param category 分类名称，支持 {@code BASIC}、{@code SPECIAL}，不区分大小写
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder category(String category) {
        try {
            this.category = PhoneUpgradeItem.Category.valueOf(category.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown phone upgrade category: " + category,
                    exception);
        }
        if (!maxStackSizeExplicit) {
            this.maxStackSize = this.category == PhoneUpgradeItem.Category.SPECIAL ? 1 : 16;
        }
        return this;
    }

    /**
     * 设置手机升级系列。
     *
     * @param series 系列标识，例如 {@code muscle}、{@code assassin}
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder series(String series) {
        if (series == null || series.isBlank()) {
            throw new IllegalArgumentException("series must not be blank");
        }
        this.series = series;
        return this;
    }

    /**
     * 设置手机升级等级。
     *
     * @param level 升级等级，必须大于或等于 1
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder level(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        this.level = level;
        if (!prerequisiteExplicit) {
            this.prerequisite = level > 1;
        }
        return this;
    }

    /**
     * 设置该升级是否需要前置升级。
     *
     * @param prerequisite 是否需要前置升级
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder prerequisite(boolean prerequisite) {
        this.prerequisite = prerequisite;
        this.prerequisiteExplicit = true;
        return this;
    }

    /**
     * 设置该升级的效果键。
     *
     * <p>效果键用于物品 tooltip 的翻译键
     * {@code tooltip.payday.upgrade_effect.<effectKey>}。</p>
     *
     * @param effectKey 效果键
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder effectKey(String effectKey) {
        if (effectKey == null || effectKey.isBlank()) {
            throw new IllegalArgumentException("effectKey must not be blank");
        }
        this.effectKey = effectKey;
        return this;
    }

    /**
     * 设置手机升级物品贴图的 RGBA 颜色。默认颜色为不透明白色，即不改变原始贴图。
     *
     * @param r 红色分量，范围 0~255
     * @param g 绿色分量，范围 0~255
     * @param b 蓝色分量，范围 0~255
     * @param a 不透明度分量，范围 0~255
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder textureColor(int r, int g, int b, int a) {
        this.textureColor = PhoneUpgradeItem.textureColor(r, g, b, a);
        return this;
    }

    /**
     * 设置手机升级物品贴图的 HSL 颜色。h 使用 0~360 度，s 和 l 使用 0~1。
     *
     * @param h 色相角度
     * @param s 饱和度
     * @param l 亮度
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder textureColor(double h, double s, double l) {
        this.textureColor = PhoneUpgradeItem.textureColor(h, s, l);
        return this;
    }

    /**
     * 直接设置已经构建完成的手机升级动作。
     *
     * @param action 手机升级动作
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder action(PhoneUpgradeAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.action = action;
        return this;
    }

    /**
     * 使用 {@link PhoneUpgradeAction.Builder} 生成手机升级动作。
     *
     * @param builder 手机升级动作 Builder
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder action(PhoneUpgradeAction.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder must not be null");
        }
        this.action = builder.build();
        return this;
    }

    /**
     * 添加属性修饰符。
     *
     * @param attributeId 属性对象或属性注册表标识字符串
     * @param amount 属性修饰数值
     * @param operation 运算类型对象或名称字符串，例如 {@code ADDITION}、{@code MULTIPLY_TOTAL}
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder attribute(Object attributeId, double amount, Object operation) {
        actionBuilder.attribute(attributeId, amount, operation);
        return this;
    }

    /**
     * 添加药水效果。
     *
     * @param effectId 药水效果注册表标识
     * @param durationTicks 持续时间，单位为 tick
     * @param amplifier 效果等级，0 表示一级
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder effect(String effectId, int durationTicks, int amplifier) {
        actionBuilder.effect(effectId, durationTicks, amplifier);
        return this;
    }

    /**
     * 添加药水效果，并控制其显示参数。
     *
     * @param effectId 药水效果注册表标识
     * @param durationTicks 持续时间，单位为 tick
     * @param amplifier 效果等级，0 表示一级
     * @param ambient 是否使用环境效果显示
     * @param showParticles 是否显示粒子
     * @param showIcon 是否显示图标
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder effect(String effectId, int durationTicks, int amplifier,
                                      boolean ambient, boolean showParticles, boolean showIcon) {
        actionBuilder.effect(effectId, durationTicks, amplifier,
                ambient, showParticles, showIcon);
        return this;
    }

    /**
     * 添加 TACZ 枪械弹匣扩容。
     *
     * @param amount 扩容数值；乘法模式表示比例，加法模式表示直接增加的弹药数量
     * @param operation {@code MULTIPLY} 或 {@code ADDITION}
     * @param gunTypes 可选枪械类型；不传时支持全部枪械
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder ammoExpansion(double amount, PhoneUpgradeAction.Operation operation,
                                             String... gunTypes) {
        actionBuilder.ammoExpansion(amount, operation, gunTypes);
        return this;
    }

    /**
     * 设置穿透本模组伤害减免和护甲吸收的概率。
     *
     * @param probability 穿透概率，范围为 {@code 0.0~1.0}
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder pierce(double probability) {
        actionBuilder.pierce(probability);
        return this;
    }

    /**
     * 添加伤害类型标签免疫。
     *
     * @param damageTypeTagId 伤害类型标签标识，例如 {@code minecraft:is_fire}
     * @param probability 免疫概率，范围为 {@code 0.0~1.0}
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder immune(String damageTypeTagId, double probability) {
        actionBuilder.immune(damageTypeTagId, probability);
        return this;
    }

    /**
     * 设置手机升级的 Curios tick 回调。
     *
     * @param curioTick Curios tick 回调
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder curioTick(CurioTick curioTick) {
        actionBuilder.curioTick(curioTick);
        return this;
    }

    /**
     * 为手机升级绑定按键激活回调。
     *
     * @param key 按键码
     * @param keyActivate 按键激活回调
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder bindKeyActivate(int key, KeyActivate keyActivate) {
        actionBuilder.bindKeyActivate(key, keyActivate);
        return this;
    }

    /**
     * 设置手机升级的护甲插槽修改回调。
     *
     * @param armorSoltModifier 护甲插槽修改回调
     * @return 当前 Builder
     */
    public PhoneUpgradeBuilder armorSoltModifier(ArmorSoltModifier armorSoltModifier) {
        actionBuilder.armorSoltModifier(armorSoltModifier);
        return this;
    }

    /**
     * 添加 Forge 事件代理回调。
     *
     * @param eventClassName Forge 事件完整类名
     * @param callback 事件代理回调
     * @return 当前 Builder
     */
    @SuppressWarnings("rawtypes")
    public PhoneUpgradeBuilder addProxyEvent(String eventClassName, ProxyEvent callback) {
        actionBuilder.addProxyEvent(eventClassName, callback);
        return this;
    }

    /**
     * 创建注册用的手机升级物品。
     *
     * @return 根据当前 Builder 配置创建的手机升级物品
     */
    @Override
    public Item createObject() {
        PhoneUpgradeAction resolvedAction = action != null ? action : actionBuilder.build();
        return new PhoneUpgradeItem(category, series, level, prerequisite, effectKey,
                resolvedAction, createItemProperties(), textureColor);
    }
}
