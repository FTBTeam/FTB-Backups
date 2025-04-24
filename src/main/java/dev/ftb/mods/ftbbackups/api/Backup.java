package dev.ftb.mods.ftbbackups.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbbackups.Backups;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Holds information about one specific backup.
 *
 * @param time time the backup was created (milliseconds since the epoch)
 * @param archivalPlugin the archival plugin used to create this backup
 * @param fileId name of the backup on disk, relative to the backup directory defined in config (could be a file or a directory)
 * @param worldName human-readable name of the world that is backed up
 * @param index a monotonically increasing numeric index
 * @param success true if the backup succeeded, false if there were any problems
 * @param size nominal size of the backup in bytes
 */
public record Backup(long time, ResourceLocation archivalPlugin, String fileId, String worldName, int index, boolean success, long size, int fileCount) implements Comparable<Backup> {
    public static final Codec<Backup> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.LONG.fieldOf("time").forGetter(Backup::time),
            ResourceLocation.CODEC.fieldOf("archival_plugin").forGetter(Backup::archivalPlugin),
            Codec.STRING.fieldOf("file").forGetter(Backup::fileId),
            Codec.STRING.optionalFieldOf("world_name", "<not known>").forGetter(Backup::worldName),
            Codec.INT.fieldOf("index").forGetter(Backup::index),
            Codec.BOOL.fieldOf("success").forGetter(Backup::success),
            Codec.LONG.fieldOf("size").forGetter(Backup::size),
            Codec.INT.fieldOf("file_count").forGetter(Backup::fileCount)
    ).apply(builder, Backup::new));

    public static final Codec<List<Backup>> LIST_CODEC = Codec.list(CODEC);

    public int hashCode() {
        return Long.hashCode(time);
    }

    public String toString() {
        return fileId;
    }

    public boolean equals(Object o) {
        return o == this || (o instanceof Backup && ((Backup) o).time == time);
    }

    public boolean deleteFiles(Path backupsFolder) {
        Path path = backupsFolder.resolve(fileId);
        try {
            if (Files.isRegularFile(path)) {
                Files.delete(path);
            } else if (Files.isDirectory(path)) {
                FileUtils.deleteDirectory(path.toFile());
            }
        } catch (IOException e) {
            Backups.LOGGER.error("Can't delete backup {}: {} / {}", fileId(), e.getClass(), e.getMessage());
            return false;
        }
        Backups.LOGGER.info("Deleted backup: {}", fileId());
        return true;
    }

    @Override
    public int compareTo(Backup o) {
        return Long.compare(time, o.time);
    }
}
