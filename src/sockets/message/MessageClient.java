package sockets.message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MessageClient extends MessageSocket
{
	private BufferedReader in;
	private PrintWriter out;
	
	public MessageClient(DataInputStream in, DataOutputStream out)
	{
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)), true);
	}
	
	public MessageClient(final int port)
	{
		try
		{
			connectToServer(port);
		}
		catch (IOException e)
		{
			//Logger.err.log("Failed to connect to server on port " + port + ", with error, " + e.getMessage());
			System.exit(1);
		}
	}
	
	public void sendMessage(final String opcode, final String msg)
	{
		out.println(opcode + " " + msg);
	}
	
	public String receiveMessage() throws IOException
	{
		return in.readLine();
	}
	
	private void connectToServer(final int port) throws IOException
	{
		//Logger.info.log("Connecting to socket...");
		Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
		//Logger.info.log("Connected to socket: " + socket);

		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
	}
}
