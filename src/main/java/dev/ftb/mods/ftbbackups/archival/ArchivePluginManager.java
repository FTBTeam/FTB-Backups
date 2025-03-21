package dev.ftb.mods.ftbbackups.archival;

import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ArchivePluginManager {
    INSTANCE;

    private final Map<ResourceLocation, IArchivalPlugin> plugins = new ConcurrentHashMap<>();

    public synchronized void register(IArchivalPlugin plugin) {
        if (plugins.containsKey(plugin.getId())) {
            throw new IllegalStateException("archival plugin " + plugin.getId() + " already registered!");
        }
        plugins.put(plugin.getId(), plugin);
    }

    public IArchivalPlugin getPlugin(ResourceLocation id) {
        return plugins.get(id);
    }
}
