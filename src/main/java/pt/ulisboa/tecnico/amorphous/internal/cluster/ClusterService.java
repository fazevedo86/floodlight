/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.internal.IAmorphousClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4.ClusterCommunicator;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.JoinCluster;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.LeaveCluster;
import pt.ulisboa.tecnico.amorphous.internal.state.GlobalStateService;

public class ClusterService implements IAmorphousClusterService {
	protected static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
	private static volatile ClusterService instance = null;

	public final String NodeId;
	public final ClusterCommunicator clusterComm;
	protected ConcurrentMap<InetAddress,ClusterNode> nodes;
	
	public static IAmorphousClusterService getInstance(){
		return ClusterService.instance;
	}
	
	public ClusterService() throws InstantiationException {
		throw new InstantiationException("An error occurred while creating an instance of " + ClusterService.class.toString() + ": Please use a constructor with an apropriate amount of arguments.");
	}
	
	public ClusterService(String NodeId, String mcastGroupIP, int Port) throws UnknownHostException, InstantiationException {
		synchronized(ClusterService.class){
			if(ClusterService.instance == null){
				ClusterService.instance = this;
				this.clusterComm = new ClusterCommunicator(mcastGroupIP, Port);
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + ClusterService.class.toString() + ": An instance already exists.");
			}
		}
		// Initialize the cluster node set
		this.nodes = new ConcurrentHashMap<InetAddress,ClusterNode>();
				
		this.NodeId = NodeId;
	}

	
	/*** Cluster Management ***/

	@Override
	public boolean startClusterService() {
		if(!this.isClusterServiceRunning()){
			this.clusterComm.initCommunications();
			try {
				this.clusterComm.sendMessage(new JoinCluster(this.NodeId));
			} catch (InvalidAmorphClusterMessageException e) {
				ClusterService.logger.error(e.getMessage());
				this.clusterComm.stopCommunications();
				return false;
			}
		}
		return this.isClusterServiceRunning();
	}

	@Override
	public boolean stopClusterService() {
		if(this.isClusterServiceRunning()){
			try {
				this.clusterComm.sendMessage(new LeaveCluster(this.NodeId));
			} catch (InvalidAmorphClusterMessageException e) {
				ClusterService.logger.error(e.getMessage());
				return false;
			}
			return this.clusterComm.stopCommunications();
		}
		return false;
	}
	
	@Override
	public boolean isClusterServiceRunning(){
		return this.clusterComm.isCommunicationActive();
	}

	@Override
	public String getNodeId(){
		return this.NodeId;
	}
	
	
	
	
	/*** Node Management ***/

	@Override
	public boolean isClusterNode(ClusterNode node) {
		return this.nodes.containsKey(node.getNodeIP());
	}

	@Override
	public Collection<ClusterNode> getClusterNodes() {
		return Collections.unmodifiableCollection(this.nodes.values());
	}
	
	private void addClusterNode(ClusterNode node) {
		if(this.isClusterNode(node)){
			ClusterNode existingNode = this.nodes.get(node.getNodeIP());
			if(!existingNode.getNodeID().equals(node.getNodeID())){
				// Different node ID from a previously registered node implies new execution of the controller on said node
				this.removeClusterNode(existingNode);
				GlobalStateService.getInstance().setClusterNodeDown(node.getNodeID());
				this.nodes.put(node.getNodeIP(), node);
			}
		} else {
			this.nodes.put(node.getNodeIP(), node);
		}
			
		ClusterService.logger.debug("Node " + node.getNodeID() + "(" + node.getNodeIP() + ") added!");
	}

	private void removeClusterNode(ClusterNode node) {
		ClusterNode removedNode = this.nodes.remove(node.getNodeIP());
		if(removedNode == null){
			ClusterService.logger.debug("Attempted to remove unregistered node " + node.getNodeID() + "(" + node.getNodeIP() + ")");
		} else {
			GlobalStateService.getInstance().setClusterNodeDown(node.getNodeID());
			ClusterService.logger.debug("Node " + removedNode.getNodeID() + "(" + removedNode.getNodeIP() + ") removed!");
		}
	}

	
	/*** Message handling ***/

	@Override
	public void processClusterMessage(InetAddress NodeAddress, IAmorphClusterMessage msg) {
		ClusterService.logger.debug("Processing message from node " + msg.getOriginatingNodeId() + "(" + NodeAddress + ")");

		// Dispatch message handling to accordingly method
		try {
			ClusterService.class.getMethod("handleMessage" + msg.getMessageType().getSimpleName(), NodeAddress.getClass(), IAmorphClusterMessage.class).invoke(this, NodeAddress, msg);
		} catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e){
			ClusterService.logger.error(e.getMessage());
		}
		
	}
	
	@SuppressWarnings("unused")
	private void handleMessageJoinCluster(InetAddress origin, IAmorphClusterMessage message){
		this.addClusterNode(new ClusterNode(origin, message.getOriginatingNodeId()));
	}

	@SuppressWarnings("unused")
	private void handleMessageLeaveCluster(InetAddress origin, IAmorphClusterMessage message){
		this.removeClusterNode(new ClusterNode(origin, message.getOriginatingNodeId()));
	}
	
	@SuppressWarnings("unused")
	private void handleMessageSyncReq(InetAddress origin, IAmorphClusterMessage message){
		ClusterNode targetNode = new ClusterNode(origin, message.getOriginatingNodeId());
		if(this.isClusterNode(targetNode)){
			GlobalStateService.getInstance().issueFullSync(targetNode);
		}
	}
	
}
