package dev.ftb.mods.ftbbackups;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.ftb.mods.ftbbackups.api.Backup;
import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import dev.ftb.mods.ftbbackups.api.event.BackupEvent;
import dev.ftb.mods.ftbbackups.archival.ArchivePluginManager;
import dev.ftb.mods.ftbbackups.net.BackupProgressPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Backups {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static final Logger LOGGER = LoggerFactory.getLogger(Backups.class);
    public static final String BACKUPS_JSON_FILE = "backups.json";

    private static Backups clientInstance = null;
    private static Backups serverInstance = null;

    private final List<Backup> backups = new ArrayList<>();
    public Path backupsFolder;
    public long nextBackupTime = -1L;
    public BackupStatus backupStatus;
    public boolean printFiles = false;

    private final AtomicInteger currentFileIndex = new AtomicInteger(0);
    private final AtomicInteger totalFileCount = new AtomicInteger(0);
    private int prevFileIndex = 0;
    private String currentFileName = "";
    public boolean playersOnlineSinceLastBackup = false;

    public static Backups getClientInstance() {
        if (clientInstance == null) {
            clientInstance = new Backups();
        }
        clientInstance.loadIndexFile();
        return clientInstance;
    }

    public static void initServerInstance() {
        serverInstance = new Backups();
    }

    public static Backups getServerInstance() {
        if (serverInstance == null) {
            initServerInstance();
        }
        return serverInstance;
    }

    private Backups() {
        backupsFolder = FTBBackupsServerConfig.getBackupFolder();

        try {
            backupsFolder = backupsFolder.toRealPath();
            Files.createDirectories(backupsFolder);
        } catch (Exception ignored) {
        }

        backupStatus = BackupStatus.NONE;

        loadIndexFile();

        LOGGER.info("Backups will be written to: {}", backupsFolder);
    }

    public void loadIndexFile() {
        backups.clear();

        Path backupsIndex = backupsIndexPath();
        JsonElement element = BackupUtils.readJson(backupsIndex);
        Backup.LIST_CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(err -> LOGGER.warn("can't parse backups index {}", backupsIndex))
                .ifPresent(backups::addAll);
    }

    private static Path backupsIndexPath() {
        return FMLPaths.GAMEDIR.get().resolve(FTBBackupsServerConfig.DEFAULT_BACKUP_FOLDER).resolve(BACKUPS_JSON_FILE);
    }

    public void tick(MinecraftServer server, long now) {
        if (nextBackupTime < 0) {
            // will be the case on the first tick
            nextBackupTime = System.currentTimeMillis() + FTBBackupsServerConfig.getBackupTimerMillis();
        }

        if (nextBackupTime > 0L && nextBackupTime <= now) {
            if (!FTBBackupsServerConfig.ONLY_IF_PLAYERS_ONLINE.get() || playersOnlineSinceLastBackup || !server.getPlayerList().getPlayers().isEmpty()) {
                playersOnlineSinceLastBackup = false;
                run(server, true, Component.translatable("ftbbackups3.lang.server"), "");
            }
        }

        if (backupStatus.isDone()) {
            backupStatus = BackupStatus.NONE;

            for (ServerLevel level : server.getAllLevels()) {
                if (level != null) {
                    level.noSave = false;
                }
            }

            if (!FTBBackupsServerConfig.SILENT.get()) {
                BackupProgressPacket.sendProgress(server, BackupProgressPacket.complete());
            }

            currentFileIndex.set(0);
            totalFileCount.set(0);
        } else if (backupStatus.isRunning() && printFiles) {
            int curIdx = currentFileIndex.get();
            if (curIdx == 0 || curIdx == totalFileCount.get() - 1) {
                LOGGER.info("[{} | {}%]: {}", currentFileIndex, (int) ((curIdx / (double) totalFileCount.get()) * 100D), currentFileName);
            }

            if (!FTBBackupsServerConfig.SILENT.get() && prevFileIndex != currentFileIndex.get()) {
                BackupProgressPacket.sendProgress(server, BackupProgressPacket.update());
                prevFileIndex = currentFileIndex.get();
            }
        }
    }

    public void notifyAll(MinecraftServer server, Component component, boolean error) {
        component = component.plainCopy();
        component.getStyle().withColor(TextColor.fromLegacyFormat(error ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE));
        server.getPlayerList().broadcastSystemMessage(component, true);
    }

    public boolean run(MinecraftServer server, boolean auto, Component name, String customName) {
        if (backupStatus.isRunningOrDone()) {
            return false;
        }

        if (auto && !FTBBackupsServerConfig.AUTO.get()) {
            return false;
        }

        notifyAll(server, Component.translatable("ftbbackups3.lang.start", name), false);
        LOGGER.info("backup starting: {}", name.getString());
        nextBackupTime = System.currentTimeMillis() + FTBBackupsServerConfig.getBackupTimerMillis();

        for (ServerLevel level : server.getAllLevels()) {
            if (level != null) {
                level.noSave = true;
            }
        }

        backupStatus = BackupStatus.RUNNING;
        server.getPlayerList().saveAll();

        CompletableFuture.runAsync(() -> {
            try {
                createBackup(server, customName);
            } catch (Exception ex) {
                LOGGER.error("backup creation failed: {} -> {}", ex.getClass(), ex.getMessage());
                server.execute(() -> notifyAll(server, Component.translatable("ftbbackups3.lang.saving_failed"), true));
            }
        }).thenRun(() -> backupStatus = BackupStatus.DONE);

        return true;
    }

    private void createBackup(MinecraftServer server, String customName) {
        Path absoluteWorldPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();

        IArchivalPlugin archivalPlugin = ArchivePluginManager.serverInstance().getPlugin(FTBBackupsServerConfig.archivalPlugin());
        if (archivalPlugin == null) {
            Backups.LOGGER.error("bad archive plugin setting in server config: {}! backup skipped", FTBBackupsServerConfig.archivalPlugin());
            return;
        }

        String backupFileName = archivalPlugin.addFileExtension(customName.isEmpty() ? DATE_TIME_FORMATTER.format(LocalDateTime.now()) : customName);

        long now = Util.getEpochMillis();
        boolean success = false;
        Exception error = null;
        long archiveSize = 0L;

        MutableInt fileCount = new MutableInt(0);
        try {
            var manifest = gatherFiles(server);
            fileCount.setValue(manifest.size());

            for (Map.Entry<Path, String> entry : manifest.entrySet()) {
                archiveSize += Files.size(entry.getKey());
            }

            doArchiveCleanup(archiveSize);

            totalFileCount.set(manifest.size());
            LOGGER.info("Backing up {} files...", totalFileCount);
            printFiles = true;

            BackupProgressPacket.sendProgress(server, BackupProgressPacket.start());

            currentFileIndex.set(0);
            Path archiveDest = backupsFolder.resolve(backupFileName);
            long start = Util.getEpochMillis();
            archiveSize = archivalPlugin.createArchive(new BackupContext(manifest, LOGGER, archiveDest, FTBBackupsServerConfig.COMPRESSION_LEVEL.get(), this));
            LOGGER.info("Created archive {} from {} in {} ms ({})!",
                    archiveDest, absoluteWorldPath,
                    Util.getEpochMillis() - start, BackupUtils.formatSizeString(archiveSize)
            );

            success = true;
        } catch (Exception ex) {
            if (!FTBBackupsServerConfig.SILENT.get()) {
                String errorName = ex.getClass().getName();
                notifyAll(server, Component.translatable("ftbbackups3.lang.fail", errorName), true);
            }
            LOGGER.error("Backup to {} failed: {} -> {}", backupFileName, ex.getClass(), ex.getMessage());
            error = ex;
        }

        printFiles = false;

        Backup backup = new Backup(now, archivalPlugin.getId(), backupFileName, server.getWorldData().getLevelName(), getLastIndex() + 1, success, archiveSize, fileCount.getValue());
        backups.add(backup);

        Backup.LIST_CODEC.encodeStart(JsonOps.INSTANCE, backups)
                .ifSuccess(json -> BackupUtils.writeJson(backupsIndexPath(), json, true));

        NeoForge.EVENT_BUS.post(new BackupEvent.Post(backup, error));

        if (error == null && !FTBBackupsServerConfig.SILENT.get()) {
            processBackupResults(server, now, archiveSize);
        }
    }

    /**
     * Scan the instance, gathering files which will be added to the backup archive
     * <ul>
     *     <li>All files in the instance's world directory (world/ for dedicated server, saves/{worldname} for SSP</li>
     *     <li>All extra files found the "extra_files" config setting (must be relative to instance dir)</li>
     *     <li>Any extra files added by listeners of {@code BackupEvent.Pre}</li>
     * </ul>
     * @param server the server
     * @return a map of absolute filepath -> string path of location within the archive
     * @throws IOException if any file-related problems occur
     */
    private Map<Path,String> gatherFiles(MinecraftServer server) throws IOException {
        Map<Path, String> res = new LinkedHashMap<>();

        Path instanceDir = server.getServerDirectory().toRealPath();

        Consumer<Path> extras = path -> {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.isAbsolute()) {
                            LOGGER.error("ignoring absolute file {} in extras!", file);
                        } else {
                            res.put(file.toRealPath(), file.toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        for (String s : FTBBackupsServerConfig.EXTRA_FILES.get()) {
            extras.accept(Path.of(s));
        }

        NeoForge.EVENT_BUS.post(new BackupEvent.Pre(extras));

        Files.walkFileTree(server.getWorldPath(LevelResource.ROOT).toAbsolutePath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.endsWith("session.lock") && Files.isReadable(file)) {
                    res.put(file.toRealPath(), instanceDir.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return res;
    }

    /**
     * Check if any existing backup archives need to be purged, based on the "backups_to_keep" and "max_total_size"
     * config settings.
     *
     * @param fileSize the file size of the archive about to be created
     */
    private void doArchiveCleanup(long fileSize) {
        if (!backups.isEmpty()) {
            backups.sort(null);

            int backupsToKeep = FTBBackupsServerConfig.BACKUPS_TO_KEEP.get() - 1;
            if (backupsToKeep > 0 && backups.size() > backupsToKeep) {
                while (backups.size() > backupsToKeep) {
                    Backup backup = backups.removeFirst();
                    backup.deleteFiles(backupsFolder);
                }
            }

            long totalSize = backups.stream().mapToLong(Backup::size).sum();

            if (fileSize > 0L) {
                long freeSpace = Math.min(FTBBackupsServerConfig.MAX_TOTAL_SIZE.get(), backupsFolder.toFile().getFreeSpace());
                while (totalSize + fileSize > freeSpace && !backups.isEmpty()) {
                    Backup backup = backups.removeFirst();
                    if (backup.deleteFiles(backupsFolder)) {
                        totalSize -= backup.size();
                    }
                }
            }
        }
    }

    private void processBackupResults(MinecraftServer server, long backupTime, long archiveSize) {
        Duration d = Duration.ZERO.plusMillis(Util.getEpochMillis() - backupTime);
        String timeString = String.format("%d.%03ds", d.toSeconds(), d.toMillisPart());
        Component component;

        if (FTBBackupsServerConfig.DISPLAY_FILE_SIZE.get()) {
            long totalSize = backups.stream().mapToLong(Backup::size).sum();

            String sizeB = BackupUtils.formatSizeString(archiveSize);
            String sizeT = BackupUtils.formatSizeString(totalSize);
            String sizeString = sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT);
            component = Component.translatable("ftbbackups3.lang.end_2", timeString, sizeString);
        } else {
            component = Component.translatable("ftbbackups3.lang.end_1", timeString);
        }

        component.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
        server.getPlayerList().broadcastSystemMessage(component, true);
    }

    public long totalBackupSize() {
        return backups.stream().mapToLong(Backup::size).sum();
    }

    private int getLastIndex() {
        int i = 0;

        for (Backup b : backups) {
            i = Math.max(i, b.index());
        }

        return i;
    }

    public int getCurrentFileIndex() {
        return currentFileIndex.get();
    }

    public int getTotalFileCount() {
        return totalFileCount.get();
    }

    public Collection<Backup> backups() {
        return Collections.unmodifiableCollection(backups);
    }

    private record BackupContext(
            Map<Path,String> manifest,
            Logger logger,
            Path archivePath,
            int compressionLevel,
            Backups backups
    ) implements IArchivalPlugin.ArchivalContext {
        @Override
        public synchronized void notifyProcessingFile(String filename) {
            backups.currentFileName = filename;
            backups.currentFileIndex.incrementAndGet();
        }
    }
}
