package Sockets;

import database.MetaData;
import database.State;
import mains.Controller;
import mains.MutableInt;

import java.io.*;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControllerServer extends Server
{
	private static int R;
	private static int rebalance_period;
	private static HashMap<String, MetaData> database = new HashMap<>();
	private static HashMap<String, MutableInt> storeAcks = new HashMap<>();
	private static ArrayList<Map.Entry<Integer, Socket>> dstores = new ArrayList<>();
	
	public ControllerServer(final int cport, final int R, final int timeout, final int rebalance_period)
	{
		super(cport, timeout);
		ControllerServer.R = R;
		ControllerServer.rebalance_period = rebalance_period;
		
		//logger.Logger.setLogFile(this);
		
		launchServer();
	}
	
	@Override
	protected void handleConnection(Socket client) throws IOException
	{
		// obtaining input and out streams
		DataInputStream in = new DataInputStream(client.getInputStream());
		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		
		// create a new thread object
		new Thread(new Controller(client, in, out)).start();
	}
	
	public static void freeFile(String filename) throws IOException
	{
		if (database.containsKey(filename))
			if (database.get(filename).getState() == State.REMOVE_COMPLETE)
				database.remove(filename);
			else
				database.remove(filename); //throw new IOException("File already existed lol"); //TODO: Add this back when doing error handelling
	}
	
	public static void addDStore(int port, Socket dstore) { dstores.add(new AbstractMap.SimpleEntry<>(port, dstore)); }
	
	public static void newDatabaseEntry(String filename, MetaData metaData) { database.put(filename, metaData); }
	public static void addNewDatabasePort(String filename) { MutableInt.incrementCount(storeAcks, filename); }
	public static boolean isReplicatedRTimes(String filename) { return storeAcks.get(filename).get() == R; }
	public static void setFileState(String filename, State state) { database.get(filename).setState(state); }
	
	public static int getDStorePort(Socket dstore) throws IOException
	{
		for (Map.Entry<Integer, Socket> buffer : dstores)
			if (buffer.getValue() == dstore) return buffer.getKey();
			
		throw new IOException("Socket not present");
	}
	public static Socket[] getDStores()
	{
		return (Socket[]) dstores.stream().map(Map.Entry::getValue).toArray();
	}
	public static Socket[] getDStores(String filename)
	{
		return (Socket[]) Arrays.stream(database.get(filename).getDstorePorts())
				.map((port) -> dstores.get(port)).toArray();
	}
	
	public static int selectDStore(String filename) { return database.get(filename).getDstorePorts()[0]; }
	
	//TODO: Remove this after testing.
	public static void setR(int r) { R = r; }
	
	public static int getR() { return R; }
	public static int getRebalance_period() { return rebalance_period; }
	public static String getFileList() { return database.keySet().stream().map(Object::toString).collect(Collectors.joining(" ")); }
	public static long getFileSize(String filename) { return database.get(filename).getSize(); }
	
	public static Stream<Integer> getRdstores() { Collections.shuffle(dstores); return dstores.subList(0, R).stream().map(Map.Entry::getKey); }
}
