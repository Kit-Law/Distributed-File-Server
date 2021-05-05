package mains;
import Constants.Protocol;
import Sockets.DstoreServer;
import Sockets.MessageSocket;
import Sockets.MessageClient;
import Sockets.fileTransfer.FileReceiver;
import Sockets.fileTransfer.FileSender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
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
		super(in, out);
		this.client = client;
	}
	
	@Override
	public void run()
	{
		try
		{
			handleMessage();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	protected void handleMessage() throws IOException
	{
		//Logger.info.log("Reading...");
		// create a ServerSocketChannel to read the request
		//SocketChannel client = (SocketChannel) key.channel();
		
		String msg = receiveMessage();
		String[] operand = MessageSocket.getOperand(msg);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.LIST_TOKEN:
				handleListRequest();
				break;
			case Protocol.STORE_TOKEN:
				handleStoreRequest(operand[0], Long.parseLong(operand[1]));
				break;
			case Protocol.LOAD_DATA_TOKEN:
				handleLoadRequest(operand[0]);
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(operand[0]);
		}
		
		client.close();
	}
	
	private void handleListRequest() throws IOException
	{
		MessageSocket.sendMessage(Protocol.LIST_TOKEN, DstoreServer.list(), client);
	}
	
	private void handleStoreRequest(final String filename, long filesize) throws IOException
	{
		MessageSocket.sendMessage(Protocol.ACK_TOKEN, "", client);
		
		FileReceiver.receive(client, Paths.get(DstoreServer.getFile_folder() + "/" + filename), filesize);
		
		DstoreServer.messageController(Protocol.STORE_ACK_TOKEN, filename);
	}
	
	private void handleLoadRequest(final String filename)
	{
		FileSender.transfer(Paths.get("./" + DstoreServer.getFile_folder() + "/" + filename), client);
	}
	
	private void handleRemoveRequest(final String filename) throws IOException
	{
		Files.delete(Paths.get("./" + DstoreServer.getFile_folder() + "/" + filename));
		
		DstoreServer.messageController(Protocol.REMOVE_ACK_TOKEN, filename);
	}
}
