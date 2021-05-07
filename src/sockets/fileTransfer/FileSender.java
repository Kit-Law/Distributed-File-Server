package sockets.fileTransfer;

import constants.Values;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileSender
{
	private SocketChannel client;
	
	public static void transfer(final Path filePath, final int port)
	{
		try { transfer(filePath, new Socket(InetAddress.getLoopbackAddress(), port)); }
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void transfer(final Path filePath, final Socket client) throws IOException
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
			
			long position = 0L;
			long size = channel.size();
			
			while (position < size)
			{
				position += channel.transferTo(position, Values.TRANSFER_MAX_SIZE, client.getChannel());
			}
			
			channel.close();
		}
		catch (IOException e) { throw e; }
	}
}