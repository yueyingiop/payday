package com.yueyingiop.payday.skill;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/** A top-level skill category and its three skill series. */
public record SkillCategory(String id, Component displayName, List<SkillSeries> series) {
    public static SkillCategory create(String id, String... series) {
        return new SkillCategory(id, Component.translatable("skill.payday.category." + id),
                Arrays.stream(series).map(value -> new SkillSeries(id, value)).toList());
    }
}
