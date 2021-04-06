import Constants.OpCodes;
import Sockets.messageClient;
import fileTransfer.FileReciver;
import logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import fileTransfer.FileSender;

public class Client extends messageClient
{
	//java Client cport timeout
	public static void main(String args[])
	{
		new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	private Client(int cport, int timeout)
	{
		super(cport, timeout);
		
		pollUser();
	}
	
	private void pollUser()
	{
		while (true)
		{
			try
			{
				System.out.print("<Client> ");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String[] command = br.readLine().split(" ");
				
				switch (command[0])
				{
					case "STORE":
						store(command[1]);
						break;
					case "LOAD":
						load(command[1]);
						break;
					case "REMOVE":
						remove(command[1]);
						break;
					case "--help":
						Logger.info.log("Usage: STORE filename");
						Logger.info.log("       LOAD filename");
						Logger.info.log("       REMOVE filename");
						break;
					case "EXIT":
						System.exit(0);
					default:
						Logger.err.log("Parsing the command. Try --help for usage.");
				}
			}
			catch (IOException e) { Logger.err.log(e.getMessage()); }
		}
	}
	
	private void store(String filename)
	{
		try
		{
			sendMessage(OpCodes.CONTROLLER_STORE_REQUEST, filename, controller);
			
			String response = receiveMessage(controller);
			
			if (getOpcode(response) != OpCodes.STORE_TO)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
			
			Arrays.asList(getOperand(response).split(" ")).forEach(s -> {
				try { storeToDstore(filename, Integer.parseInt(s)); }
				catch (IOException e) { e.printStackTrace(); }
			});
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private void storeToDstore(String filename, int port) throws IOException
	{
		Path filePath = Paths.get(filename);
		long fileSize = Files.size(filePath);
		
		SocketChannel dstore = SocketChannel.open(new InetSocketAddress(port));
		
		sendMessage(OpCodes.DSTORE_STORE_REQUEST, filename + " " + fileSize, dstore);
		
		if (Integer.parseInt(receiveMessage(dstore)) != OpCodes.ACK)
			throw new IOException("??");
		
		FileSender.transfer(filePath, dstore);
	}
	
	private void load(String filename)
	{
		try
		{
			sendMessage(OpCodes.CONTROLLER_LOAD_REQUEST, filename, controller);
			
			String response = receiveMessage(controller);
			
			if (getOpcode(response) != OpCodes.LOAD_FROM)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
			
			SocketChannel dstore = SocketChannel.open(new InetSocketAddress(Integer.parseInt(getOperand(response))));
			
			sendMessage(OpCodes.LOAD_DATA, filename, dstore);
			
			FileReciver.recive(Paths.get("./" + filename));
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private void remove(String filename)
	{
		try
		{
			sendMessage(OpCodes.CONTROLLER_REMOVE_REQUEST, filename, controller);
			
			String response = receiveMessage(controller);
			
			if (getOpcode(response) != OpCodes.REMOVE_COMPLETE)
				//TODO: make an exception class
				throw new IOException("Wrong opcode received");
		}
		catch (IOException e) { e.printStackTrace(); }
	}
}
