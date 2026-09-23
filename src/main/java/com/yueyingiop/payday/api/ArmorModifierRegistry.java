package com.yueyingiop.payday.api;
import com.yueyingiop.payday.item.ArmorCurioItem;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.*;
public final class ArmorModifierRegistry {
    private static final Map<ArmorCurioItem, Map<String, Entry>> VALUES = new WeakHashMap<>();
    private static final Map<ArmorCurioItem, Set<String>> REMOVED = new WeakHashMap<>();
    /**
     * 更新对应状态。
     * @param item 物品。
     * @param key 按键。
     * @param attribute 属性。
     * @param type 类型。
     * @param value 数值。
     * @param replace 是否替换已有修饰符。
     */
    public static synchronized void change(ArmorCurioItem item, String key, Attribute attribute, String type, float value, boolean replace) {
        Map<String, Entry> map = VALUES.computeIfAbsent(item, ignored -> new HashMap<>());
        boolean explicitlyRemoved = REMOVED.getOrDefault(item, Set.of()).contains(key);
        if (!replace && !map.containsKey(key) && item.hasBaseAttribute(key) && !explicitlyRemoved) return;
        if (!replace && map.containsKey(key)) return;
        if (value == 0 && replace) {
            map.remove(key);
            REMOVED.computeIfAbsent(item, ignored -> new HashSet<>()).add(key);
        } else {
            map.put(key, new Entry(attribute, type, value));
            Set<String> removed = REMOVED.get(item);
            if (removed != null) removed.remove(key);
        }
    }
    public record Entry(Attribute attribute, String type, float value) {}
    /**
     * 获取护甲属性修饰符条目。
     * @param item 物品。
     * @return 对应的执行结果。
     */
    public static synchronized Collection<Entry> entries(ArmorCurioItem item) { return VALUES.getOrDefault(item, Map.of()).values(); }
    /**
     * 判断是否覆盖指定属性。
     * @param item 物品。
     * @param key 按键。
     * @return 对应的执行结果。
     */
    public static synchronized boolean overrides(ArmorCurioItem item, String key) { return VALUES.getOrDefault(item, Map.of()).containsKey(key) || REMOVED.getOrDefault(item, Set.of()).contains(key); }
}
