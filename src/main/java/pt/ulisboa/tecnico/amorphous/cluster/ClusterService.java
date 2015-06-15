package pt.ulisboa.tecnico.amorphous.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.ClusterCommunicator;
import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.CommunicationProtocol;
import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.JoinClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.LeaveClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.NewOFSwitchConnection;

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
			this.clusterComm.sendMessage(new JoinClusterMessage(ClusterService.getInstance().getNodeId()));
		}
		return false;
	}

	@Override
	public boolean stopClusterService() {
		if(this.isClusterServiceRunning()){
			this.clusterComm.sendMessage(new LeaveClusterMessage(ClusterService.getInstance().getNodeId()));
			this.clusterComm.stopCommunications();
			return true;
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
	public boolean addClusterNode(ClusterNode node) {
		this.nodes.put(node.getNodeIP(), node);
		ClusterService.logger.debug("Node " + node.getNodeID() + "(" + node.getNodeIP() + ") added!");
		
		return this.isClusterNode(node);
	}

	@Override
	public boolean removeClusterNode(ClusterNode node) {
		ClusterNode removedNode = this.nodes.remove(node.getNodeIP());
		if(removedNode == null){
			ClusterService.logger.debug("Attempted to remove unregistered node " + node.getNodeID() + "(" + node.getNodeIP() + ")");
			return false;
		}
		ClusterService.logger.debug("Node " + removedNode.getNodeID() + "(" + removedNode.getNodeIP() + ") removed!");
		return true;
	}

	@Override
	public boolean isClusterNode(ClusterNode node) {
		return this.nodes.containsKey(node.getNodeIP());
	}

	@Override
	public Collection<ClusterNode> getClusterNodes() {
		return Collections.unmodifiableCollection(this.nodes.values());
	}
	
	@Override
	public void syncNode(ClusterNode node){
		// Say hi back
		this.clusterComm.sendMessage(node, new JoinClusterMessage(this.NodeId));
	}

	
	/*** Message handling ***/
	
	@Override
	public void notifyClusterMembers(ClusterMessage msg) {
		this.clusterComm.sendMessage(msg);
	}

	@Override
	public void processClusterMessage(InetAddress NodeAddress, ClusterMessage msg) {
				
		// Only process packets that don't come from me
		if(!msg.NodeID.equals(this.NodeId)){
		
			ClusterService.logger.debug("Processing packet from node " + msg.NodeID + "(" + NodeAddress + ")");
			
			ClusterNode origin = null;
			
			origin = new ClusterNode(NodeAddress, msg.NodeID);
			
			switch (msg.type) {
				case CommunicationProtocol.JOIN_CLUSTER:
					if(this.isClusterNode(origin)){
						ClusterService.logger.info("Node " + NodeAddress + " rejoined!");
						this.removeClusterNode(origin);
					}
						
					this.addClusterNode(origin);
					break;
					
				case CommunicationProtocol.LEAVE_CLUSTER:
					this.removeClusterNode(origin);
					break;
					
				case CommunicationProtocol.NEW_OF_CONNECTION:
					NewOFSwitchConnection newofmsg = (NewOFSwitchConnection)msg;
					// ToDo: Handle new OF switch connection on remote node
					break;
		
				default:
					break;
			}
		}
	}

}
