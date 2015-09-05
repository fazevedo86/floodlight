package pt.ulisboa.tecnico.amorphous.types;

import java.io.Serializable;

public class NetworkNode implements Serializable, Comparable<NetworkNode> {
	
	private static final long serialVersionUID = 45905291999722011L;

	public enum NodeType{
		GENERIC_DEVICE,
		OFSWITCH
	}

	private final Long nodeId;
	private final NodeType type;
	
	public NetworkNode(Long nodeId, NodeType nodeType) {
		this.nodeId = nodeId;
		this.type = nodeType;
	}
	
	public Long getNodeId(){
		return this.nodeId;
	}
	
	public NodeType getNodeType(){
		return this.type;
	}

	@Override
	public int compareTo(NetworkNode node) {
		if(!this.nodeId.equals(node))
			if(this.nodeId > node.nodeId)
				return 1;
			else
				return -1;
		
		return this.type.compareTo(node.type);
	}

}
