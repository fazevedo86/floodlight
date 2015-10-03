package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;
import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;

public class AmorphousMessage<T extends Serializable> implements IAmorphStateMessage<T> {

	private static final long serialVersionUID = 7046520996923296150L;
	private final String nodeId;
	protected final T payload;
	
	public AmorphousMessage(T Payload) {
		this.nodeId = ClusterService.getInstance().getNodeId();
		this.payload = Payload;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.nodeId;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO not implemented yet
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<AmorphousMessage> getMessageType() {
		return AmorphousMessage.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class getPayloadType() {
		return this.payload.getClass();
	}

	@Override
	public T getPayload() {
		return this.payload;
	}

}
