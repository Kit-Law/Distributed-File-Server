import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Controller
{
	private static int cport = -1;
	private static int R = -1;
	private static int timeout = -1;
	
	private static HashMap<String, MetaData> database = new HashMap<String, MetaData>();
	
	private static Selector selector = null;
	
	//java Controller cport R timeout
	public static void main(String args[])
	{
		cport = Integer.parseInt(args[0]);
		R = Integer.parseInt(args[1]);
		timeout = Integer.parseInt(args[2]);
		
		try
		{
			selector = Selector.open();
			ServerSocketChannel socket = ServerSocketChannel.open();
			ServerSocket serverSocket = socket.socket();
			serverSocket.bind(new InetSocketAddress("localhost", cport));
			socket.configureBlocking(false);
			int ops = socket.validOps();
			socket.register(selector, ops, null);
			
			while (true)
			{
				try
				{
					selector.select();
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iter = selectedKeys.iterator();
					while (iter.hasNext())
					{
						SelectionKey key = iter.next();
						
						if (key.isAcceptable())
						{
							handleAccept(socket, key);
						}
						
						if (key.isReadable())
						{
							handleRead(key);
						}
						
						if (key.isWritable())
						{
							handleWrite(key);
						}
						
						iter.remove();
					}
				}
				catch (ClosedChannelException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/*private static void parseCommand(String command)
	{
		String[] components = command.split("\\s+");
		
		switch (components[0])
		{
			case "STORE":
				store(components[1]);
				break;
			case "LOAD":
				load(components[1]);
				break;
			case "REMOVE":
				remove(components[1]);
				break;
			case "--help":
				System.out.println("Usage: STORE filename");
				System.out.println("       LOAD filename");
				System.out.println("       REMOVE filename");
				break;
			case "EXIT":
				System.exit(0);
			default:
				System.out.println("Error: Parsing the command. Try --help for usage.");
		}
	}*/
	
	private static void handleAccept(ServerSocketChannel mySocket, SelectionKey key) throws IOException
	{
		System.out.println("Connection Accepted...");
		
		// Accept the connection and set non-blocking mode
		SocketChannel client = mySocket.accept();
		client.configureBlocking(false);
		
		// Register that client is reading this channel
		client.register(selector, SelectionKey.OP_READ);
	}
	
	private static void handleRead(SelectionKey key) throws IOException
	{
		System.out.println("Reading...");
		// create a ServerSocketChannel to read the request
		SocketChannel client = (SocketChannel) key.channel();
		
		// Create buffer to read data
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		client.read(buffer);
		//Parse data from buffer to String
		String data = new String(buffer.array()).trim();
		
		System.out.println("Read msg: " + data);
		
		client.register(selector, SelectionKey.OP_WRITE);
	}
	
	private static void handleWrite(SelectionKey key) throws IOException
	{
		String msg = "Hi!";
		
		SocketChannel socket = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
		buffer.put(msg.getBytes());
		buffer.flip();
		int bytesWritten = socket.write(buffer);
		
		System.out.println("Sent message: \"" + msg + "\", " + bytesWritten + " bytes to: " + socket + ".");
		
		socket.register(selector, SelectionKey.OP_READ);
	}
	
	public static void saveDatabase()
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream("Database.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(database);
			out.close();
			fileOut.close();
			System.out.println("<Server> Database saved.");
		}
		catch (IOException i) { i.printStackTrace(); }
	}
	
	public static void loadDatabase()
	{
		try
		{
			FileInputStream fileIn = new FileInputStream("Database.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			database = (HashMap<String, MetaData>) in.readObject();
			in.close();
			fileIn.close();
			System.out.println("<Server> Database loaded.");
		}
		catch (Exception e) { System.err.println(e.getMessage() + e.getCause()); }
	}
}