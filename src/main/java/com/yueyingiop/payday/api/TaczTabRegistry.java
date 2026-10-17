package com.yueyingiop.payday.api;

import com.tacz.guns.resource.pojo.data.block.TabConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public registry for tabs shown by TACZ attachment workbenches.
 *
 * <p>The string overload is intentionally KubeJS-friendly. Tabs are resolved
 * into TACZ's {@link TabConfig} only after item registries are available.</p>
 */
public final class TaczTabRegistry {
    private static final Map<ResourceLocation, TabDefinition> TABS = new LinkedHashMap<>();

    static {
        register("payday:misc", "payday.type.misc.name", "payday:resistance_slot");
        register("payday:phone_upgrades", "payday.type.phone_upgrades.name", "payday:phone");
    }

    private TaczTabRegistry() {
    }

    /** Registers or replaces a tab while preserving its first insertion position. */
    public static synchronized TabDefinition register(String tabId, String translationKey, String iconItemId) {
        return register(parse(tabId), translationKey, parse(iconItemId));
    }

    /** Java-friendly overload for integrations that already have resource locations. */
    public static synchronized TabDefinition register(ResourceLocation tabId, String translationKey,
                                                      ResourceLocation iconItemId) {
        if (tabId == null || translationKey == null || translationKey.isBlank() || iconItemId == null) {
            throw new IllegalArgumentException("tabId, translationKey and iconItemId are required");
        }
        TabDefinition definition = new TabDefinition(tabId, translationKey, iconItemId);
        TABS.put(tabId, definition);
        return definition;
    }

    /** Returns a snapshot suitable for merging into TACZ's mutable block data. */
    public static synchronized List<TabDefinition> getDefinitions() {
        return List.copyOf(TABS.values());
    }

    /** Resolves the registered definitions into TACZ tab records. */
    public static synchronized List<TabConfig> getTabs() {
        List<TabConfig> result = new ArrayList<>(TABS.size());
        for (TabDefinition definition : TABS.values()) {
            ItemStack icon = ForgeRegistries.ITEMS.getDelegate(definition.iconItemId())
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
            result.add(new TabConfig(definition.id(), definition.translationKey(), icon));
        }
        return result;
    }

    private static ResourceLocation parse(String id) {
        ResourceLocation result = ResourceLocation.tryParse(id);
        if (result == null) {
            throw new IllegalArgumentException("Invalid resource location: " + id);
        }
        return result;
    }

    public record TabDefinition(ResourceLocation id, String translationKey, ResourceLocation iconItemId) {
    }
}
