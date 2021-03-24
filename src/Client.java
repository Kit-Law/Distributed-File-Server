import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client
{
	private SocketChannel controller;
	
	private int cport = -1;
	private int timeout = -1;
	
	//java Client cport timeout
	public static void main(String args[])
	{
		new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	private Client(int cport, int timeout)
	{
		this.cport = cport;
		this.timeout = timeout;
		
		try
		{
			connectToServer();
		}
		catch (IOException e) { System.err.println("Error: Failed to connect to server on port " + cport + ", with error, " + e.getMessage()); }
		
		while (true)
		{
			try
			{
				System.out.print("<Client> ");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String command = br.readLine();
				
				switch (command.contains(" ") ? command.substring(0, command.indexOf(' ')) : command)
				{
					case "STORE":
						store(command);
						break;
					case "LOAD":
						load(command);
						break;
					case "REMOVE":
						remove(command);
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
			}
			catch (IOException e) { System.err.println(e.getMessage()); }
		}
	}
	
	private void store(String filename)
	{
		try
		{
			sendMessage("Hello world", controller);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private void load(String filename)
	{
	
	}
	
	private void remove(String filename)
	{
	
	}
	
	public void sendMessage(String msg, SocketChannel socket) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		buffer.put(msg.getBytes());
		buffer.flip();
		int bytesWritten = socket.write(buffer);
		
		System.out.println("Sent message: \"" + msg + "\", " + bytesWritten + " bytes to: " + socket + ".");
		
		buffer.clear();
		buffer.put(new byte[1024]);
		buffer.clear();
		
		socket.read(buffer);
		String response = new String(buffer.array()).trim();
		System.out.println("response=" + response);
		buffer.clear();
	}
	
	private void connectToServer() throws IOException
	{
		System.out.println("Connecting to controller.");
		controller = SocketChannel.open(new InetSocketAddress("localhost", cport));
	}
}
