package database;

import java.io.Serializable;

public enum State implements Serializable
{
	STORE_IN_PROGRESS,
	STORE_COMPLETE,
	REMOVE_IN_PROGRESS,
	REMOVE_COMPLETE;
}
