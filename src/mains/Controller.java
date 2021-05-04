package mains;

import Constants.Protocol;
import Sockets.ControllerServer;
import Sockets.MessageClient;
import Sockets.MessageSocket;
import database.MetaData;
import database.State;

import java.io.*;
import java.net.Socket;

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
		System.out.println(msg);
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
	
	private void handleDstoreConnect(String port)
	{
		ControllerServer.addDStore(Integer.parseInt(port), client);
		
		//Logger.info.log("DStore: " + dstore + ", has been added.");
	}
	
	private void handleListRequest() throws IOException
	{
		sendMessage(Protocol.LIST_TOKEN, ControllerServer.getFileList());
	}
	
	private void handleStoreRequest(final String file, final long filesize) throws IOException
	{
		ControllerServer.freeFile(file);
			
		//Creates a new database entry
		ControllerServer.newDatabaseEntry(file, new MetaData(State.STORE_IN_PROGRESS, filesize));
		
		//Send the ports to the client.
		sendMessage(Protocol.STORE_TO_TOKEN, ControllerServer.getRdstorePorts());
		
		while (!ControllerServer.isReplicatedRTimes(file))
		{
			try
			{
				Thread.sleep(100);
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		sendMessage(Protocol.STORE_COMPLETE_TOKEN, "");
		ControllerServer.setFileState(file, State.STORE_COMPLETE);
	}
	
	private void handleStoreAck(String filename) throws IOException
	{
		ControllerServer.addNewDatabasePort(filename, client.getLocalPort());		//TODO: this is wrong
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