package dev.ftb.mods.ftbbackups.net;

import dev.ftb.mods.ftbbackups.Backups;
import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.config.FTBBackupsServerConfig;
import dev.ftb.mods.ftbbackups.client.BackupsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record BackupProgressPacket(int current, int total) implements CustomPacketPayload {
    public static final Type<BackupProgressPacket> TYPE = new Type<>(FTBBackups.id("backup_progress"));

    public static final StreamCodec<FriendlyByteBuf, BackupProgressPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackupProgressPacket::current,
            ByteBufCodecs.VAR_INT, BackupProgressPacket::total,
            BackupProgressPacket::new
    );

    public static void handler(BackupProgressPacket message, IPayloadContext context) {
        BackupsClient.setBackupProgress(message.current, message.total);
    }

    public static BackupProgressPacket start() {
        return new BackupProgressPacket(0, Backups.getServerInstance().getTotalFileCount());
    }

    public static BackupProgressPacket update() {
        return new BackupProgressPacket(Backups.getServerInstance().getCurrentFileIndex(), Backups.getServerInstance().getTotalFileCount());
    }

    public static BackupProgressPacket complete() {
        return new BackupProgressPacket(Backups.getServerInstance().getTotalFileCount(), Backups.getServerInstance().getTotalFileCount());
    }

    public static void sendProgress(MinecraftServer server, BackupProgressPacket packet) {
        getPlayersToNotify(server).forEach(sp -> PacketDistributor.sendToPlayer(sp, packet));
    }

    private static List<ServerPlayer> getPlayersToNotify(MinecraftServer server) {
        return FTBBackupsServerConfig.NOTIFY_ADMINS_ONLY.get() ?
                server.getPlayerList().getPlayers().stream()
                        .filter(sp -> sp.hasPermissions(2) || server.isSingleplayerOwner(sp.getGameProfile()))
                        .toList() :
                server.getPlayerList().getPlayers();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
