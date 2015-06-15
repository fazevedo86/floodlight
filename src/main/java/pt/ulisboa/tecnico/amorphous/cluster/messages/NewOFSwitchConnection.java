package pt.ulisboa.tecnico.amorphous.cluster.messages;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.CommunicationProtocol;

public class NewOFSwitchConnection extends ClusterMessage{
	public String OFSwitchID;

	public NewOFSwitchConnection(String NodeId, String OFSwitchID){
		super(NodeId, false);
		this.type = CommunicationProtocol.NEW_OF_CONNECTION;
		this.OFSwitchID = OFSwitchID;
	}
	
	public NewOFSwitchConnection(String NodeId, String OFSwitchID, boolean important){
		super(NodeId, important);
		this.type = CommunicationProtocol.NEW_OF_CONNECTION;
		this.OFSwitchID = OFSwitchID;
	}
}
