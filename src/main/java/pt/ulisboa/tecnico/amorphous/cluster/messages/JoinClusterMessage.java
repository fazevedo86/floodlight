package pt.ulisboa.tecnico.amorphous.cluster.messages;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.CommunicationProtocol;

public class JoinClusterMessage extends ClusterMessage{
	public JoinClusterMessage(String NodeId){
		super(NodeId);
		this.type = CommunicationProtocol.JOIN_CLUSTER;
	}
}