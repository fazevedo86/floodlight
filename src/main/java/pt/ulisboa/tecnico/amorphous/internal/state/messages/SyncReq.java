/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.types.NetworkHost;


public class SyncReq implements IAmorphStateMessage {

	private static final long serialVersionUID = 2677440106876323463L;
	
	public SyncReq() {
	}

	@Override
	public String getOriginatingNodeId() {
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Implement
		return null;
	}

	@Override
	public Class<SyncReq> getMessageType() {
		return SyncReq.class;
	}

	@Override
	public Class getPayloadType() {
		return null;
	}

	@Override
	public SyncReq getPayload() {
		return this;
	}


}
