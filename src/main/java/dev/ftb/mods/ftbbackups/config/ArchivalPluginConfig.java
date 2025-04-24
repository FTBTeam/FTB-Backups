package dev.ftb.mods.ftbbackups.config;

import dev.ftb.mods.ftbbackups.client.SelectArchivalPluginScreen;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.resources.ResourceLocation;

public class ArchivalPluginConfig extends ConfigValue<ResourceLocation> {
    @Override
    public void onClicked(Widget widget, MouseButton mouseButton, ConfigCallback configCallback) {
        if (getCanEdit()) {
            new SelectArchivalPluginScreen(this, configCallback).openGui();
        }
    }
}
