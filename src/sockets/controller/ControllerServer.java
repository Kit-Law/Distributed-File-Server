package sockets.controller;

import logger.ControllerLogger;
import logger.Logger;
import sockets.Server;
import database.MetaData;
import database.State;
import helpers.MutableInt;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerServer extends Server
{
	private static int R;
	public static int timeout;
	
	private static ArrayList<Thread> threads = new ArrayList<>();
	
	private static ConcurrentHashMap<String, MetaData> database = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, MutableInt> storeAcks = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, MutableInt> removeAcks = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Socket> dstores = new ConcurrentHashMap<>();
	
	public ControllerServer(final int cport, final int R, final int timeout, final int rebalance_period)
	{
		super(cport, timeout);
		ControllerServer.R = R;
		ControllerServer.timeout = timeout;
		
		new Thread(new RebalanceController(rebalance_period)).start();
		
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
		Thread controller = new Thread(new Controller(client, in, out));
		controller.start();
		
		threads.add(controller);
	}
	
	public static void freeFile(String filename) throws FileAlreadyExistsException
	{
		if (database.containsKey(filename))
			if (database.get(filename).getState() == State.REMOVE_COMPLETE)
				database.remove(filename);
			else
				throw new FileAlreadyExistsException(filename);
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
	
	public static void addNewStoreAck(String filename)
	{
		MutableInt.incrementCount(storeAcks, filename);
	}
	public static void addNewRemoveAck(String filename)
	{
		MutableInt.incrementCount(removeAcks, filename);
	}
	public static boolean isReplicatedRTimes(String filename)
	{
		return storeAcks.containsKey(filename) &&
				storeAcks.get(filename).get() == R;
	}
	public static boolean isRemoved(String filename)
	{
		return removeAcks.containsKey(filename) &&
				removeAcks.get(filename).get() == database.get(filename).getDstorePorts().size();
	}
	public static void setFileState(String filename, State state) throws FileNotFoundException
	{
		if (!database.containsKey(filename))
			throw new FileNotFoundException(filename);
		
		database.get(filename).setState(state);
	}
	
	public static int getDStorePort(Socket dstore) throws IOException
	{
		for (Map.Entry<Integer, Socket> buffer : dstores.entrySet())
			if (buffer.getValue() == dstore) return buffer.getKey();
			
		throw new IOException("Socket not present");
	}
	public static Collection<Socket> getDStores()
	{
		return dstores.values();
	}
	public static Socket[] getDStores(String filename)
	{
		return database.get(filename).getDstorePorts().stream()
				.map((port) -> dstores.get(port)).toArray(Socket[]::new);
	}
	
	public static int selectDStore(String filename) throws FileNotFoundException
	{
		if (!database.containsKey(filename))
			throw new FileNotFoundException(filename);
		
		return database.get(filename).getDstorePorts().get(0);
	}
	
	public static int getR() { return R; }
	public static String getFileList()
	{
		ArrayList<String> fileList = new ArrayList<>();
		
		for (Map.Entry<String, MetaData> file : database.entrySet())
			if (file.getValue().getState() == State.STORE_COMPLETE) fileList.add(file.getKey());
		
		return String.join(" ",  fileList);
	}
	public static long getFileSize(String filename) { return database.get(filename).getSize(); }
	
	public static Integer[] getRdstores()
	{
		HashMap<Integer, MutableInt> dstoreSize = new HashMap<>();
		
		for (Map.Entry<Integer, Socket> dstore : dstores.entrySet())
			dstoreSize.put(dstore.getKey(), new MutableInt(0));
		
		for (Map.Entry<String, MetaData> file : database.entrySet())
			file.getValue().getDstorePorts().forEach(port -> dstoreSize.get(port).increment());
		
		ArrayList<Map.Entry<Integer, MutableInt>> sortedDStores = new ArrayList<>(dstoreSize.entrySet());
		sortedDStores.sort(Map.Entry.comparingByValue());
		
		return sortedDStores.subList(0, R).stream().map(Map.Entry::getKey).toArray(Integer[]::new);
	}
	
	public static boolean hasEnoughDstores() { return dstores.size() >= R; }
	
	public static void pause() { threads.forEach((thread ->
	{
		try { thread.wait(); }
		catch (InterruptedException e) { e.printStackTrace(); } }));
	}
	public static void resume() { threads.forEach(Thread::notify); }
	
	public static void clearStoreACKS(String file) { storeAcks.remove(file); }
	public static void clearRemoveACKS(String file) { removeAcks.remove(file); }
}
