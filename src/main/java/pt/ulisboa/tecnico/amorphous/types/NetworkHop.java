package pt.ulisboa.tecnico.amorphous.types;

import java.io.Serializable;

public class NetworkHop implements Serializable, Comparable<NetworkNode> {

	private static final long serialVersionUID = -897369719368788462L;

	public enum StreamDirection{
		INBOUND,
		OUTBOUND
	}
	
	private final NetworkNode ofswitch;
	private final Integer switchPort;
	private final StreamDirection direction;
	
	public NetworkHop(NetworkNode OFSwitch, Integer SwitchPort, StreamDirection Direction) {
		this.ofswitch = OFSwitch;
		this.switchPort = SwitchPort;
		this.direction = Direction;
	}
	
	public NetworkNode getSwitch(){
		return this.ofswitch;
	}
	
	public Integer getSwitchPort(){
		return this.switchPort;
	}
	
	public StreamDirection getDirection(){
		return this.direction;
	}

	@Override
	public int hashCode(){
		return this.ofswitch.hashCode() + this.switchPort.hashCode() + this.direction.hashCode();
	}
	
	@Override
	public int compareTo(NetworkNode o) {
		return this.hashCode() - o.hashCode();
	}

}
