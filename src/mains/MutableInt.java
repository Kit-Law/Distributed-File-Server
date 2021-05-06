package mains;

import java.util.HashMap;

public class MutableInt
{
	int value;
	
	public void increment() { ++value; }
	public void decrement() { --value; }
	public int get() { return value; }
	
	public MutableInt(int value) { this.value = value; }
	
	public static <T> void incrementCount(HashMap<T, MutableInt> counter, T key)
	{
		MutableInt count = counter.get(key);
		if (count == null)
			counter.put(key, new MutableInt(1));
		else
			count.increment();
	}
}