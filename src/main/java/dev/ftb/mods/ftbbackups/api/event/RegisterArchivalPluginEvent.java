package dev.ftb.mods.ftbbackups.api.event;

import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.neoforged.bus.api.Event;

import java.util.function.Consumer;

public class RegisterArchivalPluginEvent extends Event {
    private final Consumer<IArchivalPlugin> consumer;

    public RegisterArchivalPluginEvent(Consumer<IArchivalPlugin> consumer) {
        this.consumer = consumer;
    }

    public void register(IArchivalPlugin plugin) {
        consumer.accept(plugin);
    }
}
