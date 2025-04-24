package dev.ftb.mods.ftbbackups.client;

import dev.ftb.mods.ftbbackups.Backups;
import dev.ftb.mods.ftbbackups.FTBBackups;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = FTBBackups.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BackupsClient {
    private static BackupProgress backupProgress = BackupProgress.NONE;

    private static int progressTicker = 0;

    public static void init(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(BackupsClient::onClientDisconnected);
//        NeoForge.EVENT_BUS.addListener(BackupsClient::onDebugInfoEvent);
        NeoForge.EVENT_BUS.addListener(BackupsClient::addRestoreButton);
        NeoForge.EVENT_BUS.addListener(BackupsClient::clientTick);

//        eventBus.addListener(BackupsClient::registerGuiLayer);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        if (progressTicker > 0 && --progressTicker == 0) {
            backupProgress = BackupProgress.NONE;
        }
    }

    private static void addRestoreButton(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof SelectWorldScreen s) {
            Backups backups = Backups.getClientInstance();
            if (!backups.backups().isEmpty()) {
                Component btnLabel = Component.translatable("ftbbackups3.gui.restore").append("...");
                Component title = Component.translatable("ftbbackups3.gui.restore");
                int w = s.getMinecraft().font.width(btnLabel) + 20;
                event.addListener(Button.builder(btnLabel, b -> Minecraft.getInstance().setScreen(new RestoreBackupScreen(s, title)))
                        .bounds(s.width - w - 10, 22, w, 20)
                        .tooltip(Tooltip.create(Component.translatable("ftbbackups3.gui.restore.tooltip")))
                        .build());
            }
        }
    }

    @SubscribeEvent
    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        // needs to be done with SubscribeEvent, addListener() is called too late
        event.registerAboveAll(BackupOverlayLayer.ID, new BackupOverlayLayer());
    }

    public static void setBackupProgress(int current, int total) {
        backupProgress = new BackupProgress(current, total);
        if (backupProgress.finished()) {
            progressTicker = 40;
        }
        Backups.LOGGER.info("progress: {}", backupProgress);
    }

    private static void onClientDisconnected(ClientPlayerNetworkEvent.LoggingOut ignoredEvent) {
        backupProgress = BackupProgress.NONE;
    }

//    private static void onDebugInfoEvent(CustomizeGuiOverlayEvent.DebugText event) {
//        OptionInstance<Boolean> booleanOptionInstance = Minecraft.getInstance().options.reducedDebugInfo();
//        if (booleanOptionInstance.get()) {
//            return;
//        }
//
//        if (backupProgress.total() > 0 && backupProgress.total() > backupProgress.current()) {
//            // I think this is right? @mikey (2025-03-18)
//            event.getLeft().add(progressMessage().withStyle(ChatFormatting.LIGHT_PURPLE).getString());
//        }
//    }

    public static @NotNull MutableComponent progressMessage() {
        return Component.translatable("ftbbackups3.lang.timer_progress", backupProgress.current() * 100 / backupProgress.total(), backupProgress.current(), backupProgress.total());
    }

    public static BackupProgress getBackupProgress() {
        return backupProgress;
    }

    public record BackupProgress(int current, int total) {
        public static final BackupProgress NONE = new BackupProgress(0, 0);

        public boolean inProgress() {
            return total > 0;
        }

        public boolean finished() {
            return current == total;
        }
    }
}
