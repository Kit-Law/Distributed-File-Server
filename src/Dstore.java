import Constants.OpCodes;
import Sockets.Server;
import Sockets.MessageClient;
import Sockets.fileTransfer.FileReceiver;
import Sockets.fileTransfer.FileSender;
import logger.Loggable;
import logger.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Dstore extends Server implements Loggable
{
	private int cport;
	private int port;
	private int timeout;
	private String file_folder;
	private MessageClient communicator;
	
	//Dstore port cport timeout file_folder
	public static void main(String args[])
	{
		new Dstore(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
	}
	
	protected Dstore(int port, int cport, int timeout, String file_folder)
	{
		super(port, timeout);
		
		this.port = port;
		this.cport = cport;
		this.timeout = timeout;
		this.file_folder = file_folder;
		
		logger.Logger.setLogFile(this);
		
		try
		{
			this.communicator = new MessageClient(cport, timeout);
			communicator.sendMessage(OpCodes.DSTORE_CONNECT, String.valueOf(port), communicator.controller);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		launchServer();
	}
	
	@Override
	protected void handleRead(SelectionKey key) throws IOException
	{
		Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		SocketChannel client = (SocketChannel) key.channel();
		
		String msg = receiveMessage(client);
		
		switch (getOpcode(msg))
		{
			case OpCodes.DSTORE_STORE_REQUEST:
				handleStoreRequest(getOperand(msg), client);
				break;
			case OpCodes.LOAD_DATA:
				handleLoadRequest(getOperand(msg), client);
				break;
			case OpCodes.DSTORE_REMOVE_REQUEST:
				handleRemoveRequest(getOperand(msg));
		}
	}
	
	@Override
	protected void handleWrite(SelectionKey key) throws IOException
	{ }
	
	private void handleStoreRequest(String operand, SocketChannel client) throws IOException
	{
		String filename = operand.substring(0, operand.indexOf(' '));
		long filesize = Long.parseLong(operand.substring(operand.indexOf(' ')));

		sendMessage(OpCodes.ACK, "", client);
		
		FileReceiver.receive(client, Paths.get("./" + file_folder + "/" + filename), filesize);
		
		communicator.sendMessage(OpCodes.STORE_ACK, filename, communicator.controller);
	}
	
	private void handleLoadRequest(String filename, SocketChannel client) throws IOException
	{
		FileSender.transfer(Paths.get("./" + file_folder + "/" + filename), client);
	}
	
	private void handleRemoveRequest(String filename) throws IOException
	{
		Files.delete(Paths.get("./" + file_folder + "/" + filename));
		
		communicator.sendMessage(OpCodes.REMOVE_ACK, filename, communicator.controller);
	}
}
