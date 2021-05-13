package sockets.dstore;

import logger.DstoreLogger;
import logger.Logger;
import sockets.Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class DstoreServer extends Server
{
	private static String file_folder;
	
	private static DstoreController controller;
	
	public DstoreServer(final int port, final int cport, final int timeout, final String file_folder)
	{
		super(port, timeout);
		
		DstoreServer.file_folder = file_folder;
		
		try
		{
			DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
			
			if (!Files.isDirectory(Paths.get(file_folder)))
				Files.createDirectory(Paths.get(file_folder));
			else
			{
				for(File file: new File(file_folder).listFiles())
					if (!file.isDirectory())
						file.delete();
			}
			
			controller = new DstoreController(cport, port);
			new Thread(controller).start();
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
	public static void messageController(final String opcode, final String msg) { controller.sendMessage(opcode, msg); }
	
	public static String list() { return String.join(" ", Objects.requireNonNull(new File(file_folder).list())); }
}
