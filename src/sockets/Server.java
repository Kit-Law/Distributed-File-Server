package sockets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.*;

public abstract class Server
{
	protected ServerSocket serverSocket;
	
	protected final int port;
	protected final int timeout;
	
	protected Server(final int port, final int timeout)
	{
		this.port = port;
		this.timeout = timeout;
	}
	
	protected void launchServer()
	{
		try
		{
			ServerSocketChannel socket = ServerSocketChannel.open();
			serverSocket = socket.socket();
			serverSocket.bind(new InetSocketAddress(port));
			socket.configureBlocking(true);
			
			while (true)
			{
				Socket client = null;
				
				try
				{
					client = serverSocket.accept();
					//client.setSoTimeout(timeout);
					
					handleConnection(client);
				}
				catch (ClosedChannelException e)
				{
					client.close();
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected abstract void handleConnection(Socket client) throws IOException;
}
