package Sockets;

import logger.Loggable;
import logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class messageClient extends MessageSocket implements Loggable
{
	protected SocketChannel controller;
	
	protected int cport = -1;
	protected int timeout = -1;
	
	protected messageClient(int cport, int timeout)
	{
		this.cport = cport;
		this.timeout = timeout;
		Logger.setLogFile(this);
		
		try
		{
			connectToServer();
		}
		catch (IOException e)
		{
			Logger.err.log("Failed to connect to server on port " + cport + ", with error, " + e.getMessage());
			System.exit(1);
		}
	}
	
	private void connectToServer() throws IOException
	{
		Logger.info.log("Connecting to controller...");
		controller = SocketChannel.open(new InetSocketAddress(cport));
		Logger.info.log("Connected to controller: " + controller);
	}
	
	@Override
	public String toString()
	{
		return "Client-" + cport + "-" + System.currentTimeMillis();
	}
}
