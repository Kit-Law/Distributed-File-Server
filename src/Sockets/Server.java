package Sockets;

import logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public abstract class Server
{
	protected final int port;
	protected final int timeout;
	
	protected Selector selector = null;
	
	protected Server(final int port, final int timeout)
	{
		this.port = port;
		this.timeout = timeout;
	}
	
	protected void launchServer()
	{
		try
		{
			selector = Selector.open();
			ServerSocketChannel socket = ServerSocketChannel.open();
			ServerSocket serverSocket = socket.socket();
			serverSocket.bind(new InetSocketAddress(port));
			socket.configureBlocking(false);
			int ops = socket.validOps();
			socket.register(selector, ops, null);
			
			while (true)
			{
				try
				{
					selector.select();
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iter = selectedKeys.iterator();
					
					while (iter.hasNext())
					{
						SelectionKey key = iter.next();
						
						if (key.isAcceptable())
						{
							handleAccept(socket, key);
						}
						
						if (key.isReadable())
						{
							handleRead(key);
						}
						
						iter.remove();
					}
				}
				catch (ClosedChannelException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void handleAccept(ServerSocketChannel mySocket, SelectionKey key) throws IOException
	{
		// Accept the connection and set non-blocking mode
		SocketChannel client = mySocket.accept();
		client.configureBlocking(false);
		
		// Register that client is reading this channel
		client.register(selector, SelectionKey.OP_READ);
		
		Logger.info.log("Connection Accepted from: " + client.toString() + ".");
	}
	
	protected abstract void handleRead(SelectionKey key) throws IOException;
	protected abstract void handleWrite(SelectionKey key) throws IOException;
}
