/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;
import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;

public class RemLink implements IAmorphStateMessage {

	public RemLink() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getOriginatingNodeId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getMessageType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getPayloadType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Serializable getPayload() {
		// TODO Auto-generated method stub
		return null;
	}

}
