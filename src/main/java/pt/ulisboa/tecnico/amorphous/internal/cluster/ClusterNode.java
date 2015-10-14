/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster;

import java.io.Serializable;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterNode implements Serializable {

	private static final long serialVersionUID = -7548328281868226699L;

	protected static final Logger logger = LoggerFactory.getLogger(ClusterNode.class);
	
	private final InetAddress ip;
	private final String nodeId;
	private volatile Long lastSeen;
	
	public ClusterNode(InetAddress IPAddress, String nodeId) {
		this.ip = IPAddress;
		this.nodeId = nodeId;
		this.refresh();
	}
	
	public InetAddress getNodeIP(){
		return this.ip;
	}
	
	public String getNodeID(){
		return this.nodeId;
	}
	
	/**
	 * Returns the age in milliseconds since the node was last refreshed
	 * @return
	 */
	public Long getNodeAge(){
		Long age = Long.valueOf(System.currentTimeMillis()) - this.lastSeen;
		return age;
	}
	
	/**
	 * Updates the last seen timestamp to the current time
	 */
	public void refresh(){
		this.lastSeen = Long.valueOf(System.currentTimeMillis());
	}

	@Override
	public boolean equals(Object obj){
		if(obj instanceof ClusterNode){
			ClusterNode target = (ClusterNode)obj;
			return this.ip.equals(target.ip) && this.nodeId.equals(target.nodeId);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return this.nodeId.hashCode() + ip.hashCode();
	}
	
}
