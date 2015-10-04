/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public class AddOFSwitch implements IAmorphStateMessage<NetworkNode> {

	private static final long serialVersionUID = -7124212847523105697L;
	protected final NetworkNode ofswitch;
	
	public AddOFSwitch(NetworkNode OFSwitch) {
		this.ofswitch = OFSwitch;
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
	public Class<AddOFSwitch> getMessageType() {
		return AddOFSwitch.class;
	}

	@Override
	public Class<NetworkNode> getPayloadType() {
		return NetworkNode.class;
	}

	@Override
	public NetworkNode getPayload() {
		return this.ofswitch;
	}


}
