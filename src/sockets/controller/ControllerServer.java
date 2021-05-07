package sockets.controller;

import logger.ControllerLogger;
import logger.Logger;
import sockets.Server;
import database.MetaData;
import database.State;
import helpers.MutableInt;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class ControllerServer extends Server
{
	private static int R;
	private static int rebalance_period;
	private static HashMap<String, MetaData> database = new HashMap<>();
	private static HashMap<String, MutableInt> storeAcks = new HashMap<>();
	private static HashMap<String, MutableInt> removeAcks = new HashMap<>();
	private static HashMap<Integer, Socket> dstores = new HashMap<>();
	
	public ControllerServer(final int cport, final int R, final int timeout, final int rebalance_period)
	{
		super(cport, timeout);
		ControllerServer.R = R;
		ControllerServer.rebalance_period = rebalance_period;
		
		try { ControllerLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL); }
		catch (IOException e) { e.printStackTrace(); }
		
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
				throw new IOException("File already existed lol"); //TODO: Add this back when doing error handelling
	}
	
	public static void addDStore(int port, Socket dstore) { dstores.put(port, dstore); }
	
	public static void newDatabaseEntry(String filename, MetaData metaData) { database.put(filename, metaData); }
	public static void addDatabasePorts(String filename, ArrayList<Integer> ports) { database.get(filename).addPorts(ports); }
	public static void removeDatabasePort(String filename, int port) { database.get(filename).removePort(port); }
	public static void validateDatabasePort(String filename, int port)
	{
		if (database.containsKey(filename)) database.get(filename).validatePort(port);
		else database.put(filename, new MetaData(State.STORE_COMPLETE, 0, new Integer[]{port})); //TODO: GET SIZE?????
	}
	
	public static void addNewStoreAck(String filename) { MutableInt.incrementCount(storeAcks, filename); }
	public static void addNewRemoveAck(String filename) { MutableInt.incrementCount(removeAcks, filename); }
	public static boolean isReplicatedRTimes(String filename) { return storeAcks.containsKey(filename) && storeAcks.get(filename).get() == R; }
	public static boolean isRemoved(String filename) { return removeAcks.containsKey(filename) && removeAcks.get(filename).get() == database.get(filename).getDstorePorts().size(); }
	public static void setFileState(String filename, State state) { database.get(filename).setState(state); }
	
	public static int getDStorePort(Socket dstore) throws IOException
	{
		for (Map.Entry<Integer, Socket> buffer : dstores.entrySet())
			if (buffer.getValue() == dstore) return buffer.getKey();
			
		throw new IOException("Socket not present");
	}
	public static Socket[] getDStores()
	{
		return dstores.values().toArray(new Socket[0]);
	}
	public static Socket[] getDStores(String filename)
	{
		return database.get(filename).getDstorePorts().stream()
				.map((port) -> dstores.get(port)).toArray(Socket[]::new);
	}
	
	public static int selectDStore(String filename) { return database.get(filename).getDstorePorts().get(0); }
	
	public static int getR() { return R; }
	public static int getRebalance_period() { return rebalance_period; }
	public static String getFileList()
	{
		StringBuilder fileList = new StringBuilder();
		
		for (Map.Entry<String, MetaData> file : database.entrySet())
			if (file.getValue().getState() == State.STORE_COMPLETE) fileList.append(file.getKey()).append(' ');
		
		return fileList.toString();
	}
	public static long getFileSize(String filename) { return database.get(filename).getSize(); }
	
	public static Integer[] getRdstores() { return new ArrayList<>(dstores.keySet()).subList(0, R).toArray(Integer[]::new); }
	//public static Integer[] getRdstores() { Collections.shuffle(new ArrayList<>(dstores.keySet())); return dstores.subList(0, R).stream().map(Map.Entry::getKey).toArray(Integer[]::new); }
}
