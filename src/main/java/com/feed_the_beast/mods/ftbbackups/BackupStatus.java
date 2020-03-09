package com.feed_the_beast.mods.ftbbackups;

/**
 * @author LatvianModder
 */
public enum BackupStatus
{
	NONE,
	RUNNING,
	DONE;

	public boolean isRunning()
	{
		return this == RUNNING;
	}

	public boolean isDone()
	{
		return this == DONE;
	}

	public boolean isRunningOrDone()
	{
		return isRunning() || isDone();
	}
}