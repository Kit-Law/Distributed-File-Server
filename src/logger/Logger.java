package logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logger
{
	private static Path logFile = null;
	private static LogLevel outputLevel = LogLevel.WARNING;
	
	public static class info
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.INFO);
			Logger.logToFile(msg, LogLevel.INFO);
		}
	}
	
	public static class wrn
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.WARNING);
			Logger.logToFile(msg, LogLevel.WARNING);
		}
	}
	
	public static class err
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.ERROR);
			Logger.logToFile(msg, LogLevel.ERROR);
		}
	}
	
	private static void logToConsole(String msg, LogLevel level)
	{
		System.out.println(
				level.getColour() +
				(LogLevel.WARNING.satisfies(level) ? level.name() + ": " : "") +
				msg + "\u001B[0m");
	}
	
	private static void logToFile(String msg, LogLevel level)
	{
		if (!outputLevel.satisfies(level))
			return;
			
		try
		{
			Files.write(
					logFile,
					(System.currentTimeMillis() + " " + level.name() + " " + msg + "\n").getBytes(),
					StandardOpenOption.APPEND);
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void setLogFile(Loggable obj)
	{
		logFile = Paths.get("./" + obj.toString() + ".log");
		
		try { Files.createFile(logFile); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public static void setLogLevel(LogLevel logLevel) { outputLevel = logLevel; }
}
