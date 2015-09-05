/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;

public class AddLink implements IAmorphStateMessage<NetworkLink> {

	public AddLink() {
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
	public Class<? extends IAmorphStateMessage> getMessageType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<NetworkLink> getPayloadType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkLink getPayload() {
		// TODO Auto-generated method stub
		return null;
	}


}
