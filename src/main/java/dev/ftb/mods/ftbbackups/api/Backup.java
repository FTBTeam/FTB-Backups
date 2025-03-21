package dev.ftb.mods.ftbbackups.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbbackups.Backups;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record Backup(long time, String fileId, int index, boolean success, long size) implements Comparable<Backup> {
    public static final Codec<Backup> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.LONG.fieldOf("time").forGetter(Backup::time),
            Codec.STRING.fieldOf("file").forGetter(Backup::fileId),
            Codec.INT.fieldOf("index").forGetter(Backup::index),
            Codec.BOOL.fieldOf("success").forGetter(Backup::success),
            Codec.LONG.fieldOf("size").forGetter(Backup::size)
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

    public Path getPath() {
        return Backups.INSTANCE.backupsFolder.resolve(fileId);
    }

    public boolean deleteFiles() {
        Path path = getPath();
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
