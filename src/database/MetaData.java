package database;

import java.io.Serializable;
import java.util.*;

/**
 * A class to store the information of a file. This stores the current
 * State that the file is in, the ports of the dstores that the file
 * has been stored to and the size of the file.
 */
public class MetaData implements Serializable
{
	private State state;
	private long size;
	private ArrayList<Integer> dstorePorts = new ArrayList<>();
	
	public MetaData(State state, long size, Integer[] dstorePorts)
	{
		this.state = state;
		this.size = size;
		this.dstorePorts.addAll(Arrays.asList(dstorePorts));
	}
	
	public State getState() { return state; }
	public long getSize() { return size; }
	public ArrayList<Integer> getDstorePorts() { return dstorePorts; }
	
	/**
	 * Set the current state of a file in the database.
	 *
	 * @param state Current state of the file.
	 */
	public void setState(State state) { this.state = state; }
	
	public void addPorts(ArrayList<Integer> toStore) { dstorePorts.addAll(toStore); }
	public void removePort(int toRemove) { if (dstorePorts.contains(toRemove)) dstorePorts.removeIf(port -> port == toRemove); }
	public void validatePort(int port) { if (!dstorePorts.contains(port)) dstorePorts.add(port); }
	
	/**
	 * A simple parser over the State enum to get the name of a state.
	 *
	 * @return A parsed name of the state.
	 */
	public String parseState()
	{
		return state.name().replace('_', ' ').toLowerCase();
	}
}
