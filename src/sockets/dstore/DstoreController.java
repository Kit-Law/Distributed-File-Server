package sockets.dstore;

import constants.Protocol;
import logger.DstoreLogger;
import sockets.fileTransfer.FileSender;
import sockets.message.MessageClient;
import sockets.message.MessageSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
		catch (FileAlreadyExistsException e) { sendMessage(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN, ""); }
		catch (NoSuchFileException e) { sendMessage(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN, ""); }
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
			case Protocol.REBALANCE_TOKEN:
				handleRebalance(operand);
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
		Files.delete(Paths.get(DstoreServer.getFile_folder() + "/" + filename));
		
		sendMessage(Protocol.REMOVE_ACK_TOKEN, filename);
	}
	
	private void handleRebalance(String[] operand) throws IOException
	{
		int numToStore = Integer.parseInt(operand[0]);
		
		int i = 1;
		for (int fileIndex = 0; fileIndex < numToStore; fileIndex++)
		{
			String filename = operand[i];
			long fileSize = FileChannel.open(Paths.get(DstoreServer.getFile_folder() + "/" + filename)).size();
			int numOfDstores = Integer.parseInt(operand[i + 1]);
			
			i += 2;
			
			for (int dstoreNumber = 0; dstoreNumber < numOfDstores; dstoreNumber++, i++)
			{
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(operand[i])));
				
				Socket dstore = socketChannel.socket();
				
				MessageSocket.sendMessage(Protocol.REBALANCE_STORE_TOKEN, filename + " " + fileSize, dstore, DstoreLogger.getInstance(), getSocket());
				
				if (!MessageSocket.receiveMessage(dstore, DstoreLogger.getInstance(), getSocket()).equals(Protocol.ACK_TOKEN + " "))
					throw new IOException("CUNT");
				
				FileSender.transfer(Paths.get(DstoreServer.getFile_folder() + "/" + filename), dstore);
				
				socketChannel.close();
			}
		}
		
		for (i++; i < operand.length; i++)
			Files.delete(Paths.get(DstoreServer.getFile_folder() + "/" + operand[i]));
		
		sendMessage(Protocol.REBALANCE_COMPLETE_TOKEN, "");
	}
}
