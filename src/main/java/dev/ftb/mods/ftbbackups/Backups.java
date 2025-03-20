package dev.ftb.mods.ftbbackups;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.ftb.mods.ftbbackups.api.BackupEvent;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * TODO: Logging!
 */
public enum Backups {
    INSTANCE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static final Logger LOGGER = LoggerFactory.getLogger(Backups.class);
    public static final String BACKUPS_JSON_FILE = "backups.json";

    public final List<Backup> backups = new ArrayList<>();
    public Path backupsFolder;
    public long nextBackupTime = -1L;
    public BackupStatus backupStatus = BackupStatus.NONE;
    public boolean printFiles = false;

    public int currentFile = 0;
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
                run(server, true, Component.translatable("Server"), "");
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
            if (currentFile == 0 || currentFile == totalFiles - 1) {
                LOGGER.info("[{} | {}%]: {}", currentFile, (int) ((currentFile / (double) totalFiles) * 100D), currentFileName);
            }

            if (!FTBBackupsConfig.SILENT.get()) {
                PacketDistributor.sendToAllPlayers(BackupProgressPacket.create());
            }
        }
    }

    public void notifyAll(MinecraftServer server, Component component, boolean error) {
        component = component.plainCopy();
        component.getStyle().withColor(TextColor.fromLegacyFormat(error ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE));
        LOGGER.info(component.getString());
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
                LOGGER.error("backup creation failed: {} / {}", ex.getClass(), ex.getMessage());
                notifyAll(server, Component.translatable("ftbbackups3.lang.saving_failed"), true);
            }
        }).thenRun(() -> {
            backupStatus = BackupStatus.DONE;
        });

        return true;
    }

    /**
     * TODO: Wrap in API / Plugin system to allow for different backup providers that accept files / paths and return
     *       progress / completion status
     */
    private void createBackup(MinecraftServer server, String customName) {
        File src = server.getWorldPath(LevelResource.ROOT).toFile();

        try {
            src = src.getAbsoluteFile();
        } catch (Exception ignored) {
        }

        long now = Util.getEpochMillis();
        File dstFile;
        boolean success = false;

        String backupFileName = customName.isEmpty() ? DATE_TIME_FORMATTER.format(LocalDateTime.now()) : customName;

        Exception error = null;
        long fileSize = 0L;

        try {
            // maps files on the system to path within the archive file being created
            LinkedHashMap<File, String> fileMap = new LinkedHashMap<>();
            String mcdir = server.getServerDirectory().toRealPath().toString() + File.separator;

            Consumer<File> consumer = file0 -> {
                for (File file : BackupUtils.listTree(file0)) {
                    String s1 = file.getAbsolutePath().replace(mcdir, "");

                    boolean absolute = false;
                    if (s1.startsWith(File.separator)) {
                        s1 = s1.substring(File.separator.length());
                        absolute = true;
                    }

                    fileMap.put(file.getAbsoluteFile(), (absolute ? "_extra_absolute_" : "_extra_relative_") + File.separator + s1);
                }
            };

            for (String s : FTBBackupsConfig.EXTRA_FILES.get()) {
                consumer.accept(new File(s));
            }

            NeoForge.EVENT_BUS.post(new BackupEvent.Pre(consumer));

            for (File file : BackupUtils.listTree(src)) {
                if (file.getName().equals("session.lock") || !file.canRead()) {
                    continue;
                }

                String filePath = file.getAbsolutePath();
                fileMap.put(file.getCanonicalFile(), src.getName() + File.separator + filePath.substring(src.getAbsolutePath().length() + 1));
            }

            for (Map.Entry<File, String> entry : fileMap.entrySet()) {
                fileSize += BackupUtils.getSize(entry.getKey());
            }

            long totalSize = 0L;

            if (!backups.isEmpty()) {
                backups.sort(null);

                int backupsToKeep = FTBBackupsConfig.BACKUPS_TO_KEEP.get() - 1;

                if (backupsToKeep > 0 && backups.size() > backupsToKeep) {
                    while (backups.size() > backupsToKeep) {
                        Backup backup = backups.remove(0);
                        LOGGER.info("Deleting old backup: {}", backup.fileId());
                        BackupUtils.delete(backup.getFile());
                    }
                }

                for (Backup backup : backups) {
                    totalSize += backup.size();
                }

                if (fileSize > 0L) {
                    long freeSpace = Math.min(FTBBackupsConfig.MAX_TOTAL_SIZE.get(), backupsFolder.toFile().getFreeSpace());

                    while (totalSize + fileSize > freeSpace && !backups.isEmpty()) {
                        Backup backup = backups.remove(0);
                        totalSize -= backup.size();
                        LOGGER.info("Deleting backup to free space: {}", backup.fileId());
                        BackupUtils.delete(backup.getFile());
                    }
                }
            }

            totalFiles = fileMap.size();
            LOGGER.info("Backing up {} files...", totalFiles);
            printFiles = true;

            if (FTBBackupsConfig.COMPRESSION_LEVEL.get() > 0) {
                dstFile = BackupUtils.newFile(new File(backupsFolder.toFile(), backupFileName + ".zip"));

                long start = System.currentTimeMillis();

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                zos.setLevel(FTBBackupsConfig.COMPRESSION_LEVEL.get());

                byte[] buffer = new byte[FTBBackupsConfig.BUFFER_SIZE.get()];

                LOGGER.info("Compressing {} files...", totalFiles);

                currentFile = 0;
                for (Map.Entry<File, String> entry : fileMap.entrySet()) {
                    try {
                        ZipEntry ze = new ZipEntry(entry.getValue());
                        currentFileName = entry.getValue();

                        zos.putNextEntry(ze);
                        FileInputStream fis = new FileInputStream(entry.getKey());

                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                        fis.close();
                    } catch (Exception ex) {
                        LOGGER.error("Failed to read file {}: {}", entry.getValue(), ex);
                    }

                    currentFile++;
                }

                zos.close();
                fileSize = BackupUtils.getSize(dstFile);
                LOGGER.info("Done compressing in {} ms ({})!", System.currentTimeMillis() - start, BackupUtils.getSizeString(fileSize));
            } else {
                dstFile = new File(backupsFolder.toFile(), backupFileName);
                dstFile.mkdirs();

                currentFile = 0;
                for (Map.Entry<File, String> entry : fileMap.entrySet()) {
                    try {
                        File file = entry.getKey();
                        currentFileName = entry.getValue();
                        File dst1 = new File(dstFile, entry.getValue());
                        BackupUtils.copyFile(file, dst1);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to copy file {}: {}", entry.getValue(), ex);
                    }

                    currentFile++;
                }
            }

            LOGGER.info("Created {} from {}", dstFile.getAbsolutePath(), src.getAbsolutePath());
            success = true;
        } catch (Exception ex) {
            if (!FTBBackupsConfig.SILENT.get()) {
                String errorName = ex.getClass().getName();
                notifyAll(server, Component.translatable("ftbbackups3.lang.fail", errorName), true);
            }

            ex.printStackTrace();
            error = ex;
        }

        printFiles = false;

        Backup backup = new Backup(now, backupFileName.replace('\\', '/'), getLastIndex() + 1, success, fileSize);
        backups.add(backup);
        NeoForge.EVENT_BUS.post(new BackupEvent.Post(backup, error));

        Backup.LIST_CODEC.encodeStart(JsonOps.INSTANCE, backups)
                .ifSuccess(json -> BackupUtils.toJson(indexJsonPath(server), json, true));

        if (error == null && !FTBBackupsConfig.SILENT.get()) {
            Duration d = Duration.ZERO.plusMillis(Util.getEpochMillis() - now);
            String timeString = String.format("%d.%03ds", d.toSeconds(), d.toMillisPart());
            Component component;

            if (FTBBackupsConfig.DISPLAY_FILE_SIZE.get()) {
                long totalSize = 0L;

                for (Backup backup1 : backups) {
                    totalSize += backup1.size();
                }

                String sizeB = BackupUtils.getSizeString(fileSize);
                String sizeT = BackupUtils.getSizeString(totalSize);
                String sizeString = sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT);
                component = Component.translatable("ftbbackups3.lang.end_2", timeString, sizeString);
            } else {
                component = Component.translatable("ftbbackups3.lang.end_1", timeString);
            }

            component.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            server.getPlayerList().broadcastSystemMessage(component, true);
        }
    }

    private void appendNum(StringBuilder sb, int num, char c) {
        if (num < 10) {
            sb.append('0');
        }
        sb.append(num);
        if (c != '\0') {
            sb.append(c);
        }
    }

    private int getLastIndex() {
        int i = 0;

        for (Backup b : backups) {
            i = Math.max(i, b.index());
        }

        return i;
    }
}
