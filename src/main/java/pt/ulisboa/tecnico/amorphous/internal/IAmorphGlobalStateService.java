/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal;

import java.net.InetAddress;

import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;
import pt.ulisboa.tecnico.amorphous.internal.state.IMessageStateListener;
import pt.ulisboa.tecnico.amorphous.internal.state.ISyncQueueListener;
import pt.ulisboa.tecnico.amorphous.internal.state.InvalidAmorphSyncQueueException;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IAmorphGlobalStateService extends IFloodlightService {
	
	public enum SyncType {
        /**
         * Messages will be committed to each node independently, 
         * guaranteeing message delivery
         */
        GUARANTEED,
        
        /**
         * Messages will be sent to all cluster members, 
         * without any guarantee of delivery
         */
        BEST_EFFORT
    }
	
	public enum SyncMessageState {
		/**
		 * Message waiting in line to be sent
		 */
		QUEUED,
		
		/**
		 * Message sent.
		 */
		SENT,
	}

	/**
	 * Register a new sync message queue.
	 * Each message queue is associated with a sync type describing how
	 * the message will be propagated throughout the cluster.
	 * @param queueName The queue name
	 * @param syncType The default sync type to be used for messages in this queue
	 * @param callback A callback to be executed upon changes to the queue
	 */
	public void registerSyncQueue(String queueName, SyncType queueDefaultSyncType, ISyncQueueListener callback) throws InvalidAmorphSyncQueueException;
	
	/**
	 * Register a new event listener for a given queue
	 * @param queueName The queue name
	 * @param callback A callback to be executed upon changes to the queue
	 */
	public void registerSyncQueueListener(String queueName, ISyncQueueListener callback) throws InvalidAmorphSyncQueueException;

	/**
	 * Unregister an event listener for a given queue
	 * @param queueName The queue name
	 * @param callback A callback to be executed upon changes to the queue
	 */
	public void unregisterSyncQueueListener(String queueName, ISyncQueueListener callback) throws InvalidAmorphSyncQueueException;

	/**
	 * Register a new sync message in a given queue.
	 * Each message is given a unique Id, which can later be used to check on the message.
	 * @param queueName The queue message to which the message will be added to
	 * @param message The sync message to be sent
	 * @param syncType The sync type to be used for this message
	 * @param callback A callback to be executed each time the message status is updated.
	 * @return The unique Id assign to the message
	 */
	@SuppressWarnings("rawtypes")
	public int queueSyncMessage(String queueName, IAmorphStateMessage message, SyncType syncType, IMessageStateListener callback) throws InvalidAmorphSyncQueueException;

	@SuppressWarnings("rawtypes")
	public int queueSyncMessage(String queueName, IAmorphStateMessage message, ClusterNode receiver, IMessageStateListener callback) throws InvalidAmorphSyncQueueException;
	
	/**
	 * Register a new sync message in a given queue.
	 * The message will be dealt with according to the queue settings.
	 * Each message is given a unique Id, which can later be used to check on the message.
	 * @param queueName The queue message to which the message will be added to
	 * @param message The sync message to be sent
	 * @param callback A callback to be executed each time the message status is updated.
	 * @return The unique Id assign to the message
	 */
	@SuppressWarnings("rawtypes")
	public int queueSyncMessage(String queueName, IAmorphStateMessage message, IMessageStateListener callback) throws InvalidAmorphSyncQueueException;
	
	/**
	 * Request for a full database sync from another node in the cluster
	 */
	public void requestFullSync(ClusterNode sourceNode);
	
	/**
	 * Process whichever actions necessaries upon notification of a node leaving the cluster
	 * @param nodeId
	 */
	public void setClusterNodeDown(String nodeId);
	
	/**
	 * Process an incoming state message
	 * @param NodeAddress
	 * @param msg
	 */
	public void processStateMessage(InetAddress NodeAddress, IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException;
	
}
