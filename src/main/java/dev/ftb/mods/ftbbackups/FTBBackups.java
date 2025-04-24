package dev.ftb.mods.ftbbackups;

import dev.ftb.mods.ftbbackups.api.event.RegisterArchivalPluginEvent;
import dev.ftb.mods.ftbbackups.archival.ArchivePluginManager;
import dev.ftb.mods.ftbbackups.archival.FileCopyArchiver;
import dev.ftb.mods.ftbbackups.archival.ZipArchiver;
import dev.ftb.mods.ftbbackups.client.BackupsClient;
import dev.ftb.mods.ftbbackups.net.BackupProgressPacket;
import dev.ftb.mods.ftbbackups.net.FTBBackupsNetHandler;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FTBBackups.MOD_ID)
public class FTBBackups {
    public static final String MOD_ID = "ftbbackups3";

    private static final Logger LOGGER = LoggerFactory.getLogger(FTBBackups.class);

    public FTBBackups(IEventBus eventBus, ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            eventBus.<FMLClientSetupEvent>addListener(event -> clientSetup(event, eventBus));
        }
        eventBus.addListener(this::registerNetwork);

        ConfigManager.getInstance().registerServerConfig(FTBBackupsServerConfig.CONFIG, MOD_ID + ".general", true, FTBBackupsServerConfig::onConfigChanged);
        ConfigManager.getInstance().registerClientConfig(FTBBackupsClientConfig.CONFIG, MOD_ID + ".general");

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::serverAboutToStart);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::serverStopping);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::levelTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::registerArchivalPlugins);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void clientSetup(FMLClientSetupEvent event, IEventBus eventBus) {
        BackupsClient.init(eventBus);
    }

    public void serverAboutToStart(ServerAboutToStartEvent event) {
        Backups.initServerInstance();

        NeoForge.EVENT_BUS.post(new RegisterArchivalPluginEvent(ArchivePluginManager.serverInstance()::register));
    }

    public void registerCommands(RegisterCommandsEvent event) {
        BackupCommands.register(event.getDispatcher());
    }

    public void registerNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        FTBBackupsNetHandler.init(registrar);
    }

    public void serverStopping(ServerStoppingEvent event) {
        ArchivePluginManager.serverInstance().clear();

        if (FTBBackupsServerConfig.FORCE_ON_SHUTDOWN.get()) {
            Backups.getServerInstance().run(event.getServer(), true, Component.literal("Server"), "");
        }
    }

    private void registerArchivalPlugins(RegisterArchivalPluginEvent event) {
        event.register(FileCopyArchiver.INSTANCE);
        event.register(ZipArchiver.INSTANCE);
    }

    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, BackupProgressPacket.update());
        }
    }

    public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            Backups.getServerInstance().hadPlayersOnline = true;
        }
    }

    public void levelTick(ServerTickEvent.Post event) {
        Backups.getServerInstance().tick(event.getServer(), System.currentTimeMillis());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
