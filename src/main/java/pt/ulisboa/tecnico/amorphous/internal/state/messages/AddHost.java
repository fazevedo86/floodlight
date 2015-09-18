/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkHost;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;

public class AddHost implements IAmorphStateMessage<NetworkHost> {

	private static final long serialVersionUID = -7817703665630531213L;
	private final String nodeId;
	protected final NetworkHost host;
	protected final NetworkLink networkLink;
	
	public AddHost(String NodeId, NetworkHost Host, NetworkLink NetLink) {
		this.nodeId = NodeId;
		this.host = Host;
		this.networkLink = NetLink;
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
	public Class<AddHost> getMessageType() {
		return AddHost.class;
	}

	@Override
	public Class<NetworkHost> getPayloadType() {
		return NetworkHost.class;
	}

	@Override
	public NetworkHost getPayload() {
		return this.host;
	}
	
	public NetworkLink getAttachmentPoint(){
		return this.networkLink;
	}

}
