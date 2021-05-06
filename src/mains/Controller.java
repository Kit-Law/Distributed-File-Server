package mains;

import Constants.Protocol;
import Sockets.ControllerServer;
import Sockets.MessageClient;
import Sockets.MessageSocket;
import database.MetaData;
import database.State;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller extends MessageClient implements Runnable
{
	private Socket client;
	
	//java Controller cport R timeout
	public static void main(String[] args)
	{
		new ControllerServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
	
	public Controller(Socket client, DataInputStream in, DataOutputStream out)
	{
		super(in, out);
		this.client = client;
	}
	
	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				handleMessage();
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	protected void handleMessage() throws IOException
	{
		//Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		
		String msg = receiveMessage();
		System.out.println(msg);			//TODO: remove this
		String[] operand = MessageSocket.getOperand(msg);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.JOIN_TOKEN:
				handleDstoreConnect(operand[0]);
				break;
			case Protocol.LIST_TOKEN:
				handleListRequest();
				break;
			case Protocol.STORE_TOKEN:
				handleStoreRequest(operand[0], Long.parseLong(operand[1]));
				break;
			case Protocol.STORE_ACK_TOKEN:
				handleStoreAck(operand[0]);
				break;
			case Protocol.LOAD_TOKEN:
				handleLoadRequest(operand[0]);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(operand[0]);
				break;
		}
	}
	
	private void handleDstoreConnect(String port) throws IOException
	{
		ControllerServer.addDStore(Integer.parseInt(port), client);
		
		handleRebalance();
		//Logger.info.log("DStore: " + dstore + ", has been added.");
	}
	
	private void handleRebalance() throws IOException
	{
		ArrayList<Map.Entry<Socket, String[]>> dstoreFiles = new ArrayList<>();
		HashMap<String, MutableInt> fileCounts = new HashMap<>();
		
		for (Socket dstore : ControllerServer.getDStores())
		{
			sendMessage(Protocol.LIST_TOKEN, "", dstore);
			
			String msg = receiveMessage(dstore);
			
			if (!getOpcode(msg).equals(Protocol.LIST_TOKEN))
				throw new IOException("Wrong Opcode received.");
			
			dstoreFiles.add(Map.entry(dstore, getOperand(msg)));
			
			for (String file : getOperand(msg))
				MutableInt.incrementCount(fileCounts, file);
		}
		
		int max = (int) Math.ceil(ControllerServer.getR() * fileCounts.size() / (double) dstoreFiles.size());
		
		ArrayList<Map.Entry<String, MutableInt>> filesToAlter = new ArrayList<>();
		
		for (Map.Entry<String, MutableInt> file : fileCounts.entrySet())
			if (file.getValue().get() != ControllerServer.getR())
				filesToAlter.add(Map.entry(file.getKey(), new MutableInt(ControllerServer.getR() - file.getValue().get())));
		
		HashMap<Socket, ArrayList<String>> toRemove = new HashMap<>();
		HashMap<String, ArrayList<Integer>> toStore = new HashMap<>();
		
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
				toRemove.put(dstoreFiles.get(i).getKey(), (ArrayList<String>) buffer.clone());
		}
		
		dstoreFiles.sort(Comparator.comparingInt(e -> e.getValue().length -
				(toRemove.containsKey(e.getKey()) ? toRemove.get(e.getKey()).size() : 0)));
		
		for (int i = 0; i < dstoreFiles.size(); i++)
		{
			ArrayList<String> buffer = new ArrayList<>();
			int times = max - dstoreFiles.get(i).getValue().length +
					(toRemove.containsKey(dstoreFiles.get(i).getKey()) ? toRemove.get(dstoreFiles.get(i).getKey()).size() : 0);
			if (times > filesToAlter.size()) times = filesToAlter.size();
			
			for (int j = 0; j < times; j++)
			{
				if (filesToAlter.get(j).getValue().get() > 0 &&
						!Arrays.asList(dstoreFiles.get(i).getValue()).contains(filesToAlter.get(j).getKey()))
				{
					buffer.add(filesToAlter.get(j).getKey());
					filesToAlter.get(j).getValue().decrement();
				}
				else if (times < filesToAlter.size()) times++;
			}
			
			for (String file : buffer)
			{
				if (toStore.containsKey(file))
					toStore.get(file).add(ControllerServer.getDStorePort(dstoreFiles.get(i).getKey()));
				else
					toStore.put(file, new ArrayList<>(ControllerServer.getDStorePort(dstoreFiles.get(i).getKey())));
			}
		}
		
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
			
			sendMessage(Protocol.REBALANCE_TOKEN, storeCount + " " + storeMsg + " " + removeMsg, dstore.getKey());
			
			if (!receiveMessage(dstore.getKey()).equals(Protocol.REBALANCE_COMPLETE_TOKEN))
				throw new IOException("Sadge");
		}
	}
	
	private void handleListRequest() throws IOException
	{
		sendMessage(Protocol.LIST_TOKEN, ControllerServer.getFileList());
	}
	
	private void handleStoreRequest(final String file, final long filesize) throws IOException
	{
		ControllerServer.freeFile(file);
		Integer[] dstorePorts = ControllerServer.getRdstores();
		
		//Creates a new database entry
		ControllerServer.newDatabaseEntry(file, new MetaData(State.STORE_IN_PROGRESS, filesize, dstorePorts));
		
		//Send the ports to the client.
		sendMessage(Protocol.STORE_TO_TOKEN, Stream.of(dstorePorts).map(Object::toString).collect(Collectors.joining(" ")));
		
		while (!ControllerServer.isReplicatedRTimes(file))
			try { Thread.sleep(100); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.STORE_COMPLETE_TOKEN, "");
		ControllerServer.setFileState(file, State.STORE_COMPLETE);
	}
	
	private void handleStoreAck(String filename) throws IOException
	{
		ControllerServer.addNewDatabasePort(filename);
	}
	
	private void handleLoadRequest(final String file) throws IOException
	{
		sendMessage(Protocol.LOAD_FROM_TOKEN, ControllerServer.selectDStore(file) + " " + ControllerServer.getFileSize(file));
	}
	
	private void handleRemoveRequest(final String file)
	{
		ControllerServer.setFileState(file, State.REMOVE_IN_PROGRESS);
		
		for (Socket dstore : ControllerServer.getDStores(file))
		{
			try
			{
				MessageSocket.sendMessage(Protocol.REMOVE_TOKEN, file, dstore);
				
				if (!MessageSocket.getOpcode(MessageSocket.receiveMessage(dstore)).equals(Protocol.REMOVE_ACK_TOKEN))
					throw new IOException("Sadge");
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		//TODO: change the way that a file si tested to be there
		ControllerServer.setFileState(file, State.REMOVE_COMPLETE);
		
		sendMessage(Protocol.REMOVE_COMPLETE_TOKEN, "");
	}
}