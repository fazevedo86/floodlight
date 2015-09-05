/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal;

import java.net.InetAddress;
import java.util.Collection;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;

public interface IAmorphousClusterService {

	
	/*** Cluster Management ***/

	public boolean startClusterService();
	
	public boolean stopClusterService();
	
	public boolean isClusterServiceRunning();
	
	public String getNodeId();
	
	
	/*** Node Management ***/
	
	public boolean isClusterNode(ClusterNode node);
	
	public Collection<ClusterNode> getClusterNodes();
	
	
	/*** Message handling ***/
	
	public void processClusterMessage(InetAddress NodeAddress, IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException;
	
}
