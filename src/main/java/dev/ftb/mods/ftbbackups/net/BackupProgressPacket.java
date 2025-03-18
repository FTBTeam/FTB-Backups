package dev.ftb.mods.ftbbackups.net;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.FTBBackupsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BackupProgressPacket(int current, int total) implements CustomPacketPayload {
	public static final Type<BackupProgressPacket> TYPE = new Type<>(FTBBackups.id("backup_progress"));

	public static final StreamCodec<FriendlyByteBuf, BackupProgressPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, BackupProgressPacket::current,
			ByteBufCodecs.VAR_INT, BackupProgressPacket::total,
			BackupProgressPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handler(BackupProgressPacket message, IPayloadContext context) {
		FTBBackupsClient.setFiles(message.current, message.total);
	}
}