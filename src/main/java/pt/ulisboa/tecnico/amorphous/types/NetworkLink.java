package pt.ulisboa.tecnico.amorphous.types;

import org.jgrapht.graph.DefaultWeightedEdge;

public class NetworkLink extends DefaultWeightedEdge implements Comparable<NetworkLink> {

	private static final long serialVersionUID = -1102711318559381681L;

	private final Long nodeA;
	private final Long nodeB;
	private final String nodeAPort;
	private final Integer nodeAPortNumber;
	private final String nodeBPort;
	private final Integer nodeBPortNumber;
	
	private Long bandwidth;
	
	public NetworkLink(Long n1, Long n2, String n1Port, Integer n1PortNumber, String n2Port, Integer n2PortNumber, Long Bandwidth) {
		this.nodeA = n1;
		this.nodeB = n2;
		this.nodeAPort = n1Port;
		this.nodeAPortNumber = n1PortNumber;
		this.nodeBPort = n2Port;
		this.nodeBPortNumber = n2PortNumber;
	}
	
	public Long getNodeA(){
		return this.nodeA;
	}
	
	public Long getNodeB(){
		return this.nodeB;
	}

	public String getNodeAPort(){
		return this.nodeAPort;
	}
	
	public Integer getNodeAPortNumber(){
		return this.nodeAPortNumber;
	}
	
	public String getNodeBPort(){
		return this.nodeBPort;
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
		
		// Compare the first end of the link
        if (!this.nodeA.equals(link.nodeA))
            if(this.nodeA > link.nodeA)
            	return 1;
            else
            	return -1;
        
        // Compare ports on the first end of the link
        if(!this.nodeAPort.equalsIgnoreCase(link.nodeAPort))
        	return this.nodeAPort.compareToIgnoreCase(link.nodeAPort);

        // Compare the second end of the link
        if (!this.nodeB.equals(link.nodeB))
        	if(this.nodeB > link.nodeB)
            	return 1;
            else
            	return -1;

        // Compare ports on the second end of the link
        if(!this.nodeBPort.equalsIgnoreCase(link.nodeBPort))
        	return this.nodeAPort.compareToIgnoreCase(link.nodeAPort);
	
        // Everything is the same, therefore it must be the same link
        return 0;
	}	
}
