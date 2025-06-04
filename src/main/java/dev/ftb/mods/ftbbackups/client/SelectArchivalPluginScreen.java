package dev.ftb.mods.ftbbackups.client;

import dev.ftb.mods.ftbbackups.archival.ArchivePluginManager;
import dev.ftb.mods.ftbbackups.config.ArchivalPluginConfig;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SelectArchivalPluginScreen extends AbstractButtonListScreen {
    private final ArchivalPluginConfig config;
    private final ConfigCallback callback;

    public SelectArchivalPluginScreen(ArchivalPluginConfig config, ConfigCallback callback) {
        this.config = config;
        this.callback = callback;

        setTitle(Component.translatable("ftbbackups3.general.archival_plugin"));
        showBottomPanel(false);
        showCloseButton(true);
    }

    @Override
    public void addButtons(Panel panel) {
        ArchivePluginManager.clientInstance().plugins().keySet()
                .forEach(k -> panel.add(new PluginButton(panel, k)));
    }

    @Override
    public boolean onClosedByKey(Key key) {
        if (super.onClosedByKey(key)) {
            callback.save(false);
            return true;
        }

        return false;
    }

    @Override
    protected void doCancel() {
        callback.save(false);
    }

    @Override
    protected void doAccept() {
        callback.save(true);
    }

    private class PluginButton extends SimpleTextButton {
        private final ResourceLocation value;

        PluginButton(Panel panel, ResourceLocation value) {
            super(panel, Component.literal(value.toString()), Color4I.empty());
            this.value = value;
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver) {
                Color4I.WHITE.withAlpha(30).draw(graphics, x, y, w, h);
            }
            Color4I.GRAY.withAlpha(40).draw(graphics, x, y + h, w, 1);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            playClickSound();
            config.setCurrentValue(value);
            callback.save(true);
        }
    }
}
