package pt.ulisboa.tecnico.amorphous.cluster.messages;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.CommunicationProtocol;

public class LeaveClusterMessage extends ClusterMessage{
	public LeaveClusterMessage(String NodeId){
		super(NodeId);
		this.type = CommunicationProtocol.LEAVE_CLUSTER;
	}
}
