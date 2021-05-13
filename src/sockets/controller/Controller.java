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
			case Protocol.RELOAD_TOKEN:
				handleReLoadRequest(operand[0]);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(operand[0]);
				break;
			case Protocol.REMOVE_ACK_TOKEN:
				handleRemoveAck(operand[0]);
				break;
			default:
				System.err.println("Malformed Message Received: " + msg);
				break;
		}
	}
	
	private void handleDstoreConnect(Integer port) throws IOException
	{
		client.setSoTimeout(999999999);
		
		this.port = port;
		ControllerServer.addDStore(port, client);
		ControllerLogger.getInstance().dstoreJoined(getSocket(), port);
		
		RebalancingControllerServer.handleRebalance();
	}
	
	private void handleListRequest() throws NotEnoughDstores
	{
		//if (ControllerServer.isDstore(client))
		//	return;
		
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		sendMessage(Protocol.LIST_TOKEN, ControllerServer.getFileList());
	}
	
	private void handleStoreRequest(final String file, final long filesize) throws FileAlreadyExistsException, FileNotFoundException, NotEnoughDstores
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		ControllerServer.freeFile(file);
		Integer[] dstorePorts = ControllerServer.getRdstores();
		
		//Creates a new database entry
		ControllerServer.newDatabaseEntry(file, new MetaData(State.STORE_IN_PROGRESS, filesize, dstorePorts));
		ControllerServer.clearRemoveACKS(file);
		
		//Send the ports to the client.
		sendMessage(Protocol.STORE_TO_TOKEN, Stream.of(dstorePorts).map(Object::toString).collect(Collectors.joining(" ")));
		
		while (!ControllerServer.isReplicatedRTimes(file))
			try { Thread.sleep(10); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.STORE_COMPLETE_TOKEN, "");
		ControllerServer.setFileState(file, State.STORE_COMPLETE);
		ControllerServer.clearStoreACKS(file);
	}
	
	private void handleStoreAck(String filename)
	{
		ControllerServer.addNewStoreAck(filename);
	}
	
	private void handleLoadRequest(final String file) throws NotEnoughDstores, FileNotFoundException
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		Integer port = ControllerServer.selectDStore(file, 0);
		ControllerServer.addLoad(client, port);
		
		sendMessage(Protocol.LOAD_FROM_TOKEN, port + " " + ControllerServer.getFileSize(file));
	}
	
	private void handleReLoadRequest(String file) throws NotEnoughDstores, FileNotFoundException
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		try { sendMessage(Protocol.LOAD_FROM_TOKEN, ControllerServer.reLoad(client, file) + " " + ControllerServer.getFileSize(file)); }
		catch (IOException e) { sendMessage(Protocol.ERROR_LOAD_TOKEN, ""); }
	}
	
	private void handleRemoveRequest(final String file) throws NotEnoughDstores, FileNotFoundException
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		ControllerServer.setFileState(file, State.REMOVE_IN_PROGRESS);
		
		for (Socket dstore : ControllerServer.getDStores(file))
		{
			try
			{
				MessageSocket.sendMessage(Protocol.REMOVE_TOKEN, file, dstore, ControllerLogger.getInstance(), getSocket());
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		while (!ControllerServer.isRemoved(file))
			try { Thread.sleep(10); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.REMOVE_COMPLETE_TOKEN, "");
		ControllerServer.setFileState(file, State.REMOVE_COMPLETE);
		ControllerServer.clearRemoveACKS(file);
	}
	
	private void handleRemoveAck(String filename)
	{
		ControllerServer.addNewRemoveAck(filename);
	}
	
	public void pause() throws InterruptedException { Thread.sleep(100); }
}