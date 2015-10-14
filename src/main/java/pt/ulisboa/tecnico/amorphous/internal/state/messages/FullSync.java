package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.HashSet;
import java.util.Map;

import org.jgrapht.graph.WeightedMultigraph;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public class FullSync implements IAmorphStateMessage {

	private static final long serialVersionUID = -7930919599325908263L;
	
	private final Map<NetworkNode, String> switchAffinity;
	protected final WeightedMultigraph<NetworkNode, NetworkLink> networkGraph;
	protected final HashSet<ClusterNode> nodes;
//	protected final Map<Class<? extends IFloodlightModule>, Serializable> networkPolicies;
	
//	private final Map<String, Integer> vectorClock;
	
	public FullSync(Map<NetworkNode, String> SwitchAffinity, WeightedMultigraph<NetworkNode, NetworkLink> NetworkGraph, HashSet<ClusterNode> ClusterNodes) {
		this.switchAffinity = SwitchAffinity;
		this.networkGraph = NetworkGraph;
		this.nodes = ClusterNodes;
	}

	@Override
	public String getOriginatingNodeId() {
		return null;
	}

	@Override
	public Map<String,Integer> getVectorClock() {
		return null;
	}

	@Override
	public Class<FullSync> getMessageType() {
		return FullSync.class;
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
	
	public HashSet<ClusterNode> getClusterNodes(){
		return this.nodes;
	}

}
