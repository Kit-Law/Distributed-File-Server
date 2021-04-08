package logger;

/**
 * An enum to hold the log levels, the log colours and be able to compare
 * different LogLevels to see witch one has the higher precedence.
 */
public enum LogLevel
{
	INFO(0, "\u001B[32m"),
	WARNING(1, "\u001B[33m"),
	ERROR(2, "\u001B[31m");
	
	private int level;
	private String colour;
	
	LogLevel(int level, String colour)
	{
		this.level = level;
		this.colour = colour;
	}
	
	public boolean satisfies(LogLevel incoming) { return this.level <= incoming.level; }
	
	public String getColour() { return this.colour; }
}
