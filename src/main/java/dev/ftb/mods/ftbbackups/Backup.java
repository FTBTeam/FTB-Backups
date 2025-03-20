package dev.ftb.mods.ftbbackups;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
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

    public File getFile() {
        return new File(Backups.INSTANCE.backupsFolder.toFile(), fileId);
    }

    @Override
    public int compareTo(Backup o) {
        return Long.compare(time, o.time);
    }
}
