package sockets.dstore;
import constants.Protocol;
import logger.DstoreLogger;
import sockets.message.MessageSocket;
import sockets.message.MessageClient;
import sockets.fileTransfer.FileReceiver;
import sockets.fileTransfer.FileSender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;

public class Dstore extends MessageClient implements Runnable
{
	private Socket client;
	
	//Dstore port cport timeout file_folder
	public static void main(String[] args)
	{
		new DstoreServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
	}
	
	public Dstore(Socket client, DataInputStream in, DataOutputStream out)
	{
		super(client, in, out);
		this.client = client;
	}
	
	@Override
	public void run()
	{
		try
		{
			handleMessage();
		}
		catch (FileAlreadyExistsException e) { sendMessage(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN, ""); }
		catch (Exception e) { e.printStackTrace(); }
	}
	
	protected void handleMessage() throws IOException
	{
		String msg = receiveMessage();
		String[] operand = MessageSocket.getOperand(msg);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.STORE_TOKEN:
				handleStoreRequest(operand[0], Long.parseLong(operand[1]));
				break;
			case Protocol.LOAD_DATA_TOKEN:
				handleLoadRequest(operand[0]);
				break;
			case Protocol.REBALANCE_STORE_TOKEN:
				handleRebalanceStore(operand[0], Long.parseLong(operand[1]));
				break;
			default:
				System.err.println("Malformed Message Received: " + msg);
				break;
		}
		
		client.close();
	}
	
	private void handleStoreRequest(final String filename, long filesize) throws IOException
	{
		MessageSocket.sendMessage(Protocol.ACK_TOKEN, "", client, DstoreLogger.getInstance(), getSocket());
		
		FileReceiver.receive(client, Paths.get(DstoreServer.getFile_folder() + "/" + filename), filesize);
		
		DstoreServer.messageController(Protocol.STORE_ACK_TOKEN, filename);
	}
	
	private void handleLoadRequest(final String filename) throws IOException
	{
		FileSender.transfer(Paths.get("./" + DstoreServer.getFile_folder() + "/" + filename), client);
	}
	
	private void handleRebalanceStore(String filename, long filesize) throws IOException
	{
		MessageSocket.sendMessage(Protocol.ACK_TOKEN, "", client, DstoreLogger.getInstance(), getSocket());
		
		FileReceiver.receive(client, Paths.get(DstoreServer.getFile_folder() + "/" + filename), filesize);
	}
}
