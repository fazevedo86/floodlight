package pt.ulisboa.tecnico.amorphous.cluster;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterNode {
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterNode.class);
	
	protected final InetAddress ip;
	protected final String nodeId;
	
	public ClusterNode(InetAddress IPAddress, String nodeId) {
		this.ip = IPAddress;
		this.nodeId = nodeId;
	}
	
	public InetAddress getNodeIP(){
		return this.ip;
	}
	
	public String getNodeID(){
		return this.nodeId;
	}

}
