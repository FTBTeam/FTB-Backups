package dev.ftb.mods.ftbbackups.archival;

import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import dev.ftb.mods.ftbbackups.api.event.RegisterArchivalPluginEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArchivePluginManager {
    private static final ArchivePluginManager CLIENT = new ArchivePluginManager();
    private static final ArchivePluginManager SERVER = new ArchivePluginManager();

    private final Map<ResourceLocation, IArchivalPlugin> plugins = new ConcurrentHashMap<>();

    private static boolean pluginsRegistered = false;

    public static ArchivePluginManager clientInstance() {
        if (!pluginsRegistered) {
            NeoForge.EVENT_BUS.post(new RegisterArchivalPluginEvent(CLIENT::register));
            pluginsRegistered = true;
        }
        return CLIENT;
    }

    public static ArchivePluginManager serverInstance() {
        return SERVER;
    }

    public synchronized void register(IArchivalPlugin plugin) {
        if (plugins.containsKey(plugin.getId())) {
            throw new IllegalStateException("archival plugin " + plugin.getId() + " already registered!");
        }
        plugins.put(plugin.getId(), plugin);
    }

    public IArchivalPlugin getPlugin(ResourceLocation id) {
        return plugins.get(id);
    }

    public void clear() {
        plugins.clear();
    }

    public Map<ResourceLocation, IArchivalPlugin> plugins() {
        return Collections.unmodifiableMap(plugins);
    }
}
