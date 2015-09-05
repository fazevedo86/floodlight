/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */


/**
 * 
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.messages;

import java.util.Map;


public class SyncReq implements IAmorphClusterMessage {

	private static final long serialVersionUID = 9196171633646079210L;
	
	private String NodeId;

	public SyncReq(String NodeId) {
		this.NodeId = NodeId;
	}

	@Override
	public Class<SyncReq> getMessageType() {
		return SyncReq.class;
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
