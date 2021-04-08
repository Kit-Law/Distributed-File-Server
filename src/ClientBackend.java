import Constants.OpCodes;
import Sockets.MessageClient;
import Sockets.fileTransfer.FileReceiver;
import Sockets.fileTransfer.FileSender;
import logger.Loggable;
import logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ClientBackend implements Loggable
{
	private final int cport;
	private final int timeout;
	
	private MessageClient controller;
	
	ClientBackend(final int cport, final int timeout)
	{
		this.cport = cport;
		this.timeout = timeout;
		
		Logger.setLogFile(this);
		
		controller = new MessageClient(cport);
	}
	
	protected void store(String filename)
	{
		try
		{
			controller.sendMessage(OpCodes.CONTROLLER_STORE_REQUEST, filename);
			
			String response = controller.receiveMessage();
			
			if (MessageClient.getOpcode(response) != OpCodes.STORE_TO)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
			
			Arrays.asList(MessageClient.getOperand(response).split(" ")).forEach(s -> {
				try { storeToDstore(filename, Integer.parseInt(s)); }
				catch (IOException e) { e.printStackTrace(); }
			});
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private void storeToDstore(final String filename, final int port) throws IOException
	{
		Path filePath = Paths.get(filename);
		long fileSize = Files.size(filePath);
		
		MessageClient dstore = new MessageClient(port);
		
		dstore.sendMessage(OpCodes.DSTORE_STORE_REQUEST, filename + " " + fileSize);
		
		if (Integer.parseInt(dstore.receiveMessage()) != OpCodes.ACK)
			throw new IOException("??");
		
		FileSender.transfer(filePath, dstore.getSocket());
	}
	
	protected void load(final String filename)
	{
		try
		{
			controller.sendMessage(OpCodes.CONTROLLER_LOAD_REQUEST, filename);
			
			String response = controller.receiveMessage();
			
			if (MessageClient.getOpcode(response) != OpCodes.LOAD_FROM)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
			
			MessageClient dstore = new MessageClient(Integer.parseInt(MessageClient.getOperand(response)));
			
			dstore.sendMessage(OpCodes.LOAD_DATA, filename);
			
			int size = 0; //TODO: wtf kirk
			FileReceiver.receive(dstore.getSocket(), Paths.get("./" + filename), size);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	protected void remove(final String filename)
	{
		try
		{
			controller.sendMessage(OpCodes.CONTROLLER_REMOVE_REQUEST, filename);
			
			String response = controller.receiveMessage();
			
			if (MessageClient.getOpcode(response) != OpCodes.REMOVE_COMPLETE)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@Override
	public final String toString()
	{
		return "Client-" + cport + "-" + System.currentTimeMillis();
	}
}
