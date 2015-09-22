package pt.ulisboa.tecnico.amorphous.types;

import org.jgrapht.graph.DefaultWeightedEdge;

public class NetworkLink extends DefaultWeightedEdge implements Comparable<NetworkLink> {

	private static final long serialVersionUID = -1102711318559381681L;

	private final Long nodeA;
	private final Long nodeB;
	private final Integer nodeAPortNumber;
	private final Integer nodeBPortNumber;
	
	private Long bandwidth;
	
	public NetworkLink(Long node1, Integer node1PortNumber, Long node2, Integer node2PortNumber, Long Bandwidth) {
		this.nodeA = node1;
		this.nodeB = node2;
		this.nodeAPortNumber = node1PortNumber;
		this.nodeBPortNumber = node2PortNumber;
		this.bandwidth = Bandwidth;
	}
	
	public Long getNodeA(){
		return this.nodeA;
	}
	
	public Long getNodeB(){
		return this.nodeB;
	}

	public Integer getNodeAPortNumber(){
		return this.nodeAPortNumber;
	}
	
	public Integer getNodeBPortNumber(){
		return this.nodeBPortNumber;
	}
	
	/**
	 * Gets the bandwidth in bits/second
	 * @return
	 */
	public Long getLinkBandwidth(){
		return this.bandwidth;
	}
	
	/**
	 * Update the link bandwidth in bits/second
	 * @param bandwidth
	 */
	public void setLinkBandwidth(Long bandwidth){
		// Bandwidth must always be greater than 0
		if(bandwidth > 0){
			this.bandwidth = bandwidth;
		}
	}
	
	@Override
	public int compareTo(NetworkLink link){
		if(this.equals(link)){
			return 0;
		} else {
			// Compare the first end of the link
	        if (!this.nodeA.equals(link.nodeA))
	            if(this.nodeA > link.nodeA)
	            	return 1;
	            else
	            	return -1;
	        
	        // Compare ports on the first end of the link
	        if(!this.nodeAPortNumber.equals(link.nodeAPortNumber))
	        	return this.nodeAPortNumber.compareTo(link.nodeAPortNumber);

	        // Compare the second end of the link
	        if (!this.nodeB.equals(link.nodeB))
	        	if(this.nodeB > link.nodeB)
	            	return 1;
	            else
	            	return -1;

	        // Compare ports on the second end of the link
	        if(!this.nodeBPortNumber.equals(link.nodeBPortNumber))
	        	return this.nodeBPortNumber.compareTo(link.nodeBPortNumber);
	        
	        // Default return value for different links
	        return -1;
		}
		
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof NetworkLink){
			NetworkLink target = (NetworkLink)obj;
			boolean clone = ( this.nodeA.equals(target.nodeA) && this.nodeAPortNumber.equals(target.nodeAPortNumber) && this.nodeB.equals(target.nodeB) && this.nodeBPortNumber.equals(target.nodeBPortNumber) );
			boolean reverse = ( this.nodeA.equals(target.nodeB) && this.nodeAPortNumber.equals(target.nodeBPortNumber) && this.nodeB.equals(target.nodeA) && this.nodeBPortNumber.equals(target.nodeAPortNumber) );
			
			return (clone || reverse);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return ((Long)(this.nodeA + this.nodeB)).intValue() / ((this.nodeAPortNumber * this.nodeBPortNumber) + 1); 
	}
}
