package dev.ftb.mods.ftbbackups.api.event;

import dev.ftb.mods.ftbbackups.api.Backup;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class BackupEvent extends Event {
    /**
     * Fired just before a backup is made. This can be used to add extra files to the backup; files added
     * <strong>must</strong> be relative to the game instance directory.
     */
    public static class Pre extends BackupEvent {
        private final Consumer<Path> callback;

        public Pre(Consumer<Path> callback) {
            this.callback = callback;
        }

        public void add(Path file) {
            callback.accept(file);
        }
    }

    /**
     * Fired immediately after a backup is made.
     */
    public static class Post extends BackupEvent {
        private final Backup backup;
        private final Exception error;

        public Post(Backup backup, @Nullable Exception error) {
            this.backup = backup;
            this.error = error;
        }

        /**
         * {@return the details for the backup that was just made.
         */
        public Backup getBackup() {
            return backup;
        }

        /**
         * Get the error, if any, that occurred during this backup.
         * @return the exception that was thrown, or {@code Optional.empty()} if the backup was successful
         */
        public Optional<Exception> getError() {
            return Optional.ofNullable(error);
        }
    }
}
