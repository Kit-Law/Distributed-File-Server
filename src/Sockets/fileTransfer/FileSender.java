package Sockets.fileTransfer;

import Constants.Values;

import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileSender
{
	private SocketChannel client;
	
	public static void transfer(final Path filePath, final int port)
	{
		try { transfer(filePath, SocketChannel.open(new InetSocketAddress(port))); }
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void transfer(final Path filePath, final SocketChannel client)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
			
			long position = 0L;
			long size = channel.size();
			
			while (position < size)
			{
				position += channel.transferTo(position, Values.TRANSFER_MAX_SIZE, client);
			}
			
			channel.close();
			client.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}