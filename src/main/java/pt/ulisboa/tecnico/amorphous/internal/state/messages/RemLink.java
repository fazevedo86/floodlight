/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.util.Map;

import pt.ulisboa.tecnico.amorphous.types.NetworkLink;

public class RemLink implements IAmorphStateMessage<NetworkLink> {

	private static final long serialVersionUID = -1952719479509668319L;
	protected final NetworkLink networkLink;
	
	public RemLink(NetworkLink NetLink) {
		this.networkLink = NetLink;
	}

	@Override
	public String getOriginatingNodeId() {
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<RemLink> getMessageType() {
		return RemLink.class;
	}

	@Override
	public Class<NetworkLink> getPayloadType() {
		return NetworkLink.class;
	}

	@Override
	public NetworkLink getPayload() {
		return this.networkLink;
	}

}
