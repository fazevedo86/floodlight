package pt.ulisboa.tecnico.amorphous.types;

import java.io.Serializable;

public class NetworkNode implements Serializable, Comparable<NetworkNode> {
	
	private static final long serialVersionUID = 45905291999722011L;

	public enum NetworkNodeType{
		GENERIC_DEVICE,
		OFSWITCH
	}

	private final Long nodeId;
	private final NetworkNodeType type;
	
	public NetworkNode(Long nodeId, NetworkNodeType nodeType) {
		this.nodeId = nodeId;
		this.type = nodeType;
	}
	
	public Long getNodeId(){
		return this.nodeId;
	}
	
	public NetworkNodeType getNodeType(){
		return this.type;
	}

	@Override
	public int compareTo(NetworkNode node) {
		if(!this.nodeId.equals(node))
			if(this.nodeId > node.nodeId)
				return 1;
			else
				return -1;
		
		return (this.type.equals(node.type) ? 0 : this.type.compareTo(node.type));
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof NetworkNode){
			NetworkNode target = (NetworkNode)obj;
			return this.nodeId.equals(target.nodeId) && this.type.equals(target.type);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return (this.nodeId.intValue() / (this.type.ordinal() + 1)) * this.nodeId.intValue();
	}

}
