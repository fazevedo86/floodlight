/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterNode {
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterNode.class);
	
	private final InetAddress ip;
	private final String nodeId;
	
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
