package Sockets.fileTransfer;

import Constants.Values;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

//TODO: This is stupid...
public class FileReceiver
{
	public static void receive(SocketChannel client, Path filePath, long size)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			
			long position = transfer(client, channel, size);
			
			ByteBuffer buffer = ByteBuffer.allocate((int) size);
			
			write(channel, buffer, position);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private static long transfer(final SocketChannel client, FileChannel channel, final long bytes) throws IOException
	{
		long position = 0l;
		while (position < bytes)
		{
			position += channel.transferFrom(client, position, Values.TRANSFER_MAX_SIZE);
		}
		
		return position;
	}
	
	private static int write(FileChannel channel, final ByteBuffer buffer, long position) throws IOException
	{
		assert !Objects.isNull(buffer);
		
		int bytesWritten = 0;
		while(buffer.hasRemaining())
		{
			bytesWritten += channel.write(buffer, position + bytesWritten);
		}
		
		return bytesWritten;
	}
}