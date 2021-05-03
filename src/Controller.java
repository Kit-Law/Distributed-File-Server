//import Constants.OpCodes;
import Constants.Protocol;
import Sockets.MessageSocket;
import Sockets.Server;
import database.MetaData;
import database.State;
//import logger.Loggable;
import logger.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

public class Controller extends Server //implements Loggable
{
	private final int R;
	private final int rebalance_period;
	private static HashMap<String, MetaData> database = new HashMap<>();
	private static ArrayList<Map.Entry<SocketChannel, Integer>> dstores = new ArrayList<>();
	//TODO: make this better
	private static HashMap<String, SocketChannel> stores = new HashMap<>();
	
	//java Controller cport R timeout
	public static void main(String[] args)
	{
		new Controller(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
	
	private Controller(final int cport, final int R, final int timeout, final int rebalance_period)
	{
		super(cport, timeout);
		this.R = R;
		this.rebalance_period = rebalance_period;
		
		//logger.Logger.setLogFile(this);
		
		launchServer();
	}
	
	@Override
	protected void handleRead(SelectionKey key) throws IOException
	{
		//Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		SocketChannel client = (SocketChannel) key.channel();
		
		String msg = MessageSocket.receiveMessage(client);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.JOIN_TOKEN:
				handleDstoreConnect(MessageSocket.getOperand(msg), client);
				break;
			case Protocol.STORE_TOKEN:
				handleStoreRequest(MessageSocket.getOperand(msg), client);
				break;
			case Protocol.STORE_ACK_TOKEN:
				handleStoreAck(MessageSocket.getOperand(msg));
				break;
			case Protocol.LOAD_TOKEN:
				handleLoadRequest(MessageSocket.getOperand(msg), client);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(MessageSocket.getOperand(msg), client);
				break;
		}
	}
	
	@Override
	protected void handleWrite(SelectionKey key) throws IOException
	{ }
	
	public static void saveDatabase()
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream("Database.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(database);
			out.close();
			fileOut.close();
			//Logger.info.log("Database saved.");
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
			//Logger.info.log("Database loaded.");
		}
		catch (Exception e) { System.err.println(e.getMessage() + e.getCause()); }
	}
	
	@Override
	public final String toString()
	{
		return "Controller-" + port + "-" + System.currentTimeMillis();
	}
	
	private void handleDstoreConnect(String port, SocketChannel dstore)
	{
		dstores.add(new AbstractMap.SimpleEntry<SocketChannel, Integer>(dstore, Integer.parseInt(port)));
		
		//Logger.info.log("DStore: " + dstore + ", has been added.");
	}
	
	private void handleStoreRequest(final String file, SocketChannel client) throws IOException
	{
		if (database.containsKey(file))
			if (database.get(file).getState() == State.REMOVE_COMPLETE)
				database.remove(file);
			else
				throw new IOException("File already existed lol");
		
		//Create a new database entry.
		database.put(file, new MetaData(State.STORE_IN_PROGRESS));
		
		//Get R of the dstores.
		List<Map.Entry<SocketChannel, Integer>> dstores = getRdstores();
		String dstorePorts = getPorts(dstores);
		
		//Set the dsotres ready to read.
		dstores.forEach(dstore -> {
			try { dstore.getKey().register(selector, SelectionKey.OP_READ); }
			catch (ClosedChannelException e) { e.printStackTrace(); }
		});
		
		//Send the ports to the client.
		MessageSocket.sendMessage(Protocol.STORE_TO_TOKEN, dstorePorts, client);
		
		stores.put(file, client);
	}
	
	private List<Map.Entry<SocketChannel, Integer>> getRdstores()
	{
		Collections.shuffle(dstores);
		return dstores.subList(0, R);
	}
	
	private String getPorts(List<Map.Entry<SocketChannel, Integer>> dstores)
	{
		StringBuilder ports = new StringBuilder();
		dstores.forEach(dstore -> ports.append(dstore.getValue()).append(" "));
		return ports.toString();
	}
	
	//TODO: fix this lol
	private void handleStoreAck(String operand) throws IOException
	{
		String[] args = operand.split(" ");
		
		database.get(args[0]).addDStorePort(Integer.parseInt(args[1]));
		
		if (database.get(args[0]).getDstorePorts().size() == R)
		{
			SocketChannel c = stores.get(args[0]);
			
			database.get(args[0]).setSize(Long.parseLong(args[2]));
			MessageSocket.sendMessage(Protocol.STORE_COMPLETE_TOKEN, "", c);
			
			stores.remove(args[0]);
		}
	}
	
	private void handleLoadRequest(final String file, SocketChannel client)
	{
		try
		{
			int port = selectDstore(file);
			MessageSocket.sendMessage(Protocol.LOAD_FROM_TOKEN, port + " " + database.get(file).getSize(), client);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private int selectDstore(String file) { return database.get(file).getDstorePorts().get(0); }
	
	private void handleRemoveRequest(final String file, SocketChannel client)
	{
		database.get(file).setState(State.REMOVE_IN_PROGRESS);
		List<Integer> dstorePorts = database.get(file).getDstorePorts();
		
		for (int port : dstorePorts)
		{
			try
			{
				SocketChannel dstore = SocketChannel.open(new InetSocketAddress(port));
				
				MessageSocket.sendMessage(Protocol.REMOVE_TOKEN, file, dstore);
				
				if (MessageSocket.getOpcode(MessageSocket.receiveMessage(dstore)) != Protocol.REMOVE_ACK_TOKEN)
					throw new IOException("Sadge");
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		//TODO: change the way that a file si tested to be there
		database.get(file).setState(State.REMOVE_COMPLETE);
		
		try { MessageSocket.sendMessage(Protocol.REMOVE_COMPLETE_TOKEN, "", client); }
		catch (IOException e) { e.printStackTrace(); }
	}
}