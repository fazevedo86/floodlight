package pt.ulisboa.tecnico.amorphous.cluster.messages;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.CommunicationProtocol;

public class JoinClusterMessage extends ClusterMessage{
	public JoinClusterMessage(String NodeId){
		super(NodeId, true);
		this.type = CommunicationProtocol.JOIN_CLUSTER;
	}
	
	public JoinClusterMessage(String NodeId, boolean important){
		super(NodeId, important);
		this.type = CommunicationProtocol.JOIN_CLUSTER;
	}
}