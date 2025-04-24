package dev.ftb.mods.ftbbackups.api.event;

import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.neoforged.bus.api.Event;

import java.util.function.Consumer;

/**
 * Listen to this event to register custom archival plugins; alternative methods of creating an archive file. Note
 * that the mod by default provides archive plugins to create ZIP files, and do a simple recursive file copy.
 * <p>
 * This event is fired on both client and server and plugins should be registered on both sides.
 */
public class RegisterArchivalPluginEvent extends Event {
    private final Consumer<IArchivalPlugin> consumer;

    public RegisterArchivalPluginEvent(Consumer<IArchivalPlugin> consumer) {
        this.consumer = consumer;
    }

    public void register(IArchivalPlugin plugin) {
        consumer.accept(plugin);
    }
}
