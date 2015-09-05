package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.routing.Link;

import org.jgrapht.graph.UnmodifiableGraph;
import org.projectfloodlight.openflow.types.DatapathId;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;

public class FullSync implements IAmorphStateMessage<HashMap<Class<? extends Serializable>, Serializable>> {

	private static final long serialVersionUID = -7930919599325908263L;
	
	public static final Class<? extends Serializable> NETWORK_GRAPH_KEY = UnmodifiableGraph.class;
	public static final Class<? extends Serializable> HOSTS_KEY = HashMap.class;
	public static final Class<? extends Serializable> SWITCHES_KEY = HashMap.class;
	public static final Class<? extends Serializable> POLICIES_KEY = HashMap.class;
	
	private final String NodeId;
	private final Map<String, Integer> vectorClock;
	public final HashMap<Class<? extends Serializable>, Serializable> payload;
	
	public FullSync(String NodeId, Map<String, Integer> VectorClock, UnmodifiableGraph<DatapathId, Link> NetworkGraph, HashMap<DatapathId,Set<IDevice>> Hosts, HashMap<DatapathId, ClusterNode> Switches, HashMap<String, Object> NetworkPolicies) {
		this.NodeId = NodeId;
		this.payload = new HashMap<Class<? extends Serializable>, Serializable>();
		this.payload.put(NETWORK_GRAPH_KEY, NetworkGraph);
		this.payload.put(HOSTS_KEY, Hosts);
		this.payload.put(SWITCHES_KEY, Switches);
		this.payload.put(POLICIES_KEY, NetworkPolicies);
		
		this.vectorClock = VectorClock;
//		this.networkGraph = NetworkGraph;
//		this.hosts = Hosts;
//		this.switches = Switches;
//		this.networkPolicy = NetworkPolicy;
	}

	@Override
	public Class<FullSync> getMessageType() {
		return FullSync.class;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.NodeId;
	}

	@Override
	public Map<String,Integer> getVectorClock() {
		return this.vectorClock;
	}

	@Override
	public Class<HashMap<Class<? extends Serializable>, Serializable>> getPayloadType() {
		return null;
	}

	@Override
	public HashMap<Class<? extends Serializable>, Serializable> getPayload() {
		return null;
	}

}
