package pt.ulisboa.tecnico.amorphous.cluster;

import java.net.InetAddress;
import java.util.Collection;

import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;

public interface IAmorphousClusterService {

	
	/*** Cluster Management ***/

	public boolean startClusterService();
	
	public boolean stopClusterService();
	
	public boolean isClusterServiceRunning();
	
	public String getNodeId();
	
	
	/*** Node Management ***/
	
	public boolean addClusterNode(ClusterNode node);
	
	public boolean removeClusterNode(ClusterNode node);
	
	public boolean isClusterNode(ClusterNode node);
	
	public Collection<ClusterNode> getClusterNodes();
	
	public void syncNode(ClusterNode node);
	
	
	/*** Message handling ***/
	
	public void notifyClusterMembers(ClusterMessage msg);
	
	public void processClusterMessage(InetAddress NodeAddress, ClusterMessage msg);
	
}
