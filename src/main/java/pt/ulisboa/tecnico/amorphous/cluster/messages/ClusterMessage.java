package pt.ulisboa.tecnico.amorphous.cluster.messages;

public class ClusterMessage{
	public int type = -1;
	public String NodeID;
	
	public ClusterMessage(String NodeId){
		this.NodeID = NodeId;
	}
}
