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
import java.nio.file.FileAlreadyExistsException;
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
			//catch (SocketException e) { e.printStackTrace(); }
			catch (FileAlreadyExistsException e) { sendMessage(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN, ""); }
			catch (FileNotFoundException e) { sendMessage(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN, ""); }
			catch (NotEnoughDstores e) { sendMessage(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN, ""); }
			catch (NullPointerException e) { break; }
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
			case Protocol.REMOVE_ACK_TOKEN:
				handleRemoveAck(operand[0]);
				break;
			default:
				System.err.println("Malformed Message Received: " + msg);
				break;
		}
	}
	
	private void handleDstoreConnect(String port) throws IOException
	{
		ControllerServer.addDStore(Integer.parseInt(port), client);
		ControllerLogger.getInstance().dstoreJoined(getSocket(), Integer.parseInt(port));
		
		RebalancingControllerServer.handleRebalance(getSocket());
	}
	
	private void handleListRequest() throws NotEnoughDstores
	{
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
		
		//Send the ports to the client.
		sendMessage(Protocol.STORE_TO_TOKEN, Stream.of(dstorePorts).map(Object::toString).collect(Collectors.joining(" ")));
		
		while (!ControllerServer.isReplicatedRTimes(file))
			try { Thread.sleep(100); }
			catch (Exception e) { e.printStackTrace(); }
		
		sendMessage(Protocol.STORE_COMPLETE_TOKEN, "");
		ControllerServer.setFileState(file, State.STORE_COMPLETE);
	}
	
	private void handleStoreAck(String filename)
	{
		ControllerServer.addNewStoreAck(filename);
	}
	
	private void handleLoadRequest(final String file) throws NotEnoughDstores, FileNotFoundException
	{
		if (!ControllerServer.hasEnoughDstores())
			throw new NotEnoughDstores();
		
		sendMessage(Protocol.LOAD_FROM_TOKEN, ControllerServer.selectDStore(file) + " " + ControllerServer.getFileSize(file));
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
			try { Thread.sleep(100); }
			catch (Exception e) { e.printStackTrace(); }
		
		//TODO: change the way that a file si tested to be there
		ControllerServer.setFileState(file, State.REMOVE_COMPLETE);
		
		sendMessage(Protocol.REMOVE_COMPLETE_TOKEN, "");
	}
	
	private void handleRemoveAck(String filename)
	{
		ControllerServer.addNewRemoveAck(filename);
	}
}