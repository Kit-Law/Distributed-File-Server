package sockets.fileTransfer;

import constants.Values;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//TODO: This is stupid...
public class FileReceiver
{
	public static void receive(final Socket client, final Path filePath, final long size)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			
			transfer(client, channel, size);
			
			channel.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private static void transfer(final Socket client, final FileChannel channel, final long bytes) throws IOException
	{
		long position = 0L;
		while (position < bytes)
		{
			position += channel.transferFrom(client.getChannel(), position, Values.TRANSFER_MAX_SIZE);
		}
	}
}