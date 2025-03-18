package dev.ftb.mods.ftbbackups;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FTBBackups.MOD_ID)
public class FTBBackups {
    public static final String MOD_ID = "ftbbackups3";
    public static final Logger LOGGER = LoggerFactory.getLogger(FTBBackups.class);

    public FTBBackups(IEventBus eventBus, ModContainer container) {
        eventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::serverAboutToStart);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::registerCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::serverStopping);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::levelTick);
//		FTBBackupsNetHandler.init();
        FTBBackupsConfig.register(eventBus, container);
        //ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        FTBBackupsClient.init();
    }

    public void serverAboutToStart(ServerAboutToStartEvent event) {
        Backups.INSTANCE.init(event.getServer());
    }

    public void registerCommands(RegisterCommandsEvent event) {
        BackupCommands.register(event.getDispatcher());
    }

    public void serverStopping(ServerStoppingEvent event) {
        if (FTBBackupsConfig.forceOnShutdown) {
            Backups.INSTANCE.run(event.getServer(), true, Component.literal("Server"), "");
        }
    }

    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            //TODO: Send Packets
            //FTBBackupsNetHandler.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new BackupProgressPacket(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles));
        }
    }

    public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            Backups.INSTANCE.hadPlayersOnline = true;
        }
    }

    public void levelTick(ServerTickEvent.Post event) {
        Backups.INSTANCE.tick(event.getServer(), System.currentTimeMillis());
    }
}
