package pt.ulisboa.tecnico.amorphous.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterNode {
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterNode.class);
	
	protected final InetAddress ip;

	
	public static ClusterNode getNode(String IPAddress) {
		try {
			return new ClusterNode(IPAddress);
		} catch (UnknownHostException e) {
			ClusterNode.logger.error("Failed to instantiate Amorphous ClusterNode: " + e.getMessage() + " | " + e.getStackTrace());
			return null;
		}
	}
	
	public static ClusterNode getNode(InetAddress IPAddress) {
		return new ClusterNode(IPAddress);
	}
	
	
	public ClusterNode(String IPAddress) throws UnknownHostException {
		this.ip = InetAddress.getByName(IPAddress);
	}
	
	public ClusterNode(InetAddress IPAddress) {
		this.ip = IPAddress;
	}
	
	
	public InetAddress getNodeIP(){
		return this.ip;
	}
	
	@Override
	public boolean equals(Object obj){
		return (obj instanceof ClusterListner && ((ClusterNode)obj).getNodeIP().equals(this.ip));
	}

}
