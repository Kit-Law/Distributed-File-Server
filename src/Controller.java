import Constants.OpCodes;
import Sockets.MessageSocket;
import Sockets.Server;
import database.MetaData;
import database.State;
import logger.Loggable;
import logger.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Controller extends Server implements Loggable
{
	private int R = -1;
	private int cport = -1;
	private static HashMap<String, MetaData> database = new HashMap<String, MetaData>();
	
	//java Controller cport R timeout
	public static void main(String args[])
	{
		new Controller(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	}
	
	private Controller(int cport, int R, int timeout)
	{
		super(cport, timeout);
		this.R = R;
		
		logger.Logger.setLogFile(this);
		
		launchServer();
	}
	
	@Override
	protected void handleRead(SelectionKey key) throws IOException
	{
		Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		SocketChannel client = (SocketChannel) key.channel();
		
		String msg = MessageSocket.receiveMessage(client);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case OpCodes.DSTORE_CONNECT:
				handleDstoreConnect(MessageSocket.getOperand(msg), client);
			case OpCodes.CONTROLLER_STORE_REQUEST:
				handleStoreRequest(MessageSocket.getOperand(msg), client);
				break;
			case OpCodes.CONTROLLER_LOAD_REQUEST:
				handleLoadRequest(MessageSocket.getOperand(msg), client);
				break;
			case OpCodes.CONTROLLER_REMOVE_REQUEST:
				handleRemoveRequest(MessageSocket.getOperand(msg), client);
				break;
		}
	}
	
	@Override
	protected void handleWrite(SelectionKey key) throws IOException
	{
		String msg = "Hi!";
		
		SocketChannel socket = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
		buffer.put(msg.getBytes());
		buffer.flip();
		int bytesWritten = socket.write(buffer);
		
		Logger.info.log("Sent message: \"" + msg + "\", " + bytesWritten + " bytes to: " + socket + ".");
		
		socket.register(selector, SelectionKey.OP_READ);
	}
	
	public static void saveDatabase()
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream("Database.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(database);
			out.close();
			fileOut.close();
			Logger.info.log("Database saved.");
		}
		catch (IOException i) { i.printStackTrace(); }
	}
	
	public static void loadDatabase()
	{
		try
		{
			FileInputStream fileIn = new FileInputStream("Database.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			database = (HashMap<String, MetaData>) in.readObject();
			in.close();
			fileIn.close();
			Logger.info.log("Database loaded.");
		}
		catch (Exception e) { System.err.println(e.getMessage() + e.getCause()); }
	}
	
	@Override
	public String toString()
	{
		return "Controller-" + cport + "-" + System.currentTimeMillis();
	}
	
	private void handleDstoreConnect(String port, SocketChannel dstore)
	{
	
	}
	
	private void handleStoreRequest(String file, SocketChannel client) throws IOException
	{
		//Check that the file does not already exist.
		if (database.containsKey(file))
			//TODO: make an exception class
			throw new IOException("File already existed lol");
		
		//Create a new database entry.
		database.put(file, new MetaData(State.STORE_IN_PROGRESS));
		
		//Get R of the dstores.
		List<SocketChannel> dstores = getRdstores();
		int[] dstorePorts = getPorts(dstores);
		
		//Set the dsotres ready to read.
		dstores.forEach(dstore -> {
			try { dstore.register(selector, SelectionKey.OP_READ); }
			catch (ClosedChannelException e) { e.printStackTrace(); }
		});
		
		//Send the ports to the client.
		MessageSocket.sendMessage(OpCodes.STORE_TO, Arrays.toString(dstorePorts), client);
	}
	
	private List<SocketChannel> getRdstores() { return new ArrayList<SocketChannel>(); }
	private int[] getPorts(List<SocketChannel> dstores) { return new int[0]; }
	
	private void handleLoadRequest(String file, SocketChannel client)
	{
		try
		{
			int port = selectDstore(file);
			MessageSocket.sendMessage(OpCodes.LOAD_FROM, String.valueOf(port), client);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private int selectDstore(String file) { return database.get(file).getDstorePorts()[0]; }
	
	private void handleRemoveRequest(String file, SocketChannel client)
	{
		database.get(file).setState(State.REMOVE_IN_PROGRESS);
		int[] dstorePorts = database.get(file).getDstorePorts();
		
		for (int port : dstorePorts)
		{
			try
			{
				SocketChannel dstore = SocketChannel.open(new InetSocketAddress(port));
				
				MessageSocket.sendMessage(OpCodes.DSTORE_REMOVE_REQUEST, file, dstore);
				
				if (MessageSocket.getOpcode(MessageSocket.receiveMessage(dstore)) != OpCodes.REMOVE_ACK)
					throw new IOException("Sadge");
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		//TODO: change the way that a file si tested to be there
		database.get(file).setState(State.REMOVE_COMPLETE);
		
		try { MessageSocket.sendMessage(OpCodes.REMOVE_COMPLETE, "", client); }
		catch (IOException e) { e.printStackTrace(); }
	}
}