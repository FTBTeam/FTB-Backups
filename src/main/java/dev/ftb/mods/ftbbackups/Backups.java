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
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * TODO: Logging!
 */
public enum Backups {
    INSTANCE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static final Logger LOGGER = LoggerFactory.getLogger(Backups.class);
    public static final String BACKUPS_JSON_FILE = "backups.json";

    private final List<Backup> backups = new ArrayList<>();
    public Path backupsFolder;
    public long nextBackupTime = -1L;
    public BackupStatus backupStatus = BackupStatus.NONE;
    public boolean printFiles = false;

    private final AtomicInteger currentFileIndex = new AtomicInteger(0);
    public int totalFiles = 0;
    private String currentFileName = "";
    public boolean hadPlayersOnline = false;

    public void init(MinecraftServer server) {
        String folder = FTBBackupsConfig.FOLDER.get();
        backupsFolder = folder.trim().isEmpty() ?
                FMLPaths.GAMEDIR.get().resolve("backups") :
                Paths.get(folder);

        try {
            backupsFolder = backupsFolder.toRealPath();
        } catch (Exception ignored) {
        }

        backupStatus = BackupStatus.NONE;
        backups.clear();

        Path backupsIndexFile = indexJsonPath(server);
        JsonElement element = BackupUtils.readJson(backupsIndexFile);
        Backup.LIST_CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(err -> LOGGER.warn("can't parse backups index {}", backupsIndexFile))
                .ifPresent(backups::addAll);

        LOGGER.info("Backups will be written to: {}", backupsFolder);
        nextBackupTime = System.currentTimeMillis() + FTBBackupsConfig.getBackupTimerMillis();
    }

    private static Path indexJsonPath(MinecraftServer server) {
        return server.getServerDirectory().resolve(FTBBackups.MOD_ID).resolve(BACKUPS_JSON_FILE);
    }

