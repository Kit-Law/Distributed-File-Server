package Sockets;

import java.io.*;
import java.net.Socket;

public class MessageSocket
{
	public static void sendMessage(final String opcode, final String msg, Socket socket) throws IOException
	{
		new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true).println(opcode + " " + msg);
	}
	
	public static String receiveMessage(Socket socket) throws IOException
	{
		return new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
	}
	
	public static String getOpcode(final String msg)
	{
		return (msg.contains(" ")) ? msg.substring(0, msg.indexOf(' ')) : msg;
	}
	
	public static String[] getOperand(final String msg)
	{
		return msg.substring(msg.indexOf(' ') + 1).split(" ");
	}
}
