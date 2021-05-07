package sockets.dstore;

import constants.Protocol;
import sockets.message.MessageClient;
import sockets.message.MessageSocket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DstoreController extends MessageClient implements Runnable
{
	final int port;
	
	public DstoreController(final int cport, final int port)
	{
		super(cport);
		this.port = port;
	}
	
	@Override
	public void run()
	{
		try
		{
			sendMessage(Protocol.JOIN_TOKEN,  String.valueOf(port));
			
			while (true)
			{
				handleMessage();
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	protected void handleMessage() throws IOException
	{
		String msg = receiveMessage();
		String[] operand = MessageSocket.getOperand(msg);
		
		switch (MessageSocket.getOpcode(msg))
		{
			case Protocol.LIST_TOKEN:
				handleListRequest();
				break;
			case Protocol.REMOVE_TOKEN:
				handleRemoveRequest(operand[0]);
				break;
			default:
				System.err.println("Malformed Message Received: " + msg);
				break;
		}
	}
	
	private void handleListRequest()
	{
		sendMessage(Protocol.LIST_TOKEN, DstoreServer.list());
	}
	
	private void handleRemoveRequest(final String filename) throws IOException
	{
		Files.delete(Paths.get("./" + DstoreServer.getFile_folder() + "/" + filename));
		
		sendMessage(Protocol.REMOVE_ACK_TOKEN, filename);
	}
}
