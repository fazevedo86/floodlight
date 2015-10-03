package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.net.InetAddress;
import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncMessageState;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncType;
import pt.ulisboa.tecnico.amorphous.internal.state.IMessageStateListener;

@SuppressWarnings("rawtypes")
public class MessageContainer implements IAmorphStateMessage<IAmorphStateMessage> {

	private static final long serialVersionUID = 4215237111002614268L;

	public final transient Integer messageId;
	public transient SyncMessageState syncState;
	public final transient IMessageStateListener messageStateListner;
	public transient InetAddress originatingNodeAddress;

	public final String queueName;
	public final IAmorphStateMessage message;
	public final SyncType syncType;
	
	public MessageContainer(Integer messageId, String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListener messageStateListner){
		this.messageId = messageId;
		this.queueName = queueName;
		this.message = message;
		this.syncType = syncType;
		this.syncState = SyncMessageState.QUEUED;
		this.messageStateListner = messageStateListner;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.message.getOriginatingNodeId();
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		return this.message.getVectorClock();
	}

	@Override
	public Class<MessageContainer> getMessageType() {
		return MessageContainer.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class getPayloadType() {
		return message.getMessageType();
	}

	@Override
	public IAmorphStateMessage getPayload() {
		return this.message;
	}

}
