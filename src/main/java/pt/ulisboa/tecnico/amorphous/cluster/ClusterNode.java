package pt.ulisboa.tecnico.amorphous.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.ClusterListner;

public class ClusterNode {
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterNode.class);
	
	protected final InetAddress ip;
	protected final String nodeId;

	
	public static ClusterNode createNode(String IPAddress, String nodeId) {
		try {
			return new ClusterNode(IPAddress, nodeId);
		} catch (UnknownHostException e) {
			ClusterNode.logger.error("Failed to instantiate Amorphous ClusterNode: " + e.getMessage() + " | " + e.getStackTrace());
			return null;
		}
	}
	
	public static ClusterNode createNode(InetAddress IPAddress, String nodeId) {
		return new ClusterNode(IPAddress, nodeId);
	}
	
	
	public ClusterNode(String IPAddress, String nodeId) throws UnknownHostException {
		this.ip = InetAddress.getByName(IPAddress);
		this.nodeId = nodeId;
	}
	
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
	
	@Override
	public boolean equals(Object obj){
		return (obj instanceof ClusterListner && ((ClusterNode)obj).getNodeIP().equals(this.ip));
	}

}
