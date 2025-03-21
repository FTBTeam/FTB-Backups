package dev.ftb.mods.ftbbackups.api.event;

import dev.ftb.mods.ftbbackups.api.Backup;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Consumer;

public class BackupEvent extends Event {
    public static class Pre extends BackupEvent {
        private final Consumer<Path> callback;

        public Pre(Consumer<Path> callback) {
            this.callback = callback;
        }

        public void add(Path file) {
            callback.accept(file);
        }
    }

    public static class Post extends BackupEvent {
        private final Backup backup;
        private final Exception error;

        public Post(Backup backup, @Nullable Exception error) {
            this.backup = backup;
            this.error = error;
        }

        public Backup getBackup() {
            return backup;
        }

        @Nullable
        public Exception getError() {
            return error;
        }
    }
}
