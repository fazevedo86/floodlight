/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4.ClusterCommunicator;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.SyncReq;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddHost;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddLink;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddOFSwitch;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemHost;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemLink;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemOFSwitch;

public class GlobalStateService implements IAmorphGlobalStateService {

	class StateSyncMessage{
		public final Integer messageId;
		public final String queueName;
		@SuppressWarnings("rawtypes")
		public final IAmorphStateMessage message;
		public final SyncType syncType;
		public final IMessageStateListner messageStateListner;
		
		@SuppressWarnings("rawtypes")
		public StateSyncMessage(Integer messageId, String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListner messageStateListner){
			this.messageId = messageId;
			this.queueName = queueName;
			this.message = message;
			this.syncType = syncType;
			this.messageStateListner = messageStateListner;
		}
		
	}
	
	protected static final Logger logger = LoggerFactory.getLogger(GlobalStateService.class);
	private static GlobalStateService instance;
	
	private static final int MSG_HISTORY_SIZE = 100;
	
	// Message queues
	private Map<String,Queue<StateSyncMessage>> messageQueues;
	// Default sync types for the message queues
	private Map<String,SyncType> messageQueueTypes;
	// Message history buffer
	private Map<Integer,StateSyncMessage> messageHistory;
	// Oldest message stored in message history buffer
	private Integer oldestMessage;
	// Global message counter
	private AtomicInteger msgCounter;
	
	public static GlobalStateService getInstance() {
		synchronized (GlobalStateService.class) {
			if(GlobalStateService.instance == null)
			GlobalStateService.instance = new GlobalStateService();
		}
		
		return GlobalStateService.instance;
	}
	
	private GlobalStateService() {
		this.messageQueues = new ConcurrentHashMap<String, Queue<StateSyncMessage>>();
		this.messageQueueTypes = new ConcurrentHashMap<String, SyncType>();
		this.msgCounter = new AtomicInteger(1);
		this.messageHistory = new HashMap<Integer, StateSyncMessage>(GlobalStateService.MSG_HISTORY_SIZE);
		this.oldestMessage = 0;
	}
	
	
	
	@Override
	public void registerSyncQueue(String queueName, SyncType queueDefaultSyncType) throws InvalidAmorphSyncQueueException {
		if(!this.messageQueues.containsKey(queueName)) {
			this.messageQueues.put(queueName, new ConcurrentLinkedQueue<StateSyncMessage>());
			this.messageQueueTypes.put(queueName, queueDefaultSyncType);
		} else {
			throw new InvalidAmorphSyncQueueException("Queue " + queueName + " already exists!");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int queueSyncMessage(String queueName, IAmorphStateMessage message, IMessageStateListner callback) throws InvalidAmorphSyncQueueException {
		return this.queueSyncMessage(queueName, message, this.messageQueueTypes.get(queueName), callback);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public int queueSyncMessage(String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListner callback) throws InvalidAmorphSyncQueueException {
		if(this.messageQueues.containsKey(queueName)){
			StateSyncMessage syncMessage = new StateSyncMessage(this.msgCounter.getAndIncrement(), queueName, message, this.messageQueueTypes.get(queueName), callback);
			this.addToMessageHistory(syncMessage);
			this.messageQueues.get(queueName).add(syncMessage);
			
			return syncMessage.messageId;
		} else {
			throw new InvalidAmorphSyncQueueException(queueName);
		}
	}
	
	@Override
	public void requestFullSync(){
		try {
			// Define the sync source
			ClusterNode syncSource = ClusterService.getInstance().getClusterNodes().iterator().next();
			// Issue sync request
			ClusterCommunicator.getInstance().sendMessage(syncSource, new SyncReq(ClusterService.getInstance().getNodeId()));
			// TODO Should add a security check to the inbound sync message
		} catch (InvalidAmorphClusterMessageException e) {
			GlobalStateService.logger.error(e.getMessage());
		}
	}
	
	@Override
	public void issueFullSync(ClusterNode clusterNode){
		// TODO implement it
	}
	
	@Override
	public void setClusterNodeDown(String nodeId){
		// TODO implement it
	}
	
	@Override
	public void processStateMessage(InetAddress NodeAddress, IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException {
	
		if(msg instanceof IAmorphStateMessage){
			GlobalStateService.logger.debug("Processing message from node " + msg.getOriginatingNodeId() + "(" + NodeAddress + ")");
			
			// Validate Node
			ClusterNode originatingNode = new ClusterNode(NodeAddress, msg.getOriginatingNodeId());
			if(ClusterService.getInstance().isClusterNode(originatingNode)){
				// Dispatch message handling to accordingly method
				try {
					GlobalStateService.class.getMethod("handleMessage" + msg.getMessageType().getSimpleName(), IAmorphClusterMessage.class).invoke(this, msg);
				} catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e){
					GlobalStateService.logger.error(e.getMessage());
				}
			} else {
				GlobalStateService.logger.warn("Received a state sync message (" + msg.getMessageType() + ") from a node that does not belong to the cluster (" + NodeAddress.getHostAddress() + ")");
			}
		} else {
			throw new InvalidAmorphClusterMessageException();
		}
	}
	
	private void addToMessageHistory(StateSyncMessage msg){
		synchronized (this.messageHistory) {
			if(this.messageHistory.size() >= GlobalStateService.MSG_HISTORY_SIZE){
				this.messageHistory.remove(this.oldestMessage);
				this.oldestMessage++;
			}
			
			this.messageHistory.put(msg.messageId,msg);
		}
	}
	
	@SuppressWarnings("unused")
	private void handleMessageAddHost(IAmorphClusterMessage message){
		AddHost msg = (AddHost)message;
		// TODO
	}
	
	private void handleMessageRemHost(IAmorphClusterMessage message){
		RemHost msg = (RemHost)message;
		// TODO
	}
	
	private void handleMessageAddLink(IAmorphClusterMessage message){
		AddLink msg = (AddLink)message;
		// TODO
	}
	
	private void handleMessageRemLink(IAmorphClusterMessage message){
		RemLink msg = (RemLink)message;
		// TODO
	}
	
	private void handleMessageAddOFSwitch(IAmorphClusterMessage message){
		AddOFSwitch msg = (AddOFSwitch)message;
		// TODO
	}
	
	private void handleMessageRemOFSwitch(IAmorphClusterMessage message){
		RemOFSwitch msg = (RemOFSwitch)message;
		// TODO
	}

	private void handleMessageReqSync(IAmorphClusterMessage message){
		SyncReq msg = (SyncReq)message;
		// TODO
	}

	

}
