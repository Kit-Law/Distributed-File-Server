package sockets.controller;

import constants.Protocol;
import exeptions.NotEnoughDstores;
import logger.ControllerLogger;
import sockets.message.MessageClient;
import sockets.message.MessageSocket;
import database.MetaData;
import database.State;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller extends MessageClient implements Runnable
{
	private Socket client;
	private Integer port = null;
	
	//java Controller cport R timeout
	public static void main(String[] args)
	{
		new ControllerServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
	
	public Controller(Socket client, DataInputStream in, DataOutputStream out)
	{
		super(client, in, out);
		this.client = client;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				handleMessage();
			}
			catch (SocketException e) { if (port != null) ControllerServer.dstoreFailed(port); return; }
			catch (FileAlreadyExistsException e) { sendMessage(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN, ""); }
			catch (FileNotFoundException e) { sendMessage(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN, ""); }
			catch (NotEnoughDstores e) { sendMessage(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN, ""); }
			catch (SocketTimeoutException e) { e.printStackTrace(); return; }
			catch (NullPointerException e) { return; }
			catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	protected void handleMessage() throws IOException
	{
		String msg = receiveMessage();
		String[] operand = MessageSocket.getOperand(msg);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.JOIN_TOKEN:
				handleDstoreConnect(Integer.parseInt(operand[0]));
				break;
			case Protocol.LIST_TOKEN:
				if (ControllerServer.isDstore(client)) handleRebalList(msg);
				else handleListRequest();
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
			case Protocol.RELOAD_TOKEN:
				handleReLoadRequest(operand[0]);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(operand[0]);
				break;
			case Protocol.REMOVE_ACK_TOKEN:
				handleRemoveAck(operand[0]);
				break;
			case Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN:
				handleRemoveAck(operand[0]);
				break;
			case Protocol.REBALANCE_COMPLETE_TOKEN:
				RebalancingControllerServer.rebalanceComplete = true;
				break;
			default:
				System.err.println("Malformed Message Received: " + msg);
				break;
		}
	}
	
	private void handleDstoreConnect(Integer port) throws IOException
	{
		this.port = port;
		ControllerServer.addDStore(port, client);
		ControllerLogger.getInstance().dstoreJoined(getSocket(), port);
		
		while (RebalancingControllerServer.isRebalancing)
			try { Thread.sleep(10); }
			catch (Exception e) { e.printStackTrace(); }
		
		new Thread(new RebalancingControllerServer()).start();
	}
	
	private void handleRebalList(String msg)
	{
		RebalancingControllerServer.msg = msg;
		RebalancingControllerServer.msgReceived = true;
	}
	
	private void handleListRequest() throws NotEnoughDstores
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		sendMessage(Protocol.LIST_TOKEN, ControllerServer.getDatabase().getFileList());
	}
	
	private void handleStoreRequest(final String file, final long filesize) throws FileAlreadyExistsException, FileNotFoundException, NotEnoughDstores, SocketException
	{
		client.setSoTimeout(ControllerServer.getTimeout());
		
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		ControllerServer.getDatabase().freeFile(file);
		Integer[] dstorePorts;
		
		synchronized (ControllerServer.getDatabase())
		{
			dstorePorts = ControllerServer.getRdstores();
			
			//Creates a new database entry
			ControllerServer.getDatabase().newEntry(file, new MetaData(State.STORE_IN_PROGRESS, filesize, dstorePorts));
		}
		
		//Send the ports to the client.
		sendMessage(Protocol.STORE_TO_TOKEN, Stream.of(dstorePorts).map(Object::toString).collect(Collectors.joining(" ")));
		
		while (!ControllerServer.getDatabase().isReplicatedRTimes(file, ControllerServer.getR()))
			try { Thread.sleep(10); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.STORE_COMPLETE_TOKEN, "");
		ControllerServer.getDatabase().updateFileState(file, State.STORE_COMPLETE);
		
		client.setSoTimeout(0);
	}
	
	private void handleStoreAck(String filename)
	{
		ControllerServer.getDatabase().getMetaData(filename).incrementStoreAcks();
	}
	
	private void handleLoadRequest(final String file) throws NotEnoughDstores, FileNotFoundException, SocketException
	{
		client.setSoTimeout(ControllerServer.getTimeout());
		
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		Integer port = ControllerServer.getDatabase().selectDStore(file, 0);
		ControllerServer.addLoad(client, port);
		
		sendMessage(Protocol.LOAD_FROM_TOKEN, port + " " + ControllerServer.getDatabase().getMetaData(file).getSize());
		
		client.setSoTimeout(0);
	}
	
	private void handleReLoadRequest(String file) throws NotEnoughDstores, FileNotFoundException
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		try { sendMessage(Protocol.LOAD_FROM_TOKEN, ControllerServer.reLoad(client, file) + " " + ControllerServer.getDatabase().getMetaData(file).getSize()); }
		catch (IOException e) { sendMessage(Protocol.ERROR_LOAD_TOKEN, ""); }
	}
	
	private void handleRemoveRequest(final String file) throws NotEnoughDstores, FileNotFoundException, SocketException
	{
		client.setSoTimeout(ControllerServer.getTimeout());
		
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		if (!ControllerServer.getDatabase().contains(file))
			throw new FileNotFoundException();
		
		if (ControllerServer.getDatabase().getMetaData(file).getState() != State.REMOVE_IN_PROGRESS)
		{
			ControllerServer.getDatabase().updateFileState(file, State.REMOVE_IN_PROGRESS);
			
			for (Socket dstore : ControllerServer.getDStores(file))
			{
				try
				{
					MessageSocket.sendMessage(Protocol.REMOVE_TOKEN, file, dstore, ControllerLogger.getInstance(), getSocket());
				}
				catch (Exception e) { e.printStackTrace(); }
			}
		}
		
		while (!ControllerServer.getDatabase().isRemoved(file))
			try { Thread.sleep(10); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.REMOVE_COMPLETE_TOKEN, "");
		ControllerServer.getDatabase().updateFileState(file, State.REMOVE_COMPLETE);
		
		client.setSoTimeout(0);
	}
	
	private void handleRemoveAck(String filename)
	{
		ControllerServer.getDatabase().getMetaData(filename).incrementRemoveAcks();
	}
	
	public void pause() throws InterruptedException { Thread.sleep(100); }
}