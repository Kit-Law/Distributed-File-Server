package helpers;

import java.util.concurrent.ConcurrentHashMap;

public class MutableInt implements Comparable<MutableInt>
{
	int value;
	
	public void increment() { ++value; }
	public void decrement() { --value; }
	public int get() { return value; }
	
	public MutableInt(int value) { this.value = value; }
	
	public static <T> void incrementCount(ConcurrentHashMap<T, MutableInt> counter, T key)
	{
		MutableInt count = counter.get(key);
		if (count == null)
			counter.put(key, new MutableInt(1));
		else
			count.increment();
	}
	
	@Override
	public int compareTo(MutableInt o)
	{
		return Integer.compare(value, o.get());
	}
}