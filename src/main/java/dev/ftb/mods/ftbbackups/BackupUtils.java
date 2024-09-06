package dev.ftb.mods.ftbbackups;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class BackupUtils
{
	public static final long KB = 1024L;
	public static final long MB = KB * 1024L;
	public static final long GB = MB * 1024L;
	public static final long TB = GB * 1024L;

	public static final double KB_D = 1024D;
	public static final double MB_D = KB_D * 1024D;
	public static final double GB_D = MB_D * 1024D;
	public static final double TB_D = GB_D * 1024D;

	public static String getTimeString(long millis)
	{
		boolean neg = false;
		if (millis < 0L)
		{
			neg = true;
			millis = -millis;
		}

		StringBuilder sb = new StringBuilder();

		if (millis < 1000L)
		{
			if (neg)
			{
				sb.append('-');
			}

			sb.append(millis);
			sb.append('m');
			sb.append('s');
			return sb.toString();
		}

		long secs = millis / 1000L;

		if (neg)
		{
			sb.append('-');
		}

		long h = (secs / 3600L) % 24;
		long m = (secs / 60L) % 60L;
		long s = secs % 60L;

		if (secs >= 86400L)
		{
			sb.append(secs / 86400L);
			sb.append('d');
			sb.append(' ');
		}

		if (h > 0 || secs >= 86400L)
		{
			if (h < 10)
			{
				sb.append('0');
			}
			sb.append(h);
			//sb.append("h ");
			sb.append(':');
		}

		if (m < 10)
		{
			sb.append('0');
		}
		sb.append(m);
		//sb.append("m ");
		sb.append(':');
		if (s < 10)
		{
			sb.append('0');
		}
		sb.append(s);
		//sb.append('s');

		return sb.toString();
	}

	public static long getSize(File file)
	{
		if (!file.exists())
		{
			return 0L;
		}
		else if (file.isFile())
		{
			return file.length();
		}
		else if (file.isDirectory())
		{
			long length = 0L;
			File[] f1 = file.listFiles();
			if (f1 != null && f1.length > 0)
			{
				for (File aF1 : f1)
				{
					length += getSize(aF1);
				}
			}
			return length;
		}
		return 0L;
	}

	public static String getSizeString(double b)
	{
		if (b >= TB_D)
		{
			return String.format("%.1fTB", b / TB_D);
		}
		else if (b >= GB_D)
		{
			return String.format("%.1fGB", b / GB_D);
		}
		else if (b >= MB_D)
		{
			return String.format("%.1fMB", b / MB_D);
		}
		else if (b >= KB_D)
		{
			return String.format("%.1fKB", b / KB_D);
		}

		return ((long) b) + "B";
	}

	public static String getSizeString(File file)
	{
		return getSizeString(getSize(file));
	}

	public static File newFile(File file)
	{
		if (!file.exists())
		{
			try
			{
				File parent = file.getParentFile();
				if (!parent.exists())
				{
					parent.mkdirs();
				}
				file.createNewFile();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return file;
	}

	public static List<File> listTree(File file)
	{
		List<File> l = new ArrayList<>();
		listTree0(l, file);
		return l;
	}

	public static void listTree0(List<File> list, File file)
	{
		if (file.isDirectory())
		{
			File[] fl = file.listFiles();

			if (fl != null && fl.length > 0)
			{
				for (File aFl : fl)
				{
					listTree0(list, aFl);
				}
			}
		}
		else if (file.isFile())
		{
			list.add(file);
		}
	}

	public static void copyFile(File src, File dst) throws Exception
	{
		if (src.exists() && !src.equals(dst))
		{
			if (src.isDirectory() && dst.isDirectory())
			{
				for (File f : listTree(src))
				{
					File dst1 = new File(dst.getAbsolutePath() + File.separatorChar + (f.getAbsolutePath().replace(src.getAbsolutePath(), "")));
					copyFile(f, dst1);
				}
			}
			else
			{
				dst = newFile(dst);

				try (FileInputStream fis = new FileInputStream(src);
					 FileOutputStream fos = new FileOutputStream(dst);
					 FileChannel srcC = fis.getChannel();
					 FileChannel dstC = fos.getChannel())
				{
					dstC.transferFrom(srcC, 0L, srcC.size());
				}
			}
		}
	}

	public static boolean delete(File file)
	{
		if (!file.exists())
		{
			return false;
		}
		else if (file.isFile())
		{
			return file.delete();
		}

		String[] files = file.list();

		if (files != null)
		{
			for (String s : files)
			{
				delete(new File(file, s));
			}
		}

		return file.delete();
	}

	public static void toJson(Writer writer, @Nullable JsonElement element, boolean prettyPrinting)
	{
		if (element == null || element.isJsonNull())
		{
			try
			{
				writer.write("null");
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			return;
		}

		JsonWriter jsonWriter = new JsonWriter(writer);
		jsonWriter.setLenient(true);
		jsonWriter.setHtmlSafe(false);
		jsonWriter.setSerializeNulls(true);

		if (prettyPrinting)
		{
			jsonWriter.setIndent("\t");
		}

		try
		{
			Streams.write(element, jsonWriter);
		}
		catch (Exception ex)
		{
			throw new JsonIOException(ex);
		}
	}

	public static void toJson(File file, @Nullable JsonElement element, boolean prettyPrinting)
	{
		try (OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(newFile(file)), StandardCharsets.UTF_8);
			 BufferedWriter writer = new BufferedWriter(output))
		{
			toJson(writer, element, prettyPrinting);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static JsonElement readJson(File file)
	{
		if (!file.exists())
		{
			return JsonNull.INSTANCE;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
		{
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setLenient(true);
			JsonElement element = Streams.parse(jsonReader);

			if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT)
			{
				throw new JsonSyntaxException("Did not consume the entire document.");
			}

			return element;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	public static String removeAllWhitespace(String s)
	{
		char[] chars = new char[s.length()];
		int j = 0;

		for (int i = 0; i < chars.length; i++)
		{
			char c = s.charAt(i);

			if (c > ' ')
			{
				chars[j] = c;
				j++;
			}
		}

		return new String(chars, 0, j);
	}
}