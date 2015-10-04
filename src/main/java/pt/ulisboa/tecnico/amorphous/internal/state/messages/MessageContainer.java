package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.net.InetAddress;
import java.util.Map;

import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncMessageState;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncType;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.state.IMessageStateListener;

@SuppressWarnings("rawtypes")
public class MessageContainer implements IAmorphStateMessage<IAmorphStateMessage> {

	private static final long serialVersionUID = 4215237111002614268L;
	
	public final transient Integer messageId;
	public transient SyncMessageState syncState;
	public final transient IMessageStateListener messageStateListner;
	public transient InetAddress originatingNodeAddress;
	public final transient SyncType syncType;
	public final transient ClusterNode receiverNode;

	private final String nodeId;
	public final String queueName;
	public final IAmorphStateMessage message;
	
	public MessageContainer(String NodeId, Integer messageId, String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListener messageStateListner){
		this.nodeId = NodeId;
		this.messageId = messageId;
		this.queueName = queueName;
		this.message = message;
		this.syncType = syncType;
		this.syncState = SyncMessageState.QUEUED;
		this.messageStateListner = messageStateListner;
		this.receiverNode = null;
	}
	
	public MessageContainer(String NodeId, ClusterNode ReceiverNode, Integer messageId, String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListener messageStateListner){
		this.nodeId = NodeId;
		this.messageId = messageId;
		this.queueName = queueName;
		this.message = message;
		this.syncType = syncType;
		this.syncState = SyncMessageState.QUEUED;
		this.messageStateListner = messageStateListner;
		this.receiverNode = ReceiverNode;
	}

	@Override
	public String getOriginatingNodeId() {
		return this.nodeId;
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
	
	public ClusterNode getReceiverNode(){
		return this.receiverNode;
	}

}
