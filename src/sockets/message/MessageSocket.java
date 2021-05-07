package sockets.message;

import logger.Logger;

import java.io.*;
import java.net.Socket;

public class MessageSocket
{
	public static void sendMessage(final String opcode, final String msg, Socket socket, Logger logger, Socket from) throws IOException
	{
		new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true).println(opcode + " " + msg);
		logger.messageSent(from, opcode + " " + msg);
	}
	
	public static String receiveMessage(Socket socket, Logger logger, Socket from) throws IOException
	{
		String msg = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
		logger.messageReceived(from, msg);
		
		return msg;
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
