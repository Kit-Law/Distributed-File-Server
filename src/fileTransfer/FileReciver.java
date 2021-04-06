package fileTransfer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileReciver
{
	public static void recive(Path filePath)
	{
		try
		{
			FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
}


final class FileReceiver {
	
	private final int port;
	private final FileWriter fileWriter;
	private final long size;
	
	FileReceiver(final int port, final FileWriter fileWriter, final long size) {
		this.port = port;
		this.fileWriter = fileWriter;
		this.size = size;
	}
	
	void receive() throws IOException {
		SocketChannel channel = null;
		
		try (final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
			init(serverSocketChannel);
			
			channel = serverSocketChannel.accept();
			
			doTransfer(channel);
		} finally {
			if (!Objects.isNull(channel)) {
				channel.close();
			}
			
			this.fileWriter.close();
		}
	}
	
	private void doTransfer(final SocketChannel channel) throws IOException {
		assert !Objects.isNull(channel);
		
		this.fileWriter.transfer(channel, this.size);
	}
	
	private void init(final ServerSocketChannel serverSocketChannel) throws IOException {
		assert !Objects.isNull(serverSocketChannel);
		
		serverSocketChannel.bind(new InetSocketAddress(this.port));
	}
}

final class FileWriter {
	
	private final FileChannel channel;
	private static final long TRANSFER_MAX_SIZE = 1000;
	
	FileWriter(final String path) throws IOException {
		if (!Objects.isNull(path)) {
			throw new IllegalArgumentException("path required");
		}
		
		this.channel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
	}
	
	void transfer(final SocketChannel channel, final long bytes) throws IOException {
		assert !Objects.isNull(channel);
		
		long position = 0l;
		while (position < bytes) {
			position += this.channel.transferFrom(channel, position, TRANSFER_MAX_SIZE);
		}
	}
	
	int write(final ByteBuffer buffer, long position) throws IOException {
		assert !Objects.isNull(buffer);
		
		int bytesWritten = 0;
		while(buffer.hasRemaining()) {
			bytesWritten += this.channel.write(buffer, position + bytesWritten);
		}
		
		return bytesWritten;
	}
	
	void close() throws IOException {
		this.channel.close();
	}
}