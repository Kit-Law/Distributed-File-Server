package database;

import java.io.Serializable;

/**
 * An enum that holds the state that a file in the database is in.
 */
public enum State implements Serializable
{
	/** Used when a store has just been requested by the client. */
	STORE_IN_PROGRESS,
	/** Used when a store has been committed to every dstore. */
	STORE_COMPLETE,
	/** Used when a Client requests the removable of a file form every dstore. */
	REMOVE_IN_PROGRESS,
	/** Used when a every dstore has removed their copy of a file. */
	REMOVE_COMPLETE
}
