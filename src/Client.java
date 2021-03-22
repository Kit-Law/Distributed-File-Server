import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client
{
	private static SocketChannel client;
	private static ByteBuffer buffer;
	private static EchoClient instance;
	
	private static int cport = -1;
	private static int timeout = -1;
	
	//java Client cport timeout
	public static void main(String args[])
	{
		cport = Integer.parseInt(args[0]);
		timeout = Integer.parseInt(args[1]);
		
		new Client();
	}
	
	private Client()
	{
		while (true)
		{
			String[] command = System.console().readLine().split("\\s+");
			
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
				case "EXIT":
					System.exit(0);
				default:
					System.out.println("Error parsing the command.");
			}
		}
	}
	
	private void store(String filename)
	{
	
	}
	
	private void load(String filename)
	{
	
	}
	
	private void remove(String filename)
	{
	
	}
	
	public String sendMessage(String msg)
	{
		buffer = ByteBuffer.wrap(msg.getBytes());
		String response = null;
		try {
			client.write(buffer);
			buffer.clear();
			client.read(buffer);
			response = new String(buffer.array()).trim();
			System.out.println("response=" + response);
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
}
