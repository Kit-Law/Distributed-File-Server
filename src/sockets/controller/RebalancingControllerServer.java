package sockets.controller;

import constants.Protocol;
import logger.ControllerLogger;
import sockets.message.MessageSocket;
import helpers.MutableInt;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class RebalancingControllerServer
{
	public static void handleRebalance(Socket controller) throws IOException
	{
		ArrayList<Map.Entry<Socket, String[]>> dstoreFiles = new ArrayList<>();
		HashMap<String, MutableInt> fileCounts = new HashMap<>();
		
		getFileData(dstoreFiles, fileCounts, controller);
		
		int maxFiles = (int) Math.ceil(ControllerServer.getR() * fileCounts.size() / (double) dstoreFiles.size());
		
		ArrayList<Map.Entry<String, MutableInt>> filesToAlter = calFilesToAlter(fileCounts);
		
		HashMap<Socket, ArrayList<String>> toRemove = calFilesToRemove(dstoreFiles, filesToAlter);
		HashMap<String, ArrayList<Integer>> toStore = calFilesToStore(dstoreFiles, filesToAlter, toRemove, maxFiles);
		
		messageDStores(dstoreFiles, toRemove, toStore, controller);
	
		updateDatabase(dstoreFiles, toRemove, toStore);
	}
	
	private static void getFileData(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles, HashMap<String, MutableInt> fileCounts, Socket controller) throws IOException
	{
		for (Socket dstore : ControllerServer.getDStores())
		{
			MessageSocket.sendMessage(Protocol.LIST_TOKEN, "", dstore, ControllerLogger.getInstance(), controller);
			
			String msg = MessageSocket.receiveMessage(dstore, ControllerLogger.getInstance(), controller);
			
			if (!MessageSocket.getOpcode(msg).equals(Protocol.LIST_TOKEN))
				throw new IOException("Wrong Opcode received.");
			
			dstoreFiles.add(Map.entry(dstore, MessageSocket.getOperand(msg)));
			
			for (String file : MessageSocket.getOperand(msg))
				MutableInt.incrementCount(fileCounts, file);
		}
	}
	
	private static ArrayList<Map.Entry<String, MutableInt>> calFilesToAlter(HashMap<String, MutableInt> fileCounts)
	{
		ArrayList<Map.Entry<String, MutableInt>> filesToAlter = new ArrayList<>();
		
		for (Map.Entry<String, MutableInt> file : fileCounts.entrySet())
			if (file.getValue().get() != ControllerServer.getR())
				filesToAlter.add(Map.entry(file.getKey(), new MutableInt(ControllerServer.getR() - file.getValue().get())));
		
		return filesToAlter;
	}
	
	public static HashMap<Socket, ArrayList<String>> calFilesToRemove(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
																	  ArrayList<Map.Entry<String, MutableInt>> filesToAlter)
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
			
			for (String file : buffer)
			{
				if (toStore.containsKey(file))
					toStore.get(file).add(ControllerServer.getDStorePort(dstoreFile.getKey()));
				else toStore.put(file, new ArrayList<>(ControllerServer.getDStorePort(dstoreFile.getKey())));
			}
		}
		
		return toStore;
	}
	
	private static void messageDStores(ArrayList<Map.Entry<Socket, String[]>> dstoreFiles,
									   HashMap<Socket, ArrayList<String>> toRemove,
									   HashMap<String, ArrayList<Integer>> toStore,
									   Socket controller) throws IOException
	{
		for (Map.Entry<Socket, String[]> dstore : dstoreFiles)
		{
			StringBuilder storeMsg = new StringBuilder();
			int storeCount = 1;
			
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
			if (toRemove.containsKey(dstore.getKey()))
				removeMsg.append(toRemove.get(dstore.getKey()).size()).append(' ').append(String.join(" ", toRemove.get(dstore.getKey())));
			
			if (storeMsg.length() == 0 && removeMsg.length() == 0)
				continue;
			
			MessageSocket.sendMessage(Protocol.REBALANCE_TOKEN, storeCount + " " + storeMsg + " " + removeMsg, dstore.getKey(), ControllerLogger.getInstance(), controller);
			
			if (!MessageSocket.receiveMessage(dstore.getKey(), ControllerLogger.getInstance(), controller).equals(Protocol.REBALANCE_COMPLETE_TOKEN))
				throw new IOException("Sadge");
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