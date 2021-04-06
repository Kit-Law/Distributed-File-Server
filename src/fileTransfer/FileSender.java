package fileTransfer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileSender
{
	private SocketChannel client;
	private static final long TRANSFER_MAX_SIZE = 1000;
	
	public static void transfer(Path filePath, int port)
	{
		try { transfer(filePath, SocketChannel.open(new InetSocketAddress(port))); }
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void transfer(Path filePath, SocketChannel client)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
			
			long position = 0l;
			long size = channel.size();
			
			while (position < size)
			{
				position += channel.transferTo(position, TRANSFER_MAX_SIZE, client);
			}
			
			channel.close();
			client.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}