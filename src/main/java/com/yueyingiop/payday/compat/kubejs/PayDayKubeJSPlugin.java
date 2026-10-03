package com.yueyingiop.payday.compat.kubejs;

import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.ExposureRiskModifier;
import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.BindingsEvent;

/** PayDay 的 KubeJS 扩展入口。 */
public class PayDayKubeJSPlugin extends KubeJSPlugin {
    /**
     * 初始化界面或插件。
     */
    @Override
    public void init() {
        RegistryInfo.ITEM.addType("armor_curio", ArmorCurioBuilder.class, ArmorCurioBuilder::new);
        RegistryInfo.ITEM.addType("armor_slot", ArmorSlotBuilder.class, ArmorSlotBuilder::new);
        RegistryInfo.ITEM.addType("payday:armor_curio", ArmorCurioBuilder.class,
                ArmorCurioBuilder::new);
        RegistryInfo.ITEM.addType("payday:armor_slot", ArmorSlotBuilder.class,
                ArmorSlotBuilder::new);
        RegistryInfo.ITEM.addType("phone_upgrade", PhoneUpgradeBuilder.class,
                PhoneUpgradeBuilder::new);
        RegistryInfo.ITEM.addType("payday:phone_upgrade", PhoneUpgradeBuilder.class,
                PhoneUpgradeBuilder::new);
    }

    /**
     * 向 KubeJS 注册 Payday 脚本绑定。
     * @param event 事件。
     */
    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("ArmorModifier", ArmorModifier.class);
        event.add("ExposureRiskModifier", ExposureRiskModifier.class);
        event.add("PhoneUpgradeAction", com.yueyingiop.payday.api.PhoneUpgradeAction.class);
        event.add("PhoneUpgradeBuilder", PhoneUpgradeBuilder.class);
        event.add("PhoneUpgradeHelper", PhoneUpgradeHelper.class);
        event.add("PhoneUpgradeHealingMode", PhoneUpgradeHelper.HealingMode.class);
    }
}