    public void tick(MinecraftServer server, long now) {
        if (nextBackupTime > 0L && nextBackupTime <= now) {
            if (!FTBBackupsConfig.ONLY_IF_PLAYERS_ONLINE.get() || hadPlayersOnline || !server.getPlayerList().getPlayers().isEmpty()) {
                hadPlayersOnline = false;
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

            if (!FTBBackupsConfig.SILENT.get()) {
                PacketDistributor.sendToAllPlayers(BackupProgressPacket.reset());
            }
        } else if (backupStatus.isRunning() && printFiles) {
            int curIdx = currentFileIndex.get();
            if (curIdx == 0 || curIdx == totalFiles - 1) {
                LOGGER.info("[{} | {}%]: {}", currentFileIndex, (int) ((curIdx / (double) totalFiles) * 100D), currentFileName);
            }

            if (!FTBBackupsConfig.SILENT.get()) {
                PacketDistributor.sendToAllPlayers(BackupProgressPacket.create());
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

        if (auto && !FTBBackupsConfig.AUTO.get()) {
            return false;
        }

        notifyAll(server, Component.translatable("ftbbackups3.lang.start", name), false);
        LOGGER.info("backup starting: {}", name.getString());
        nextBackupTime = System.currentTimeMillis() + FTBBackupsConfig.getBackupTimerMillis();

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

        IArchivalPlugin archivalPlugin = ArchivePluginManager.INSTANCE.getPlugin(FTBBackupsConfig.archivalPlugin());

        String backupFileName = IArchivalPlugin.addFileExtension(archivalPlugin, customName.isEmpty() ? DATE_TIME_FORMATTER.format(LocalDateTime.now()) : customName);

        long now = Util.getEpochMillis();
        boolean success = false;
        Exception error = null;
        long archiveSize = 0L;

        try {
            var manifest = gatherFiles(server);

            for (Map.Entry<Path, String> entry : manifest.entrySet()) {
                archiveSize += Files.size(entry.getKey());
            }

            doArchiveCleanup(archiveSize);

            totalFiles = manifest.size();
            LOGGER.info("Backing up {} files...", totalFiles);
            printFiles = true;

            currentFileIndex.set(0);
            Path archiveDest = backupsFolder.resolve(backupFileName);
            long start = Util.getEpochMillis();
            archiveSize = archivalPlugin.createArchive(new BackupContext(manifest, LOGGER, archiveDest, FTBBackupsConfig.COMPRESSION_LEVEL.get(), this));
            LOGGER.info("Created archive {} from {} in {} ms ({})!",
                    archiveDest, absoluteWorldPath,
                    Util.getEpochMillis() - start, BackupUtils.formatSizeString(archiveSize)
            );

            success = true;
        } catch (Exception ex) {
            if (!FTBBackupsConfig.SILENT.get()) {
                String errorName = ex.getClass().getName();
                notifyAll(server, Component.translatable("ftbbackups3.lang.fail", errorName), true);
            }
            LOGGER.error("Backup to {} failed: {} -> {}", backupFileName, ex.getClass(), ex.getMessage());
            error = ex;
        }

        printFiles = false;

        Backup backup = new Backup(now, backupFileName, getLastIndex() + 1, success, archiveSize);
        backups.add(backup);

        Backup.LIST_CODEC.encodeStart(JsonOps.INSTANCE, backups)
                .ifSuccess(json -> BackupUtils.writeJson(indexJsonPath(server), json, true));

        NeoForge.EVENT_BUS.post(new BackupEvent.Post(backup, error));

        if (error == null && !FTBBackupsConfig.SILENT.get()) {
            processBackupResults(server, now, archiveSize);
        }
    }

    /**
     * Scan the instance, gathering files which will be added to the backup archive
     * <ul>
     *     <li>All files in the instance's world directory (world/ for dedicated server, saves/{worldname} for SSP</li>
     *     <li>All extra files found the "extra_files" config setting (may be absolute paths, or relative to instance dir)</li>
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
                        res.put(file.toRealPath(), file.isAbsolute() ? "_extra_absolute_" + file : "_extra_relative_" + File.separator + file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        for (String s : FTBBackupsConfig.EXTRA_FILES.get()) {
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

//    private long archiveFiles(Map<Path, String> manifest, Path dstFile) throws IOException {
//        long archiveSize = 0L;
//        if (FTBBackupsConfig.COMPRESSION_LEVEL.get() > 0) {
//            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile.toFile()));
//            zos.setLevel(FTBBackupsConfig.COMPRESSION_LEVEL.get());
//
//            byte[] buffer = new byte[FTBBackupsConfig.BUFFER_SIZE.get()];
//
//            LOGGER.info("Compressing {} files...", totalFiles);
//
//            currentFile = 0;
//            manifest.forEach((absPath, archiveEntry) -> {
//                try {
//                    ZipEntry ze = new ZipEntry(archiveEntry);
//                    currentFileName = archiveEntry;
//
//                    zos.putNextEntry(ze);
//                    FileInputStream fis = new FileInputStream(absPath.toFile());
//
//                    int len;
//                    while ((len = fis.read(buffer)) > 0) {
//                        zos.write(buffer, 0, len);
//                    }
//                    zos.closeEntry();
//                    fis.close();
//                } catch (Exception ex) {
//                    LOGGER.error("Failed to read file {}: {}", archiveEntry, ex);
//                }
//
//                currentFile++;
//            });
//
//            zos.close();
//            archiveSize = Files.size(dstFile);
//        } else {
//            Files.createDirectories(backupsFolder);
//
//            currentFile = 0;
//            for (Map.Entry<Path, String> entry : manifest.entrySet()) {
//                try {
//                    Path file = entry.getKey();
//                    currentFileName = entry.getValue();
//                    Path newFile = dstFile.resolve(Path.of(entry.getValue()));
//                    Files.createDirectories(newFile.getParent());
//                    Files.copy(file, newFile);
//                    archiveSize += Files.size(newFile);
//                } catch (Exception ex) {
//                    LOGGER.error("Failed to copy file {}: {}", entry.getValue(), ex);
//                }
//
//                currentFile++;
//            }
//        }
//
//        return archiveSize;
//    }

    /**
     * Check if any existing backup archives need to be purged, based on the "backups_to_keep" and "max_total_size"
     * config settings.
     *
     * @param fileSize the file size of the archive about to be created
     */
    private void doArchiveCleanup(long fileSize) {
        if (!backups.isEmpty()) {
            backups.sort(null);

            int backupsToKeep = FTBBackupsConfig.BACKUPS_TO_KEEP.get() - 1;
            if (backupsToKeep > 0 && backups.size() > backupsToKeep) {
                while (backups.size() > backupsToKeep) {
                    Backup backup = backups.removeFirst();
                    backup.deleteFiles();
                }
            }

            long totalSize = backups.stream().mapToLong(Backup::size).sum();

            if (fileSize > 0L) {
                long freeSpace = Math.min(FTBBackupsConfig.MAX_TOTAL_SIZE.get(), backupsFolder.toFile().getFreeSpace());
                while (totalSize + fileSize > freeSpace && !backups.isEmpty()) {
                    Backup backup = backups.removeFirst();
                    if (backup.deleteFiles()) {
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

        if (FTBBackupsConfig.DISPLAY_FILE_SIZE.get()) {
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
