/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.floodlightcontroller.routing.Link;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.IAmorphTopologyListner;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphousClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4.ClusterCommunicator;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddHost;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddLink;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.AddOFSwitch;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.FullSync;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemHost;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemLink;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.RemOFSwitch;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.SyncReq;
import pt.ulisboa.tecnico.amorphous.types.NetworkHost;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode.NetworkNodeType;

public class GlobalStateService extends Thread implements IAmorphGlobalStateService, IAmorphTopologyListner, Comparable<IAmorphTopologyListner> {

	class StateSyncMessage{
		public final Integer messageId;
		public final String queueName;
		@SuppressWarnings("rawtypes")
		public final IAmorphStateMessage message;
		public final SyncType syncType;
		public SyncMessageState syncState;
		public final IMessageStateListner messageStateListner;
		
		@SuppressWarnings("rawtypes")
		public StateSyncMessage(Integer messageId, String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListner messageStateListner){
			this.messageId = messageId;
			this.queueName = queueName;
			this.message = message;
			this.syncType = syncType;
			this.syncState = SyncMessageState.QUEUED;
			this.messageStateListner = messageStateListner;
		}
		
	}
	
	protected static final Logger logger = LoggerFactory.getLogger(GlobalStateService.class);
	private static GlobalStateService instance;
	
	private static final String STATE_SYNC_QUEUE = "StateSync";
	private static final int MSG_HISTORY_SIZE = 100;
	
	// Topology Manager
	private IAmorphTopologyManagerService amorphTopologyManager = null;
	// Cluster service
	private IAmorphousClusterService amorphClusterService = null;
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
	// Full Sync protection mechanism
	private String syncSourceNodeId = "";
	
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
		
