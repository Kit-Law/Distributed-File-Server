package logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * A logging class that outputs to a logging file, if the log level is great enough,
 * and to the console in different colours. The log file has the format:
 *
 * [Current system time (s)] [Log level] [msg]
 *
 * @author Christopher Lawrence
 */
public class Logger
{
	/** This holds the path to the output log file. */
	private static Path logFile = null;
	/** This holds the level of the log that needs to be reached
	 * before outputting to the log file. */
	private static LogLevel outputLevel = LogLevel.WARNING;
	
	/** A inner class to wrap logs with LogLevel.INFO. */
	public static class info
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.INFO);
			Logger.logToFile(msg, LogLevel.INFO);
		}
	}
	
	/** A inner class to wrap logs with LogLevel.WARNING. */
	public static class wrn
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.WARNING);
			Logger.logToFile(msg, LogLevel.WARNING);
		}
	}
	
	/** A inner class to wrap logs with LogLevel.ERROR. */
	public static class err
	{
		public static void log(String msg)
		{
			Logger.logToConsole(msg, LogLevel.ERROR);
			Logger.logToFile(msg, LogLevel.ERROR);
		}
	}
	
	/**
	 * Outputs a log message and respective log level to the console.
	 *
	 * @param msg The message to log.
	 * @param level The LogLevel of the message.
	 * */
	private static void logToConsole(String msg, LogLevel level)
	{
		System.out.println(
				level.getColour() +
				(LogLevel.WARNING.satisfies(level) ? level.name() + ": " : "") +
				msg + "\u001B[0m");
	}
	
	/**
	* Outputs a log message and respective log level to the output file.
	*
	* @param msg The message to log.
	* @param level The LogLevel of the message.
	*/
	private static void logToFile(String msg, LogLevel level)
	{
		//Checks if the log is worth outputting to a file
		if (!outputLevel.satisfies(level))
			return;
			
		try
		{
			//Appends the output file
			Files.write(
					logFile,
					(System.currentTimeMillis() + " " + level.name() + " " + msg + "\n").getBytes(),
					StandardOpenOption.APPEND);
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Sets the current log file for the running program.
	 *
	 * @param obj The main object that is going to be used for logging.
	 */
	public static void setLogFile(Loggable obj)
	{
		logFile = Paths.get("./" + obj.toString() + ".log");
		
		try { Files.createFile(logFile); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Sets the log level for the output file.
	 *
	 * @param logLevel The level the needed to reach to output to the log file.
	 */
	public static void setLogLevel(LogLevel logLevel) { outputLevel = logLevel; }
}
