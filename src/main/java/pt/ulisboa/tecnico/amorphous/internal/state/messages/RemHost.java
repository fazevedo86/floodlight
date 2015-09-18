/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkHost;

public class RemHost implements IAmorphStateMessage<NetworkHost> {

	private static final long serialVersionUID = 2983083639936201427L;
	private final String nodeId;
	protected final NetworkHost host;
	
	public RemHost(String NodeId, NetworkHost Host) {
		this.nodeId = NodeId;
		this.host = Host;
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
	public Class<RemHost> getMessageType() {
		return RemHost.class;
	}

	@Override
	public Class<NetworkHost> getPayloadType() {
		return NetworkHost.class;
	}

	@Override
	public NetworkHost getPayload() {
		return this.host;
	}


}
