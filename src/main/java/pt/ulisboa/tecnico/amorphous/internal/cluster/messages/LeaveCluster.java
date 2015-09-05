/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.messages;

import java.util.Map;

public class LeaveCluster implements IAmorphClusterMessage {

	private String NodeId;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5148510938150891159L;

	public LeaveCluster(String NodeId) {
		this.NodeId = NodeId;
	}

	@Override
	public Class<LeaveCluster> getMessageType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.NodeId;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Auto-generated method stub
		return null;
	}

}
