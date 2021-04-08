package Sockets;

import logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class MessageClient extends MessageSocket
{
	SocketChannel socket;
	
	public MessageClient(int port)
	{
		try
		{
			socket = connectToServer(port);
		}
		catch (IOException e)
		{
			Logger.err.log("Failed to connect to server on port " + port + ", with error, " + e.getMessage());
			System.exit(1);
		}
	}
	
	public void sendMessage(int opcode, String msg) throws IOException
	{
		sendMessage(opcode, msg, socket);
	}
	
	public String receiveMessage() throws IOException
	{
		return receiveMessage(socket);
	}
	
	private static SocketChannel connectToServer(int port) throws IOException
	{
		Logger.info.log("Connecting to socket...");
		SocketChannel socket = SocketChannel.open(new InetSocketAddress(port));
		Logger.info.log("Connected to socket: " + socket);
		
		return socket;
	}
	
	public SocketChannel getSocket() { return socket; }
}
