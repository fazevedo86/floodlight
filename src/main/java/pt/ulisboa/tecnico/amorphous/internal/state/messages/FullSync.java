package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.routing.Link;

import org.jgrapht.graph.UnmodifiableGraph;
import org.jgrapht.graph.WeightedMultigraph;
import org.projectfloodlight.openflow.types.DatapathId;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public class FullSync implements IAmorphStateMessage {

	private static final long serialVersionUID = -7930919599325908263L;
	
	private final String NodeId;
	private final Map<NetworkNode, String> switchAffinity;
	protected final WeightedMultigraph<NetworkNode, NetworkLink> networkGraph;
//	protected final Map<Class<? extends IFloodlightModule>, Serializable> networkPolicies;
	
//	private final Map<String, Integer> vectorClock;
	
	public FullSync(String NodeId, Map<NetworkNode, String> SwitchAffinity, WeightedMultigraph<NetworkNode, NetworkLink> NetworkGraph) {
		this.NodeId = NodeId;
		this.switchAffinity = SwitchAffinity;
		this.networkGraph = NetworkGraph;		
	}

	@Override
	public String getOriginatingNodeId() {
		return this.NodeId;
	}

	@Override
	public Map<String,Integer> getVectorClock() {
		return null;
	}

	@Override
	public Class getMessageType() {
		return null;
	}

	@Override
	public Class getPayloadType() {
		return null;
	}

	@Override
	public FullSync getPayload() {
		return this;
	}
	
	public Map<NetworkNode, String> getSwitchAffinityMap(){
		return this.switchAffinity;
	}
	
	public WeightedMultigraph<NetworkNode, NetworkLink> getNetworkGraph(){
		return this.networkGraph;
	}

}
