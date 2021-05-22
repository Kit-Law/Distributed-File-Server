package database;

import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Database
{
	private ConcurrentHashMap<String, MetaData> database = new ConcurrentHashMap<>();
	
	public synchronized void newEntry(String filename, MetaData metaData) { database.put(filename, metaData); }
	
	public synchronized void addPort(String filename, int port) { database.get(filename).addPort(port); }
	public synchronized void addPorts(String filename, ArrayList<Integer> ports) { database.get(filename).addPorts(ports); }
	public synchronized void removePort(String filename, int port) { database.get(filename).removePort(port); }
	
	public synchronized void updateFileState(String filename, State state) throws FileNotFoundException
	{
		if (!database.containsKey(filename)) throw new FileNotFoundException(filename);
		else database.get(filename).setState(state);
	}
	
	public synchronized int selectDStore(String filename, Integer index) throws FileNotFoundException //TODO:remove this and make it random or something
	{
		if (!database.containsKey(filename))
			throw new FileNotFoundException(filename);
		
		return database.get(filename).getDstorePorts().get(index);
	}
	
	public boolean isReplicatedRTimes(String filename, int R)
	{
		if (database.containsKey(filename) && database.get(filename).getState() == State.STORE_COMPLETE)
		{
			return true;
		}
		if (database.containsKey(filename) && database.get(filename).getStoreAcks() >= R)
		{
			database.get(filename).resetStroeAcks();
			return true;
		}
		else return false;
	}
	public boolean isRemoved(String filename)
	{
		if (!database.containsKey(filename) || database.get(filename).getState() == State.REMOVE_COMPLETE)
		{
			return true;
		}
		if (database.containsKey(filename) &&
				database.get(filename).getRemoveAcks() == database.get(filename).getDstorePorts().size())
		{
			//database.get(filename).resetRemoveAcks(); //TODO: remove filename
			return true;
		}
		else return false;
	}
	
	public synchronized void freeFile(String filename) throws FileAlreadyExistsException
	{
		if (database.containsKey(filename))
			if (database.get(filename).getState() == State.REMOVE_COMPLETE)
				database.remove(filename);
			else
				throw new FileAlreadyExistsException(filename);
	}
	
	public void dstoreFailed(Integer port)
	{
		ArrayList<String> toRemove = new ArrayList<>();
		
		for (Map.Entry<String, MetaData> file : database.entrySet())
		{
			file.getValue().removePort(port);
			if (file.getValue().getDstorePorts().size() == 0) toRemove.add(file.getKey());
		}
		
		for (String file : toRemove)
			database.remove(file);
	}
	
	public synchronized String getFileList()
	{
		ArrayList<String> fileList = new ArrayList<>();
		
		for (Map.Entry<String, MetaData> file : database.entrySet())
			if (file.getValue().getState() == State.STORE_COMPLETE) fileList.add(file.getKey());
		
		return String.join(" ",  fileList);
	}
	
	public void resetRemoveAcks(String file) { database.get(file).resetRemoveAcks(); }
	
	public synchronized boolean contains(String file) { return database.containsKey(file); }
	public synchronized MetaData getMetaData(String file) { return database.get(file); }
	public synchronized Set<Map.Entry<String, MetaData>> getEntrySet() { return database.entrySet(); }
}
