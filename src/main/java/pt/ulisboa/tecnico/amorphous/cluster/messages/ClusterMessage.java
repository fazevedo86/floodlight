package pt.ulisboa.tecnico.amorphous.cluster.messages;

public class ClusterMessage{
	public boolean important = false;
	public int type = -1;
	public String NodeID;
	
	public ClusterMessage(String NodeId, boolean important){
		this.NodeID = NodeId;
		this.important = important;
	}
}
