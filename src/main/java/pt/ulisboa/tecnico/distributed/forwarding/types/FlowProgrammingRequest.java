package pt.ulisboa.tecnico.distributed.forwarding.types;

import java.util.Map;

import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.U64;

import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;
import pt.ulisboa.tecnico.amorphous.types.NetworkHop;
import pt.ulisboa.tecnico.amorphous.types.NetworkHost;

public class FlowProgrammingRequest implements IAmorphStateMessage<NetworkHop> {

	public static final int INFO_NOT_SET = -1;
	
	private static final long serialVersionUID = -4529388828518392204L;

	protected final Long rawCookie;
	protected final NetworkHop hop;
	protected final NetworkHost src;
	protected final NetworkHost dst;
	protected final Integer rawEtherType;
	protected Short ipProto;
	protected Integer srcPort;
	protected Integer dstPort;
	
	public FlowProgrammingRequest(U64 cookie, NetworkHop networkHop, NetworkHost sourceHost, NetworkHost destinationHost, EthType ethernetType) {
		this.rawCookie = cookie.getValue();
		this.hop = networkHop;
		this.src = sourceHost;
		this.dst = destinationHost;
		this.rawEtherType = ethernetType.getValue();
		
		this.ipProto = FlowProgrammingRequest.INFO_NOT_SET;
		this.srcPort = FlowProgrammingRequest.INFO_NOT_SET;
		this.dstPort = FlowProgrammingRequest.INFO_NOT_SET;
	}

	@Override
	public String getOriginatingNodeId() {
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		return null;
	}

	@Override
	public Class<FlowProgrammingRequest> getMessageType() {
		return FlowProgrammingRequest.class;
	}

	@Override
	public Class<NetworkHop> getPayloadType() {
		return NetworkHop.class;
	}

	@Override
	public NetworkHop getPayload() {
		return this.hop;
	}
	
	public void setIPProtocol(Short IPProto){
		this.ipProto = IPProto;
	}
	
	public void setSourceTransportPort(Integer SourceTransportPort){
		if(SourceTransportPort >= 0 && SourceTransportPort <= 65535){
			this.srcPort = SourceTransportPort;
		}
	}
	
	public void setDestinationTransportPort(Integer DestinationTransportPort){
		if(DestinationTransportPort >= 0 && DestinationTransportPort <= 65535){
			this.dstPort = DestinationTransportPort;
		}
	}
	
	public U64 getCookie(){
		return U64.of(this.rawCookie);
	}
	
	public NetworkHop getNetworkHop(){
		return this.hop;
	}
	public NetworkHost getSourceHost(){
		return this.src;
	}
	
	public NetworkHost getDestinationHost(){
		return this.dst;
	}
	
	public EthType getEtherType(){
		return EthType.of(this.rawEtherType);
	}
	
	public Short getIPProtocol(){
		return this.ipProto;
	}
	
	public Integer getSourceTransportPort(){
		return this.srcPort;
	}
	
	public Integer getDestinationTransportPort(){
		return this.dstPort;
	}

}
