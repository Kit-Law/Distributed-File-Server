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
	private int storeAcks = 0;
	private int removeAcks = 0;
	
	public MetaData(State state, long size, Integer[] dstorePorts)
	{
		this.state = state;
		this.size = size;
		this.dstorePorts.addAll(Arrays.asList(dstorePorts));
	}
	
	public void addPort(int port) { if (!dstorePorts.contains(port)) dstorePorts.add(port); }
	public void addPorts(ArrayList<Integer> toStore) { dstorePorts.addAll(toStore); }
	public void removePort(int toRemove) { dstorePorts.removeIf(port -> port == toRemove); }
	
	public void incrementStoreAcks() { storeAcks++; }
	public void incrementRemoveAcks() { removeAcks++; }
	public void resetStroeAcks() { storeAcks = 0; }
	public void resetRemoveAcks() { removeAcks = 0; }
	
	public void setState(State state) { this.state = state; } //TODO: check this ageinets the old one
	
	public State getState() { return state; }
	public long getSize() { return size; }
	public ArrayList<Integer> getDstorePorts() { return dstorePorts; }
	public int getStoreAcks() { return storeAcks; }
	public int getRemoveAcks() { return removeAcks; }
}
