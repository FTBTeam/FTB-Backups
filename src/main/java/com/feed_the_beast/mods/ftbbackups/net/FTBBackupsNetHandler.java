package com.feed_the_beast.mods.ftbbackups.net;

import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class FTBBackupsNetHandler
{
	public static final SimpleNetworkWrapper NET = new SimpleNetworkWrapper(FTBBackups.MOD_ID);

	public static void init()
	{
		NET.registerMessage(new MessageBackupProgress.Handler(), MessageBackupProgress.class, 1, Side.CLIENT);
	}
}