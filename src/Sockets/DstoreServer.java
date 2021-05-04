package Sockets;

import Constants.Protocol;
import mains.Dstore;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DstoreServer extends Server
{
	private static int cport;
	private static String file_folder;
	
	private static MessageClient controller;
	
	public DstoreServer(final int port, final int cport, final int timeout, final String file_folder)
	{
		super(port, timeout);
		
		DstoreServer.cport = cport;
		DstoreServer.file_folder = file_folder;
		
		try
		{
			if (!Files.isDirectory(Paths.get(file_folder)))
				Files.createDirectory(Paths.get(file_folder));
			
			controller = new MessageClient(cport);
			controller.sendMessage(Protocol.JOIN_TOKEN, String.valueOf(port));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		launchServer();
	}
	
	@Override
	protected void handleConnection(Socket client) throws IOException
	{
		// obtaining input and out streams
		DataInputStream in = new DataInputStream(client.getInputStream());
		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		
		// create a new thread object
		new Thread(new Dstore(client, in, out)).start();
	}
	
	public static String getFile_folder() { return file_folder; }
	public static void messageController(final String opcode, final String msg) throws IOException { controller.sendMessage(opcode, msg); }
}
