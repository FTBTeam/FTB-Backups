package com.feed_the_beast.mods.ftbbackups.net;

import com.feed_the_beast.mods.ftbbackups.FTBBackupsClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class BackupProgressMessage
{
	private int current, total;

	public BackupProgressMessage(int c, int t)
	{
		current = c;
		total = t;
	}

	public BackupProgressMessage(PacketBuffer buf)
	{
		total = buf.readVarInt();
		current = buf.readVarInt();
	}

	public void write(PacketBuffer buf)
	{
		buf.writeVarInt(total);
		buf.writeVarInt(current);
	}

	public void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBBackupsClient.setFiles(current, total));
	}
}