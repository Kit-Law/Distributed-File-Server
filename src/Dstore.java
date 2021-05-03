//import Constants.OpCodes;
import Constants.Protocol;
import Sockets.MessageSocket;
import Sockets.Server;
import Sockets.MessageClient;
import Sockets.fileTransfer.FileReceiver;
import Sockets.fileTransfer.FileSender;
//import logger.Loggable;
import logger.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Dstore extends Server //implements Loggable
{
	private int cport;
	private int port;
	private int timeout;
	private String file_folder;
	
	private MessageClient controller;
	
	//Dstore port cport timeout file_folder
	public static void main(String[] args) throws InterruptedException
	{
		/*Thread d1 = new Thread("d1")
		{
			public void run() { new Dstore(401, Integer.parseInt(args[1]), Integer.parseInt(args[2]), "./d1"); }
		};
		
		Thread d2 = new Thread("d2")
		{
			public void run() { new Dstore(402, Integer.parseInt(args[1]), Integer.parseInt(args[2]), "./d2"); }
		};
		
		Thread d3 = new Thread("d3")
		{
			public void run() { new Dstore(403, Integer.parseInt(args[1]), Integer.parseInt(args[2]), "./d3"); }
		};
		
		//Begin all of the threads.
		d1.start();
		d2.start();
		d3.start();
		
		//Wait for all of the threads to finish.
		d1.join();
		d2.join();
		d3.join();*/
		
		new Dstore(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
	}
	
	protected Dstore(final int port, final int cport, final int timeout, final String file_folder)
	{
		super(port, timeout);
		
		this.port = port;
		this.cport = cport;
		this.timeout = timeout;
		this.file_folder = file_folder;
		
		//logger.Logger.setLogFile(this);
		
		try
		{
			if (!Files.isDirectory(Paths.get(file_folder)))
				Files.createDirectory(Paths.get(file_folder));
			
			controller = new MessageClient(cport);
			controller.sendMessage(Protocol.JOIN_TOKEN, String.valueOf(port));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		launchServer();
	}
	
	@Override
	protected void handleRead(final SelectionKey key) throws IOException
	{
		//Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		SocketChannel client = (SocketChannel) key.channel();
		
		String msg = MessageSocket.receiveMessage(client);
		//TODO: fix this
		if (msg.equals(""))
			return;
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.STORE_TOKEN:
				handleStoreRequest(MessageSocket.getOperand(msg), client);
				break;
			case Protocol.LOAD_DATA_TOKEN:
				handleLoadRequest(MessageSocket.getOperand(msg), client);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(MessageSocket.getOperand(msg));
		}
		
		key.cancel();
		key.channel().close();
	}
	
	@Override
	protected void handleWrite(final SelectionKey key) throws IOException
	{ }
	
	private void handleStoreRequest(final String operand, final SocketChannel client) throws IOException
	{
		String filename = operand.substring(0, operand.indexOf(' '));
		long filesize = Long.parseLong(operand.substring(operand.indexOf(' ') + 1));
		
		MessageSocket.sendMessage(Protocol.ACK_TOKEN, "", client);
		
		FileReceiver.receive(client, Paths.get(file_folder + "/" + filename), filesize);
		
		controller.sendMessage(Protocol.STORE_ACK_TOKEN, filename + " " + port + " " + filesize);
	}
	
	private void handleLoadRequest(final String filename, final SocketChannel client)
	{
		FileSender.transfer(Paths.get("./" + file_folder + "/" + filename), client);
	}
	
	private void handleRemoveRequest(final String filename) throws IOException
	{
		Files.delete(Paths.get("./" + file_folder + "/" + filename));
		
		controller.sendMessage(Protocol.REMOVE_ACK_TOKEN, filename);
	}
	
	@Override
	public final String toString()
	{
		return "DStore-" + port + "-" + System.currentTimeMillis();
	}
}
