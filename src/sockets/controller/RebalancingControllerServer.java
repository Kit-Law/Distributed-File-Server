package sockets.controller;

import constants.Protocol;
import logger.ControllerLogger;
import sockets.message.MessageSocket;
import helpers.MutableInt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RebalancingControllerServer
{
	public static boolean rebalanceComplete;
	
	public static void handleRebalance() throws IOException
	{
		rebalanceComplete = false;
		ArrayList<Map.Entry<Socket, String[]>> dstoreFiles = new ArrayList<>();
		ConcurrentHashMap<String, MutableInt> fileCounts = new ConcurrentHashMap<>();

		getFileData(dstoreFiles, fileCounts);
		
		int maxFiles = (int) Math.ceil(ControllerServer.getR() * fileCounts.size() / (double) dstoreFiles.size());
		int minFiles = (int) Math.floor(ControllerServer.getR() * fileCounts.size() / (double) dstoreFiles.size());
		
		ArrayList<Map.Entry<String, MutableInt>> filesToAlter = calFilesToAlter(fileCounts);
		ArrayList<String> filesToShuffle = calFilesToShuffle(fileCounts);
		
		HashMap<Socket, ArrayList<String>> toRemove = calFilesToRemove(dstoreFiles, filesToAlter, minFiles);
		HashMap<String, ArrayList<Integer>> toStore = calFilesToStore(dstoreFiles, filesToAlter, filesToShuffle, toRemove, maxFiles);
		
		messageDStores(dstoreFiles, toRemove, toStore);
	
		updateDatabase(dstoreFiles, toRemove, toStore);
	}
	
	private static void getFileData(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles, ConcurrentHashMap<String, MutableInt> fileCounts) throws IOException
	{
		for (Socket dstore : ControllerServer.getDStores())
		{
			MessageSocket.sendMessage(Protocol.LIST_TOKEN, "", dstore, ControllerLogger.getInstance(), dstore);
			
			String msg = MessageSocket.receiveMessage(dstore, ControllerLogger.getInstance(), dstore);
			
			if (!MessageSocket.getOpcode(msg).equals(Protocol.LIST_TOKEN))
				throw new IOException("Wrong Opcode received.");
			
			if (MessageSocket.getOperand(msg)[0].equals(""))
			{
				dstoreFiles.add(Map.entry(dstore, new String[0]));
				continue;
			}
			
			dstoreFiles.add(Map.entry(dstore, MessageSocket.getOperand(msg)));
			
			for (String file : MessageSocket.getOperand(msg))
			{
				MutableInt.incrementCount(fileCounts, file);
			}
		}
	}
	
	private static ArrayList<Map.Entry<String, MutableInt>> calFilesToAlter(ConcurrentHashMap<String, MutableInt> fileCounts)
	{
		ArrayList<Map.Entry<String, MutableInt>> filesToAlter = new ArrayList<>();
		
		for (Map.Entry<String, MutableInt> file : fileCounts.entrySet())
			if (file.getValue().get() != ControllerServer.getR())
				filesToAlter.add(Map.entry(file.getKey(), new MutableInt(ControllerServer.getR() - file.getValue().get())));
		
		return filesToAlter;
	}
	
	private static ArrayList<String> calFilesToShuffle(ConcurrentHashMap<String, MutableInt> fileCounts)
	{
		ArrayList<String> filesToShuffle = new ArrayList<>();
		
		for (Map.Entry<String, MutableInt> file : fileCounts.entrySet())
			if (file.getValue().get() == ControllerServer.getR())
				filesToShuffle.add(file.getKey());
		
		return filesToShuffle;
	}
	
	public static HashMap<Socket, ArrayList<String>> calFilesToRemove(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
																	  ArrayList<Map.Entry<String, MutableInt>> filesToAlter,
																	  int minFiles)
	{
		HashMap<Socket, ArrayList<String>> toRemove = new HashMap<>();
		
		dstoreFiles.sort(Comparator.comparingInt(e -> e.getValue().length));
		filesToAlter.sort(Comparator.comparingInt(e -> e.getValue().get()));
		
		for (int i = dstoreFiles.size() - 1; i >= 0; i--)
		{
			ArrayList<String> buffer = new ArrayList<>();
			int times = Math.min(filesToAlter.size(), dstoreFiles.get(i).getValue().length);
			
			for (int j = 0; j < times; j++)
			{
				if ((dstoreFiles.get(i).getValue().length - buffer.size()) <= minFiles)
					break;
				
				if (filesToAlter.get(j).getValue().get() < 0 &&
						Arrays.asList(dstoreFiles.get(i).getValue()).contains(filesToAlter.get(j).getKey()))
				{
					buffer.add(filesToAlter.get(j).getKey());
					filesToAlter.get(j).getValue().increment();
				}
				else if (times < filesToAlter.size()) times++;
			}
			
			if (buffer.size() > 0)
				toRemove.put(dstoreFiles.get(i).getKey(), (ArrayList<String>) buffer.clone());	//TODO: test if this can be removed
		}
		
		return toRemove;
	}
	
	public static HashMap<String, ArrayList<Integer>> calFilesToStore(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
																	  ArrayList<Map.Entry<String, MutableInt>> filesToAlter,
																	  ArrayList<String> filesToShuffle,
																	  HashMap<Socket, ArrayList<String>> toRemove,
																	  int max) throws IOException
	{
		HashMap<String, ArrayList<Integer>> toStore = new HashMap<>();
		
		dstoreFiles.sort(Comparator.comparingInt(e -> e.getValue().length -
				(toRemove.containsKey(e.getKey()) ? toRemove.get(e.getKey()).size() : 0)));
		
		for (Map.Entry<Socket, String[]> dstoreFile : dstoreFiles)
		{
			ArrayList<String> buffer = new ArrayList<>();
			int times = max - dstoreFile.getValue().length + (toRemove.containsKey(dstoreFile.getKey()) ? toRemove.get(dstoreFile.getKey()).size() : 0);
			if (times > filesToAlter.size()) times = filesToAlter.size();
			
			for (int j = 0; j < times; j++)
			{
				if (filesToAlter.get(j).getValue().get() > 0 && !Arrays.asList(dstoreFile.getValue()).contains(filesToAlter.get(j).getKey()))
				{
					buffer.add(filesToAlter.get(j).getKey());
					filesToAlter.get(j).getValue().decrement();
				}
				else if (times < filesToAlter.size()) times++;
			}
			
			int remaining = buffer.size() + dstoreFile.getValue().length;
			for (int j = remaining; j < max; j++)
			{
				String file = filesToShuffle.get(0);
				
				for (Map.Entry<Socket, String[]> dfile : dstoreFiles)
				{
					if (dfile == dstoreFile)
						continue;
					
					boolean b = false;
					for (String f : dfile.getValue())
					{
						if (f.equals(file))
						{
							if (!toRemove.containsKey(dfile.getKey()))
								toRemove.put(dfile.getKey(), new ArrayList<String>());
							
							toRemove.get(dfile.getKey()).add(f);
							buffer.add(f);
							filesToShuffle.remove(0);
							file = filesToShuffle.get(0);
							j++;
							
							if (j == max) b = true;
							
							break;
						}
					}
					if (b) break;
				}
			}
			
			for (String file : buffer)
			{
				if (toStore.containsKey(file))
					toStore.get(file).add(ControllerServer.getDStorePort(dstoreFile.getKey()));
				else
				{
					ArrayList<Integer> ports = new ArrayList<>();
					ports.add(ControllerServer.getDStorePort(dstoreFile.getKey()));
					toStore.put(file, ports);
				}
			}
		}
		
		return toStore;
	}
	
	private static void messageDStores(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
									   HashMap<Socket, ArrayList<String>> toRemove,
									   HashMap<String, ArrayList<Integer>> toStore) throws IOException
	{
		for (Map.Entry<Socket, String[]> dstore : dstoreFiles)
		{
			StringBuilder storeMsg = new StringBuilder();
			int storeCount = 0;
			
			for (String file : dstore.getValue())
			{
				if (toStore.containsKey(file))
				{
					storeMsg.append(file).append(' ').append(toStore.get(file).size()).append(' ').append(
							toStore.get(file).stream().map(Object::toString).collect(Collectors.joining(" "))).append(' ');
					storeCount++;
					
					toStore.remove(file);
				}
			}
			
			StringBuilder removeMsg = new StringBuilder();
			int removeCount = 0;
			if (toRemove.containsKey(dstore.getKey()))
			{
				removeMsg.append(String.join(" ", toRemove.get(dstore.getKey())));
				removeCount++;
			}
			
			if (storeCount == 0 && removeCount == 0)
				continue;
			
			MessageSocket.sendMessage(Protocol.REBALANCE_TOKEN, storeCount + " " + storeMsg + removeCount + " " + removeMsg, dstore.getKey(), ControllerLogger.getInstance(), dstore.getKey());
			
			while (!rebalanceComplete)
				try { Thread.sleep(10); }
				catch (Exception e) { e.printStackTrace(); }
			
			rebalanceComplete = false;
			//if (!MessageSocket.receiveMessage(dstore.getKey(), ControllerLogger.getInstance(), dstore.getKey()).equals(Protocol.REBALANCE_COMPLETE_TOKEN + " "))
			//	throw new IOException("Sadge");
		}
	}
	
	private static void updateDatabase(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
									   HashMap<Socket, ArrayList<String>> toRemove,
									   HashMap<String, ArrayList<Integer>> toStore) throws IOException
	{
		for (Map.Entry<Socket, String[]> dstore : dstoreFiles)
		{
			int port = ControllerServer.getDStorePort(dstore.getKey());
			
			for (String file : dstore.getValue())
				ControllerServer.validateDatabasePort(file, port);
		}
		
		for (Map.Entry<String, ArrayList<Integer>> entry : toStore.entrySet())
			ControllerServer.addDatabasePorts(entry.getKey(), entry.getValue());
		
		for (Map.Entry<Socket, ArrayList<String>> entry : toRemove.entrySet())
		{
			int port = ControllerServer.getDStorePort(entry.getKey());
			
			for (String file : entry.getValue())
				ControllerServer.removeDatabasePort(file, port);
		}
	}
}
