package pt.ulisboa.tecnico.amorphous.types;

import java.io.Serializable;

public class NetworkHop implements Serializable, Comparable<NetworkNode> {

	private static final long serialVersionUID = -897369719368788462L;

	private final NetworkNode ofswitch;
	private final NetworkHost origin;
	private final NetworkHost destination;
	private final Integer inboundSwitchPort;
	private final Integer outboundSwitchPort;
	
	public NetworkHop(NetworkHost SourceHost, NetworkHost Targethost, NetworkNode OFSwitch, Integer InboundSwitchPort, Integer OutboundSwitchPort) {
		this.origin = SourceHost;
		this.destination = Targethost;
		this.ofswitch = OFSwitch;
		this.inboundSwitchPort = InboundSwitchPort;
		this.outboundSwitchPort = OutboundSwitchPort;
	}
	
	public NetworkHost getSourceHost(){
		return this.origin;
	}
	
	public NetworkHost getDestinationHost(){
		return this.destination;
	}
	
	public NetworkNode getSwitch(){
		return this.ofswitch;
	}
	
	public Integer getInboundSwitchPort(){
		return this.inboundSwitchPort;
	}
	
	public Integer getOutboundSwitchPort(){
		return this.outboundSwitchPort;
	}

	@Override
	public int hashCode(){
		return this.ofswitch.hashCode() + this.inboundSwitchPort.hashCode() + this.outboundSwitchPort.hashCode();
	}
	
	@Override
	public int compareTo(NetworkNode o) {
		return this.hashCode() - o.hashCode();
	}

}
