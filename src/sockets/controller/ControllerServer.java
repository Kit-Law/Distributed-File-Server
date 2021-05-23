package sockets.controller;

import database.Database;
import logger.ControllerLogger;
import logger.Logger;
import sockets.Server;
import database.MetaData;
import helpers.MutableInt;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerServer extends Server
{
	private static int R;
	public static int timeout;
	
	private static Database database = new Database();
	private static ArrayList<Controller> instances = new ArrayList<>();
	private static ConcurrentHashMap<Integer, Socket> dstores = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Integer> loads = new ConcurrentHashMap<>();
	
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
		DataInputStream in = new DataInputStream(client.getInputStream());
		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		
		Controller instance = new Controller(client, in, out);
		instances.add(instance);
		
		new Thread(instance).start();
	}
	
	public static synchronized void addLoad(Socket client, Integer port) { loads.put(client.getPort(), port); }
	public static synchronized Integer reLoad(Socket client, String filename) throws IOException, FileNotFoundException
	{
		if (!loads.containsValue(client.getPort()))
			throw new IOException("ERROR_LOAD");
		
		return database.selectDStore(filename, 1);
	}
	
	public static synchronized void addDStore(int port, Socket dstore) { dstores.put(port, dstore); }
	
	public static synchronized void dstoreFailed(Integer port)
	{
		database.dstoreFailed(port);
		dstores.remove(port);
		loads.remove(port);
	}
	
	public static synchronized int getDStorePort(Socket dstore) throws IOException
	{
		for (Map.Entry<Integer, Socket> buffer : dstores.entrySet())
			if (buffer.getValue() == dstore) return buffer.getKey();
			
		throw new IOException("Socket not present");
	}
	public static synchronized Collection<Socket> getDStores()
	{
		return dstores.values();
	}
	public static synchronized Socket[] getDStores(String filename)
	{
		return database.getMetaData(filename).getDstorePorts().stream()
				.map((port) -> dstores.get(port)).toArray(Socket[]::new);
	}
	
	public static synchronized Integer[] getRdstores()	//TODO: Fix this
	{
		HashMap<Integer, MutableInt> dstoreSize = new HashMap<>();
		
		for (Map.Entry<Integer, Socket> dstore : dstores.entrySet())
			dstoreSize.put(dstore.getKey(), new MutableInt(0));
		
		for (Map.Entry<String, MetaData> file : database.getEntrySet())
			file.getValue().getDstorePorts().forEach(port -> dstoreSize.get(port).increment());
		
		ArrayList<Map.Entry<Integer, MutableInt>> sortedDStores = new ArrayList<>(dstoreSize.entrySet());
		sortedDStores.sort(Map.Entry.comparingByValue());
		
		return sortedDStores.subList(0, R).stream().map(Map.Entry::getKey).toArray(Integer[]::new);
	}
	
	public static synchronized boolean hasEnoughDstores() { return dstores.size() >= R; }
	
	public static synchronized boolean isDstore(Socket client) { return dstores.containsValue(client); }
	
	public static int getR() { return R; }
	public static int getTimeout() { return timeout; }
	public static synchronized Database getDatabase() { return database; }
}