		try{
			this.registerSyncQueue(GlobalStateService.STATE_SYNC_QUEUE, SyncType.BEST_EFFORT);
		} catch(InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.error("Failed to create state sync queue: " + e.getMessage());
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	public void setTopologyManager(IAmorphTopologyManagerService topoMngr){
		if(this.amorphTopologyManager == null)
			this.amorphTopologyManager = topoMngr;
	}
	
	public void setClusterService(IAmorphousClusterService clusterServ){
		if(this.amorphClusterService == null)
			this.amorphClusterService = clusterServ;
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
	
	//------------------------------------------------------------------------
	//							IAmorphGlobalStateService
	//------------------------------------------------------------------------
	@Override
	public void requestFullSync(ClusterNode sourceNode){
		try {
			// Issue sync request
			this.amorphClusterService.getClusterComm().sendMessage(sourceNode, new SyncReq(this.amorphClusterService.getNodeId()));
			GlobalStateService.logger.error("SyncReq sent to node " + sourceNode.getNodeIP().getHostAddress() );
			
			// Add a security check to the inbound sync message
			this.syncSourceNodeId = sourceNode.getNodeID();
			
		} catch (InvalidAmorphClusterMessageException e) {
			GlobalStateService.logger.error("Failed to send SyncReq message to node " + sourceNode.getNodeIP().getHostAddress());
		}
	}
	
	@Override
	public void setClusterNodeDown(String nodeId){
		// Obtain graph snapshot
		FullSync msg = LocalStateService.getInstance().getFullClusterState();
		
		// Network graph cleanup
		for(NetworkNode node : msg.getSwitchAffinityMap().keySet())
			if(msg.getSwitchAffinityMap().get(node).equals(nodeId))
				this.amorphTopologyManager.removeRemoteSwitch(node, nodeId);
	}
	
	@Override
	public void processStateMessage(InetAddress NodeAddress, IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException {
	
		if(msg instanceof IAmorphStateMessage){
			GlobalStateService.logger.debug("Processing message from node " + msg.getOriginatingNodeId() + " (" + NodeAddress.getHostName() + ")");
			
			// Validate Node
			ClusterNode originatingNode = new ClusterNode(NodeAddress, msg.getOriginatingNodeId());
			if(this.amorphClusterService.isClusterNode(originatingNode)){
				// Dispatch message handling to accordingly method
				try {
					GlobalStateService.class.getDeclaredMethod("handleMessage" + msg.getMessageType().getSimpleName(), ClusterNode.class, IAmorphClusterMessage.class).invoke(this, originatingNode, msg);
				} catch(InvocationTargetException ite){
					GlobalStateService.logger.error(ite.getClass().getSimpleName() + ": " + ite.getCause().getClass().getSimpleName() + "");
					ite.getCause().printStackTrace();
				} catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException | SecurityException e){
					GlobalStateService.logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			} else {
				GlobalStateService.logger.warn("Received a state sync message (" + msg.getMessageType() + ") from a node that does not belong to the cluster (" + NodeAddress.getHostAddress() + ")");
			}
		} else {
			throw new InvalidAmorphClusterMessageException();
		}
	}
	//------------------------------------------------------------------------
	
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
	private void handleMessageAddOFSwitch(ClusterNode origin, IAmorphClusterMessage message){
		AddOFSwitch msg = (AddOFSwitch)message;
		GlobalStateService.logger.info("Received a new AddSwitch message from node " + msg.getOriginatingNodeId());
		this.amorphTopologyManager.addRemoteSwitch(msg.getPayload(), message.getOriginatingNodeId());
	}
	
	@SuppressWarnings("unused")
	private void handleMessageRemOFSwitch(ClusterNode origin, IAmorphClusterMessage message){
		RemOFSwitch msg = (RemOFSwitch)message;
		GlobalStateService.logger.info("Received a new RemoveSwitch message from node " + msg.getOriginatingNodeId());
		this.amorphTopologyManager.removeRemoteSwitch(msg.getPayload(), msg.getOriginatingNodeId());
	}
	
	@SuppressWarnings("unused")
	private void handleMessageAddLink(ClusterNode origin, IAmorphClusterMessage message){
		AddLink msg = (AddLink)message;
		GlobalStateService.logger.info("Received a new AddLink message from node " + msg.getOriginatingNodeId());
		this.amorphTopologyManager.addRemoteSwitchLink(msg.getPayload(), msg.getOriginatingNodeId());
	}
	
	@SuppressWarnings("unused")
	private void handleMessageRemLink(ClusterNode origin, IAmorphClusterMessage message){
		RemLink msg = (RemLink)message;
		GlobalStateService.logger.info("Received a new RemoveLink message from node " + msg.getOriginatingNodeId());
		this.amorphTopologyManager.removeRemoteSwitchLink(msg.getPayload(), msg.getOriginatingNodeId());
	}
	
	@SuppressWarnings("unused")
	private void handleMessageAddHost(ClusterNode origin, IAmorphClusterMessage message){
		AddHost msg = (AddHost)message;
		GlobalStateService.logger.info("Received a new AddHost message from node " + message.getOriginatingNodeId());
		this.amorphTopologyManager.addRemoteHost(msg.getPayload(), msg.getAttachmentPoint(), msg.getOriginatingNodeId());
	}
	
	@SuppressWarnings("unused")
	private void handleMessageRemHost(ClusterNode origin, IAmorphClusterMessage message){
		RemHost msg = (RemHost)message;
		GlobalStateService.logger.info("Received a new RemoveHost message from node " + msg.getOriginatingNodeId());
		this.amorphTopologyManager.removeRemoteHost(msg.getPayload(), msg.getOriginatingNodeId());
	}

	@SuppressWarnings("unused")
	private void handleMessageSyncReq(ClusterNode origin, IAmorphClusterMessage message){
		GlobalStateService.logger.info("Received a new SyncReq message from node " + message.getOriginatingNodeId());
		
		FullSync replyMsg = LocalStateService.getInstance().getFullClusterState();
		try {
			this.amorphClusterService.getClusterComm().sendMessage(origin, replyMsg);
		} catch (InvalidAmorphClusterMessageException e) {
			GlobalStateService.logger.error("Failed to send FullSync message: " + e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private void handleMessageFullSync(ClusterNode origin, IAmorphClusterMessage message){
		FullSync msg = (FullSync)message;
		
		if(this.syncSourceNodeId.equals(msg.getOriginatingNodeId())){
			GlobalStateService.logger.info("Received a new FullSync message from node " + message.getOriginatingNodeId());
			LocalStateService.getInstance().setFullClusterState(msg);
			this.syncSourceNodeId = "";
		} else {
			GlobalStateService.logger.info("Discarding unexpected FullSync message from node " + message.getOriginatingNodeId());
		}
	}
	
	//------------------------------------------------------------------------
	//							IAmorphTopologyListner
	//------------------------------------------------------------------------
	@Override
	public void switchAdded(NetworkNode ofswitch) {
		GlobalStateService.logger.info("Queueing a new AddOFSwitch message to the cluster (ofswitch=" + DatapathId.of(ofswitch.getNodeId()) + ")");
		AddOFSwitch msg = new AddOFSwitch(this.amorphClusterService.getNodeId(), ofswitch);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue AddOFSwitch message (ofswitch=" + DatapathId.of(ofswitch.getNodeId()) + "): " + e.getMessage());
		}
	}

	@Override
	public void switchRemoved(NetworkNode ofswitch) {
		GlobalStateService.logger.info("Queueing a new RemOFSwitch message to the cluster (ofswitch=" + DatapathId.of(ofswitch.getNodeId()) + ")");
		RemOFSwitch msg = new RemOFSwitch(this.amorphClusterService.getNodeId(), ofswitch);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue RemOFSwitch message (ofswitch=" + DatapathId.of(ofswitch.getNodeId()) + "): " + e.getMessage());
		}
	}

	@Override
	public void linkAdded(NetworkLink link) {
		GlobalStateService.logger.info("Queueing a new AddLink message to the cluster (s" + link.getNodeA() + "-eth" + link.getNodeAPortNumber() + ":s" + link.getNodeB() + "-eth" + link.getNodeBPortNumber() + ")");
		AddLink msg = new AddLink(this.amorphClusterService.getNodeId(), link);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue AddLink message (s" + link.getNodeA() + "-eth" + link.getNodeAPortNumber() + ":s" + link.getNodeB() + "-eth" + link.getNodeBPortNumber() + "): " + e.getMessage());
		}
	}

	@Override
	public void linkRemoved(NetworkLink link) {
		GlobalStateService.logger.info("Queueing a new RemLink message to the cluster (s" + link.getNodeA() + "-eth" + link.getNodeAPortNumber() + ":s" + link.getNodeB() + "-eth" + link.getNodeBPortNumber() + ")");
		RemLink msg = new RemLink(this.amorphClusterService.getNodeId(), link);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue RemLink message (s" + link.getNodeA() + "-eth" + link.getNodeAPortNumber() + ":s" + link.getNodeB() + "-eth" + link.getNodeBPortNumber() + "): " + e.getMessage());
		}
	}

	@Override
	public void hostAdded(NetworkHost host, NetworkLink attachmentPoint) {
		GlobalStateService.logger.info("Queueing a new AddHost message to the cluster (s" + attachmentPoint.getNodeA() + "-eth" + attachmentPoint.getNodeAPortNumber() + ":h" + attachmentPoint.getNodeB() + "-eth" + attachmentPoint.getNodeBPortNumber() + ")");
		AddHost msg = new AddHost(this.amorphClusterService.getNodeId(), host, attachmentPoint);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue AddHost message (s" + attachmentPoint.getNodeA() + "-eth" + attachmentPoint.getNodeAPortNumber() + ":h" + attachmentPoint.getNodeB() + "-eth" + attachmentPoint.getNodeBPortNumber() + "): " + e.getMessage());
		}
	}

	@Override
	public void hostRemoved(NetworkHost host) {
		GlobalStateService.logger.info("Queueing a new RemHost message to the cluster (h=" + host.getNodeId() + ")");
		RemHost msg = new RemHost(this.amorphClusterService.getNodeId(), host);
		try {
			this.queueSyncMessage(GlobalStateService.STATE_SYNC_QUEUE, msg, null);
		} catch (InvalidAmorphSyncQueueException e) {
			GlobalStateService.logger.info("Unable to queue RemHost message (h=" + host.getNodeId() + "): " + e.getMessage());
		}
	}
	//------------------------------------------------------------------------

	
	//------------------------------------------------------------------------
	//							Thread
	//------------------------------------------------------------------------
	@Override
	public void run() {
		while(this.amorphClusterService.isClusterServiceRunning()){
			for(Queue<StateSyncMessage> q : this.messageQueues.values()){
				Iterator<StateSyncMessage> iterator = q.iterator();
				while(iterator.hasNext()){
					StateSyncMessage msg = iterator.next();
					if(msg.syncState.equals(SyncMessageState.QUEUED)){
						switch(msg.syncType){
							case BEST_EFFORT:
								try {
									this.amorphClusterService.getClusterComm().sendMessage(msg.message);
								} catch (InvalidAmorphClusterMessageException e) {
									GlobalStateService.logger.error("Failed to send message: " + e.getMessage());
								}
								if(msg.messageStateListner != null)
									msg.messageStateListner.onStateUpdate(SyncMessageState.SENT);
								q.remove(msg);
								break;
								
							case GUARANTEED:
								for(ClusterNode node : this.amorphClusterService.getClusterNodes()){
									try {
										this.amorphClusterService.getClusterComm().sendMessage(node, msg.message);
									} catch (InvalidAmorphClusterMessageException e) {
										GlobalStateService.logger.error("Failed to send message: " + e.getMessage());
									}
								}
								if(msg.messageStateListner != null)
									msg.messageStateListner.onStateUpdate(SyncMessageState.SENT);
								q.remove(msg);
								break;
						}
					}
				}
			}
			
			// Sleep it out
			try {
				sleep(500);
			} catch (InterruptedException e) {
				GlobalStateService.logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
	}
	//------------------------------------------------------------------------
	
	@Override
	public int compareTo(IAmorphTopologyListner o){
		return 1;
	}
}
