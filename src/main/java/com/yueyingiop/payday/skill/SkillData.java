package com.yueyingiop.payday.skill;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/** 技能名称、点数和文本效果。效果仅用于界面展示，不参与实际游戏逻辑。 */
public final class SkillData {
    private static final Map<String, List<Definition>> DATA = Map.ofEntries(
            Map.entry("mastermind/medical", List.of(
                    new Definition("skill.payday.node.mastermind.medical.1.name", 1, "skill.payday.node.mastermind.medical.1.base", 3, "skill.payday.node.mastermind.medical.1.mastery"),
                    new Definition("skill.payday.node.mastermind.medical.2.name", 2, "skill.payday.node.mastermind.medical.2.base", 4, "skill.payday.node.mastermind.medical.2.mastery"),
                    new Definition("skill.payday.node.mastermind.medical.3.name", 2, "skill.payday.node.mastermind.medical.3.base", 4, "skill.payday.node.mastermind.medical.3.mastery"),
                    new Definition("skill.payday.node.mastermind.medical.4.name", 3, "skill.payday.node.mastermind.medical.4.base", 6, "skill.payday.node.mastermind.medical.4.mastery"),
                    new Definition("skill.payday.node.mastermind.medical.5.name", 2, "skill.payday.node.mastermind.medical.5.base", 6, "skill.payday.node.mastermind.medical.5.mastery"),
                    new Definition("skill.payday.node.mastermind.medical.6.name", 4, "skill.payday.node.mastermind.medical.6.base", 8, "skill.payday.node.mastermind.medical.6.mastery")
            )),
            Map.entry("mastermind/control", List.of(
                    new Definition("skill.payday.node.mastermind.control.1.name", 1, "skill.payday.node.mastermind.control.1.base", 3, "skill.payday.node.mastermind.control.1.mastery"),
                    new Definition("skill.payday.node.mastermind.control.2.name", 2, "skill.payday.node.mastermind.control.2.base", 4, "skill.payday.node.mastermind.control.2.mastery"),
                    new Definition("skill.payday.node.mastermind.control.3.name", 2, "skill.payday.node.mastermind.control.3.base", 4, "skill.payday.node.mastermind.control.3.mastery"),
                    new Definition("skill.payday.node.mastermind.control.4.name", 3, "skill.payday.node.mastermind.control.4.base", 6, "skill.payday.node.mastermind.control.4.mastery"),
                    new Definition("skill.payday.node.mastermind.control.5.name", 3, "skill.payday.node.mastermind.control.5.base", 6, "skill.payday.node.mastermind.control.5.mastery"),
                    new Definition("skill.payday.node.mastermind.control.6.name", 4, "skill.payday.node.mastermind.control.6.base", 8, "skill.payday.node.mastermind.control.6.mastery")
            )),
            Map.entry("mastermind/sharpshooter", List.of(
                    new Definition("skill.payday.node.mastermind.sharpshooter.1.name", 1, "skill.payday.node.mastermind.sharpshooter.1.base", 3, "skill.payday.node.mastermind.sharpshooter.1.mastery"),
                    new Definition("skill.payday.node.mastermind.sharpshooter.2.name", 2, "skill.payday.node.mastermind.sharpshooter.2.base", 4, "skill.payday.node.mastermind.sharpshooter.2.mastery"),
                    new Definition("skill.payday.node.mastermind.sharpshooter.3.name", 2, "skill.payday.node.mastermind.sharpshooter.3.base", 4, "skill.payday.node.mastermind.sharpshooter.3.mastery"),
                    new Definition("skill.payday.node.mastermind.sharpshooter.4.name", 3, "skill.payday.node.mastermind.sharpshooter.4.base", 6, "skill.payday.node.mastermind.sharpshooter.4.mastery"),
                    new Definition("skill.payday.node.mastermind.sharpshooter.5.name", 3, "skill.payday.node.mastermind.sharpshooter.5.base", 6, "skill.payday.node.mastermind.sharpshooter.5.mastery"),
                    new Definition("skill.payday.node.mastermind.sharpshooter.6.name", 4, "skill.payday.node.mastermind.sharpshooter.6.base", 8, "skill.payday.node.mastermind.sharpshooter.6.mastery")
            )),
            Map.entry("mobster/shotgun", List.of(
                    new Definition("skill.payday.node.mobster.shotgun.1.name", 1, "skill.payday.node.mobster.shotgun.1.base", 3, "skill.payday.node.mobster.shotgun.1.mastery"),
                    new Definition("skill.payday.node.mobster.shotgun.2.name", 2, "skill.payday.node.mobster.shotgun.2.base", 4, "skill.payday.node.mobster.shotgun.2.mastery"),
                    new Definition("skill.payday.node.mobster.shotgun.3.name", 2, "skill.payday.node.mobster.shotgun.3.base", 4, "skill.payday.node.mobster.shotgun.3.mastery"),
                    new Definition("skill.payday.node.mobster.shotgun.4.name", 3, "skill.payday.node.mobster.shotgun.4.base", 6, "skill.payday.node.mobster.shotgun.4.mastery"),
                    new Definition("skill.payday.node.mobster.shotgun.5.name", 3, "skill.payday.node.mobster.shotgun.5.base", 6, "skill.payday.node.mobster.shotgun.5.mastery"),
                    new Definition("skill.payday.node.mobster.shotgun.6.name", 4, "skill.payday.node.mobster.shotgun.6.base", 8, "skill.payday.node.mobster.shotgun.6.mastery")
            )),
            Map.entry("mobster/heavy_armor", List.of(
                    new Definition("skill.payday.node.mobster.heavy_armor.1.name", 1, "skill.payday.node.mobster.heavy_armor.1.base", 3, "skill.payday.node.mobster.heavy_armor.1.mastery"),
                    new Definition("skill.payday.node.mobster.heavy_armor.2.name", 2, "skill.payday.node.mobster.heavy_armor.2.base", 4, "skill.payday.node.mobster.heavy_armor.2.mastery"),
                    new Definition("skill.payday.node.mobster.heavy_armor.3.name", 2, "skill.payday.node.mobster.heavy_armor.3.base", 4, "skill.payday.node.mobster.heavy_armor.3.mastery"),
                    new Definition("skill.payday.node.mobster.heavy_armor.4.name", 3, "skill.payday.node.mobster.heavy_armor.4.base", 6, "skill.payday.node.mobster.heavy_armor.4.mastery"),
                    new Definition("skill.payday.node.mobster.heavy_armor.5.name", 3, "skill.payday.node.mobster.heavy_armor.5.base", 6, "skill.payday.node.mobster.heavy_armor.5.mastery"),
                    new Definition("skill.payday.node.mobster.heavy_armor.6.name", 4, "skill.payday.node.mobster.heavy_armor.6.base", 8, "skill.payday.node.mobster.heavy_armor.6.mastery")
            )),
            Map.entry("mobster/ammo", List.of(
                    new Definition("skill.payday.node.mobster.ammo.1.name", 1, "skill.payday.node.mobster.ammo.1.base", 3, "skill.payday.node.mobster.ammo.1.mastery"),
                    new Definition("skill.payday.node.mobster.ammo.2.name", 2, "skill.payday.node.mobster.ammo.2.base", 4, "skill.payday.node.mobster.ammo.2.mastery"),
                    new Definition("skill.payday.node.mobster.ammo.3.name", 2, "skill.payday.node.mobster.ammo.3.base", 4, "skill.payday.node.mobster.ammo.3.mastery"),
                    new Definition("skill.payday.node.mobster.ammo.4.name", 3, "skill.payday.node.mobster.ammo.4.base", 6, "skill.payday.node.mobster.ammo.4.mastery"),
                    new Definition("skill.payday.node.mobster.ammo.5.name", 3, "skill.payday.node.mobster.ammo.5.base", 6, "skill.payday.node.mobster.ammo.5.mastery"),
                    new Definition("skill.payday.node.mobster.ammo.6.name", 4, "skill.payday.node.mobster.ammo.6.base", 8, "skill.payday.node.mobster.ammo.6.mastery")
            )),
            Map.entry("technician/engineering", List.of(
                    new Definition("skill.payday.node.technician.engineering.1.name", 1, "skill.payday.node.technician.engineering.1.base", 3, "skill.payday.node.technician.engineering.1.mastery"),
                    new Definition("skill.payday.node.technician.engineering.2.name", 2, "skill.payday.node.technician.engineering.2.base", 4, "skill.payday.node.technician.engineering.2.mastery"),
                    new Definition("skill.payday.node.technician.engineering.3.name", 2, "skill.payday.node.technician.engineering.3.base", 4, "skill.payday.node.technician.engineering.3.mastery"),
                    new Definition("skill.payday.node.technician.engineering.4.name", 3, "skill.payday.node.technician.engineering.4.base", 6, "skill.payday.node.technician.engineering.4.mastery"),
                    new Definition("skill.payday.node.technician.engineering.5.name", 3, "skill.payday.node.technician.engineering.5.base", 6, "skill.payday.node.technician.engineering.5.mastery"),
                    new Definition("skill.payday.node.technician.engineering.6.name", 4, "skill.payday.node.technician.engineering.6.base", 8, "skill.payday.node.technician.engineering.6.mastery")
            )),
            Map.entry("technician/repair", List.of(
                    new Definition("skill.payday.node.technician.repair.1.name", 1, "skill.payday.node.technician.repair.1.base", 3, "skill.payday.node.technician.repair.1.mastery"),
                    new Definition("skill.payday.node.technician.repair.2.name", 2, "skill.payday.node.technician.repair.2.base", 4, "skill.payday.node.technician.repair.2.mastery"),
                    new Definition("skill.payday.node.technician.repair.3.name", 2, "skill.payday.node.technician.repair.3.base", 4, "skill.payday.node.technician.repair.3.mastery"),
                    new Definition("skill.payday.node.technician.repair.4.name", 3, "skill.payday.node.technician.repair.4.base", 6, "skill.payday.node.technician.repair.4.mastery"),
                    new Definition("skill.payday.node.technician.repair.5.name", 3, "skill.payday.node.technician.repair.5.base", 6, "skill.payday.node.technician.repair.5.mastery"),
                    new Definition("skill.payday.node.technician.repair.6.name", 4, "skill.payday.node.technician.repair.6.base", 8, "skill.payday.node.technician.repair.6.mastery")
            )),
            Map.entry("technician/suppression", List.of(
                    new Definition("skill.payday.node.technician.suppression.1.name", 1, "skill.payday.node.technician.suppression.1.base", 3, "skill.payday.node.technician.suppression.1.mastery"),
                    new Definition("skill.payday.node.technician.suppression.2.name", 2, "skill.payday.node.technician.suppression.2.base", 4, "skill.payday.node.technician.suppression.2.mastery"),
                    new Definition("skill.payday.node.technician.suppression.3.name", 2, "skill.payday.node.technician.suppression.3.base", 4, "skill.payday.node.technician.suppression.3.mastery"),
                    new Definition("skill.payday.node.technician.suppression.4.name", 3, "skill.payday.node.technician.suppression.4.base", 6, "skill.payday.node.technician.suppression.4.mastery"),
                    new Definition("skill.payday.node.technician.suppression.5.name", 3, "skill.payday.node.technician.suppression.5.base", 6, "skill.payday.node.technician.suppression.5.mastery"),
                    new Definition("skill.payday.node.technician.suppression.6.name", 4, "skill.payday.node.technician.suppression.6.base", 8, "skill.payday.node.technician.suppression.6.mastery")
            )),
            Map.entry("ghost/ninjutsu", List.of(
                    new Definition("skill.payday.node.ghost.ninjutsu.1.name", 1, "skill.payday.node.ghost.ninjutsu.1.base", 3, "skill.payday.node.ghost.ninjutsu.1.mastery"),
                    new Definition("skill.payday.node.ghost.ninjutsu.2.name", 2, "skill.payday.node.ghost.ninjutsu.2.base", 4, "skill.payday.node.ghost.ninjutsu.2.mastery"),
                    new Definition("skill.payday.node.ghost.ninjutsu.3.name", 2, "skill.payday.node.ghost.ninjutsu.3.base", 4, "skill.payday.node.ghost.ninjutsu.3.mastery"),
                    new Definition("skill.payday.node.ghost.ninjutsu.4.name", 3, "skill.payday.node.ghost.ninjutsu.4.base", 6, "skill.payday.node.ghost.ninjutsu.4.mastery"),
                    new Definition("skill.payday.node.ghost.ninjutsu.5.name", 3, "skill.payday.node.ghost.ninjutsu.5.base", 6, "skill.payday.node.ghost.ninjutsu.5.mastery"),
                    new Definition("skill.payday.node.ghost.ninjutsu.6.name", 4, "skill.payday.node.ghost.ninjutsu.6.base", 8, "skill.payday.node.ghost.ninjutsu.6.mastery")
            )),
            Map.entry("ghost/evasion", List.of(
                    new Definition("skill.payday.node.ghost.evasion.1.name", 1, "skill.payday.node.ghost.evasion.1.base", 3, "skill.payday.node.ghost.evasion.1.mastery"),
                    new Definition("skill.payday.node.ghost.evasion.2.name", 2, "skill.payday.node.ghost.evasion.2.base", 4, "skill.payday.node.ghost.evasion.2.mastery"),
                    new Definition("skill.payday.node.ghost.evasion.3.name", 2, "skill.payday.node.ghost.evasion.3.base", 4, "skill.payday.node.ghost.evasion.3.mastery"),
                    new Definition("skill.payday.node.ghost.evasion.4.name", 3, "skill.payday.node.ghost.evasion.4.base", 6, "skill.payday.node.ghost.evasion.4.mastery"),
                    new Definition("skill.payday.node.ghost.evasion.5.name", 3, "skill.payday.node.ghost.evasion.5.base", 6, "skill.payday.node.ghost.evasion.5.mastery"),
                    new Definition("skill.payday.node.ghost.evasion.6.name", 4, "skill.payday.node.ghost.evasion.6.base", 8, "skill.payday.node.ghost.evasion.6.mastery")
            )),
            Map.entry("ghost/suppressor", List.of(
                    new Definition("skill.payday.node.ghost.suppressor.1.name", 1, "skill.payday.node.ghost.suppressor.1.base", 3, "skill.payday.node.ghost.suppressor.1.mastery"),
                    new Definition("skill.payday.node.ghost.suppressor.2.name", 2, "skill.payday.node.ghost.suppressor.2.base", 4, "skill.payday.node.ghost.suppressor.2.mastery"),
                    new Definition("skill.payday.node.ghost.suppressor.3.name", 2, "skill.payday.node.ghost.suppressor.3.base", 4, "skill.payday.node.ghost.suppressor.3.mastery"),
                    new Definition("skill.payday.node.ghost.suppressor.4.name", 3, "skill.payday.node.ghost.suppressor.4.base", 6, "skill.payday.node.ghost.suppressor.4.mastery"),
                    new Definition("skill.payday.node.ghost.suppressor.5.name", 3, "skill.payday.node.ghost.suppressor.5.base", 6, "skill.payday.node.ghost.suppressor.5.mastery"),
                    new Definition("skill.payday.node.ghost.suppressor.6.name", 4, "skill.payday.node.ghost.suppressor.6.base", 8, "skill.payday.node.ghost.suppressor.6.mastery")
            )),
            Map.entry("fugitive/quickdraw", List.of(
                    new Definition("skill.payday.node.fugitive.quickdraw.1.name", 1, "skill.payday.node.fugitive.quickdraw.1.base", 3, "skill.payday.node.fugitive.quickdraw.1.mastery"),
                    new Definition("skill.payday.node.fugitive.quickdraw.2.name", 2, "skill.payday.node.fugitive.quickdraw.2.base", 4, "skill.payday.node.fugitive.quickdraw.2.mastery"),
                    new Definition("skill.payday.node.fugitive.quickdraw.3.name", 2, "skill.payday.node.fugitive.quickdraw.3.base", 4, "skill.payday.node.fugitive.quickdraw.3.mastery"),
                    new Definition("skill.payday.node.fugitive.quickdraw.4.name", 3, "skill.payday.node.fugitive.quickdraw.4.base", 6, "skill.payday.node.fugitive.quickdraw.4.mastery"),
                    new Definition("skill.payday.node.fugitive.quickdraw.5.name", 3, "skill.payday.node.fugitive.quickdraw.5.base", 6, "skill.payday.node.fugitive.quickdraw.5.mastery"),
                    new Definition("skill.payday.node.fugitive.quickdraw.6.name", 4, "skill.payday.node.fugitive.quickdraw.6.base", 8, "skill.payday.node.fugitive.quickdraw.6.mastery")
            )),
            Map.entry("fugitive/rebirth", List.of(
                    new Definition("skill.payday.node.fugitive.rebirth.1.name", 1, "skill.payday.node.fugitive.rebirth.1.base", 3, "skill.payday.node.fugitive.rebirth.1.mastery"),
                    new Definition("skill.payday.node.fugitive.rebirth.2.name", 2, "skill.payday.node.fugitive.rebirth.2.base", 4, "skill.payday.node.fugitive.rebirth.2.mastery"),
                    new Definition("skill.payday.node.fugitive.rebirth.3.name", 2, "skill.payday.node.fugitive.rebirth.3.base", 4, "skill.payday.node.fugitive.rebirth.3.mastery"),
                    new Definition("skill.payday.node.fugitive.rebirth.4.name", 3, "skill.payday.node.fugitive.rebirth.4.base", 6, "skill.payday.node.fugitive.rebirth.4.mastery"),
                    new Definition("skill.payday.node.fugitive.rebirth.5.name", 3, "skill.payday.node.fugitive.rebirth.5.base", 6, "skill.payday.node.fugitive.rebirth.5.mastery"),
                    new Definition("skill.payday.node.fugitive.rebirth.6.name", 4, "skill.payday.node.fugitive.rebirth.6.base", 8, "skill.payday.node.fugitive.rebirth.6.mastery")
            )),
            Map.entry("fugitive/melee", List.of(
                    new Definition("skill.payday.node.fugitive.melee.1.name", 1, "skill.payday.node.fugitive.melee.1.base", 3, "skill.payday.node.fugitive.melee.1.mastery"),
                    new Definition("skill.payday.node.fugitive.melee.2.name", 2, "skill.payday.node.fugitive.melee.2.base", 4, "skill.payday.node.fugitive.melee.2.mastery"),
                    new Definition("skill.payday.node.fugitive.melee.3.name", 2, "skill.payday.node.fugitive.melee.3.base", 4, "skill.payday.node.fugitive.melee.3.mastery"),
                    new Definition("skill.payday.node.fugitive.melee.4.name", 3, "skill.payday.node.fugitive.melee.4.base", 6, "skill.payday.node.fugitive.melee.4.mastery"),
                    new Definition("skill.payday.node.fugitive.melee.5.name", 3, "skill.payday.node.fugitive.melee.5.base", 6, "skill.payday.node.fugitive.melee.5.mastery"),
                    new Definition("skill.payday.node.fugitive.melee.6.name", 4, "skill.payday.node.fugitive.melee.6.base", 8, "skill.payday.node.fugitive.melee.6.mastery")
            ))
    );

    private SkillData() {
    }

    public static List<Definition> forSeries(String categoryId, String seriesId) {
        return DATA.getOrDefault(categoryId + "/" + seriesId, List.of());
    }

    public record Definition(Component name, int baseCost, Component baseEffect,
                              int masteryCost, Component masteryEffect) {
        public Definition(String name, int baseCost, String baseEffect,
                          int masteryCost, String masteryEffect) {
            this(Component.translatable(name), baseCost, Component.translatable(baseEffect),
                    masteryCost, Component.translatable(masteryEffect));
        }
    }
}
