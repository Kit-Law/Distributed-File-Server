import java.io.Serializable;

public class MetaData implements Serializable
{
	private State state;
	private int size;
	private int[] dstorePorts;
	
	public MetaData(State state, int size, int[] dstorePorts)
	{
		this.state = state;
		this.size = size;
		this.dstorePorts = dstorePorts;
	}
	
	public String parseState() { return state.name().replace('_', ' ').toLowerCase(); }
	
	public State getState() { return state; }
	public int getSize() { return size; }
	public int[] getDstorePorts() { return dstorePorts; }
}
