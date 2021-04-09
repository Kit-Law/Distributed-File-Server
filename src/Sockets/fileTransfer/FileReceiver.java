package Sockets.fileTransfer;

import Constants.Values;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//TODO: This is stupid...
public class FileReceiver
{
	public static void receive(final SocketChannel client, final Path filePath, final long size)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			
			transfer(client, channel, size);
			
			channel.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private static void transfer(final SocketChannel client, final FileChannel channel, final long bytes) throws IOException
	{
		long position = 0L;
		while (position < bytes)
		{
			position += channel.transferFrom(client, position, Values.TRANSFER_MAX_SIZE);
		}
	}
}