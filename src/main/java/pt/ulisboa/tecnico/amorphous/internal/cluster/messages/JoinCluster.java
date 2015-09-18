/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.messages;

import java.util.Map;

public class JoinCluster implements IAmorphClusterMessage {

	private static final long serialVersionUID = -652835016013601017L;
	
	private final String NodeId;
	private final Boolean isAdvertisement;

	public JoinCluster(String NodeId, Boolean isAdvertise) {
		this.NodeId = NodeId;
		this.isAdvertisement = isAdvertise;
	}

	@Override
	public Class<JoinCluster> getMessageType() {
		return JoinCluster.class;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.NodeId;
	}

	@Override
	public Map<String,Integer> getVectorClock() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Boolean isAdvertisement(){
		return this.isAdvertisement;
	}

}
