/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public class RemOFSwitch implements IAmorphStateMessage<NetworkNode> {

	private static final long serialVersionUID = 595055927605119847L;
	private final String nodeId;
	protected final NetworkNode ofswitch;
	
	public RemOFSwitch(String NodeId, NetworkNode OFSwitch) {
		this.nodeId = NodeId;
		this.ofswitch = OFSwitch;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.nodeId;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Implement
		return null;
	}

	@Override
	public Class<RemOFSwitch> getMessageType() {
		return RemOFSwitch.class;
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
