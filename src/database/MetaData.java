package database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
	
	public MetaData(State state)
	{
		this.state = state;
	}
	
	public MetaData(State state, int size, ArrayList<Integer> dstorePorts)
	{
		this.state = state;
		this.size = size;
		this.dstorePorts = dstorePorts;
	}
	
	public State getState() { return state; }
	public long getSize() { return size; }
	public List<Integer> getDstorePorts() { return dstorePorts; }
	
	/**
	 * Set the current state of a file in the database.
	 *
	 * @param state Current state of the file.
	 */
	public void setState(State state) { this.state = state; }
	public void setSize(long size) { this.size = size; }
	
	public void addDStorePort(int port) { this.dstorePorts.add(port); }
	
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
