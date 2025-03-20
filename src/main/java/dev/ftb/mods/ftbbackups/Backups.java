package dev.ftb.mods.ftbbackups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.ftb.mods.ftbbackups.api.BackupEvent;
import dev.ftb.mods.ftbbackups.net.BackupProgressPacket;
import net.minecraft.ChatFormatting;
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
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * TODO: Logging!
 */
public enum Backups {
    INSTANCE;

    public static final Logger LOGGER = LoggerFactory.getLogger(Backups.class);

    public final List<Backup> backups = new ArrayList<>();
    public File backupsFolder;
    public long nextBackup = -1L;
    public BackupStatus doingBackup = BackupStatus.NONE;
    public boolean printFiles = false;

    public int currentFile = 0;
    public int totalFiles = 0;
    private String currentFileName = "";
    public boolean hadPlayersOnline = false;

    public void init(MinecraftServer server) {
        Path dataDir = server.getServerDirectory();
        String folder = FTBBackupsConfig.FOLDER.get();
        backupsFolder = folder.trim().isEmpty() ?
                FMLPaths.GAMEDIR.get().resolve("backups").toFile() :
                new File(folder);

        try {
            backupsFolder = backupsFolder.getCanonicalFile();
        } catch (Exception ignored) {
        }

        doingBackup = BackupStatus.NONE;
        backups.clear();

        File file = new File(dataDir.toFile(), "local/ftbutilities/backups.json");

        if (!file.exists()) {
            File oldFile = new File(backupsFolder, "backups.json");

            if (oldFile.exists()) {
                oldFile.renameTo(file);
            }
        }

        JsonElement element = BackupUtils.readJson(file);

        if (element != null && element.isJsonArray()) {
            try {
                for (JsonElement e : element.getAsJsonArray()) {
                    JsonObject json = e.getAsJsonObject();

                    if (!json.has("size")) {
                        json.addProperty("size", BackupUtils.getSize(new File(backupsFolder, json.get("file").getAsString())));
                    }

                    Backup.CODEC.parse(JsonOps.INSTANCE, json).ifSuccess(backups::add);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        LOGGER.info("Backups folder - {}", backupsFolder.getAbsolutePath());
        nextBackup = System.currentTimeMillis() + FTBBackupsConfig.getBackupTimerMillis();
    }

    public void tick(MinecraftServer server, long now) {
        if (nextBackup > 0L && nextBackup <= now) {
            if (!FTBBackupsConfig.ONLY_IF_PLAYERS_ONLINE.get() || hadPlayersOnline || !server.getPlayerList().getPlayers().isEmpty()) {
                hadPlayersOnline = false;
                run(server, true, Component.translatable("Server"), "");
            }
        }

        if (doingBackup.isDone()) {
            doingBackup = BackupStatus.NONE;

            for (ServerLevel world : server.getAllLevels()) {
                if (world != null) {
                    world.noSave = false;
                }
            }

            if (!FTBBackupsConfig.SILENT.get()) {
                PacketDistributor.sendToAllPlayers(BackupProgressPacket.reset());
            }
        } else if (doingBackup.isRunning() && printFiles) {
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
        if (doingBackup.isRunningOrDone()) {
            return false;
        }

        if (auto && !FTBBackupsConfig.AUTO.get()) {
            return false;
        }

        notifyAll(server, Component.translatable("ftbbackups3.lang.start", name), false);
        nextBackup = System.currentTimeMillis() + FTBBackupsConfig.getBackupTimerMillis();

        for (ServerLevel world : server.getAllLevels()) {
            if (world != null) {
                world.noSave = true;
            }
        }

        doingBackup = BackupStatus.RUNNING;
        server.getPlayerList().saveAll();

        new Thread(() -> {
            try {
                try {
                    createBackup(server, customName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    notifyAll(server, Component.translatable("ftbbackups3.lang.saving_failed"), true);
                }
            } catch (Exception ex) {
                notifyAll(server, Component.translatable("ftbbackups3.lang.saving_failed"), true);
                ex.printStackTrace();
            }

            doingBackup = BackupStatus.DONE;
        }).start();

        return true;
    }

    /**
     * TODO: Wrap in API / Plugin system to allow for different backup providers that accept files / paths and return
     *       progress / completion status
     */
    private void createBackup(MinecraftServer server, String customName) {
        File src = server.getWorldPath(LevelResource.ROOT).toFile();

        try {
            src = src.getCanonicalFile();
        } catch (Exception ex) {
        }

        Calendar time = Calendar.getInstance();
        File dstFile;
        boolean success = false;
        StringBuilder out = new StringBuilder();

        if (customName.isEmpty()) {
            appendNum(out, time.get(Calendar.YEAR), '-');
            appendNum(out, time.get(Calendar.MONTH) + 1, '-');
            appendNum(out, time.get(Calendar.DAY_OF_MONTH), '-');
            appendNum(out, time.get(Calendar.HOUR_OF_DAY), '-');
            appendNum(out, time.get(Calendar.MINUTE), '-');
            appendNum(out, time.get(Calendar.SECOND), '\0');
        } else {
            out.append(customName);
        }

        Exception error = null;
        long fileSize = 0L;

        try {
            LinkedHashMap<File, String> fileMap = new LinkedHashMap<>();
            String mcdir = server.getServerDirectory().getFileSystem().toString();

            Consumer<File> consumer = file0 -> {
                for (File file : BackupUtils.listTree(file0)) {
                    String s1 = file.getAbsolutePath().replace(mcdir, "");

                    if (s1.startsWith(File.separator)) {
                        s1 = s1.substring(File.separator.length());
                    }

                    fileMap.put(file, "_extra_" + File.separator + s1);
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
                fileMap.put(file, src.getName() + File.separator + filePath.substring(src.getAbsolutePath().length() + 1));
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
                    long freeSpace = Math.min(FTBBackupsConfig.MAX_TOTAL_SIZE.get(), backupsFolder.getFreeSpace());

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
                out.append(".zip");
                dstFile = BackupUtils.newFile(new File(backupsFolder, out.toString()));

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
                dstFile = new File(backupsFolder, out.toString());
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

        Backup backup = new Backup(time.getTimeInMillis(), out.toString().replace('\\', '/'), getLastIndex() + 1, success, fileSize);
        backups.add(backup);
        NeoForge.EVENT_BUS.post(new BackupEvent.Post(backup, error));

        JsonArray array = new JsonArray();

        for (Backup backup1 : backups) {
            Backup.CODEC.encodeStart(JsonOps.INSTANCE, backup1).ifSuccess(array::add);
        }

        BackupUtils.toJson(new File(server.getServerDirectory().toFile(), "local/ftbutilities/backups.json"), array, true);

        if (error == null && !FTBBackupsConfig.SILENT.get()) {
            Duration d = Duration.ZERO.plusMillis(System.currentTimeMillis() - time.getTimeInMillis());
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
