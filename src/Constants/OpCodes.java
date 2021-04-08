package Constants;

public class OpCodes
{
	/** A general acknowledgement. */
	public static final int ACK = 0;
	/** A Client requests to the Controller to store a file. */
	public static final int CONTROLLER_STORE_REQUEST = 1;
	/** The Controller sends the Client the available ports. */
	public static final int STORE_TO = 2;
	/** The Client requests to store a file on the DStore. */
	public static final int DSTORE_STORE_REQUEST = 12;
	/** A DStore sends an ack to the Controller when a store is complete. */
	public static final int STORE_ACK = 3;
	/** The Controller sends an ack to the Client when the store is fully complete. */
	public static final int STORE_COMPLETE = 4;
	/** A Client sends a load request to the Controller */
	public static final int CONTROLLER_LOAD_REQUEST = 5;
	/** The Controller sends the port for a DStore that the Client can get the file from. */
	public static final int LOAD_FROM = 6;
	/** A Client requests a DStore for the file. */
	public static final int LOAD_DATA = 7;
	/** A Client requests to the Controller to remove a file. */
	public static final int CONTROLLER_REMOVE_REQUEST = 8;
	/** The Controller requests a remove from a DStore. */
	public static final int DSTORE_REMOVE_REQUEST = 9;
	/** A DStore sends the Controller an ack when the file is removed. */
	public static final int REMOVE_ACK = 10;
	/** The Controller sends a complete ack to the Client when all of the files have been removed. */
	public static final int REMOVE_COMPLETE = 11;
	/** The DStore will send a request to connect to the controller. */
	public static final int DSTORE_CONNECT = 13;
}
