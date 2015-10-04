package pt.ulisboa.tecnico.amorphous.types;

import java.io.Serializable;

public class NetworkHop implements Serializable, Comparable<NetworkNode> {

	private static final long serialVersionUID = -897369719368788462L;

	private final NetworkNode ofswitch;
	private final Integer inboundSwitchPort;
	private final Integer outboundSwitchPort;
	
	public NetworkHop(NetworkNode OFSwitch, Integer InboundSwitchPort, Integer OutboundSwitchPort) {
		this.ofswitch = OFSwitch;
		this.inboundSwitchPort = InboundSwitchPort;
		this.outboundSwitchPort = OutboundSwitchPort;
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
