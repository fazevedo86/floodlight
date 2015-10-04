/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkHost;

public class RemHost implements IAmorphStateMessage<NetworkHost> {

	private static final long serialVersionUID = 2983083639936201427L;
	protected final NetworkHost host;
	
	public RemHost(NetworkHost Host) {
		this.host = Host;
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
