package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.cluster.IAmorphousCluster;
import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.NewOFSwitchConnection;

public class ClusterService implements IAmorphousCluster {
	protected static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
	private static volatile ClusterService instance = null;
	
	public static final String LOCAL_MCAST_GROUP = "224.0.0.1";
	public static final int MIN_PORT = 1025;
	public static final int MAX_PORT = 65534;

	public final String NodeId;
	protected final ClusterListner listner;
	protected final ClusterCommunicator sender;
	protected ConcurrentMap<InetAddress,ClusterNode> nodes;
	
	public static IAmorphousCluster getInstance(){
		return ClusterService.instance;
	}
	
	public ClusterService() throws InstantiationException {
		throw new InstantiationException("An error occurred while creating an instance of " + ClusterService.class.toString() + ": Please us the a constructor with an apropriate amount of arguments.");
	}
	
	public ClusterService(String NodeId, String mcastGroupIP, int Port) throws UnknownHostException, InstantiationException {
		synchronized(ClusterService.class){
			if(ClusterService.instance == null){
				
				ClusterService.instance = this;
				
				this.listner = new ClusterListner(mcastGroupIP, Port);
				this.sender = new ClusterCommunicator(mcastGroupIP, Port);
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
		// Boot the multicast group listner
		if(!this.isClusterServiceRunning()){
			if( this.listner.startListner() && this.sender.startCommunicator() ){
				this.listner.start();
				this.sender.start();
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean stopClusterService() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isClusterServiceRunning(){
		return this.listner.isListening() && this.sender.isCommunicating();
	}

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
		if(this.nodes.remove(node.getNodeIP()) == null){
			ClusterService.logger.debug("Attempted to remove unregistered node " + node.getNodeID() + "(" + node.getNodeIP() + ")");
			return false;
		}
		
		ClusterService.logger.debug("Node " + node.getNodeID() + "(" + node.getNodeIP() + ") removed!");
		
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

	
	/*** Message handling ***/
	
	@Override
	public void notifyClusterMembers(ClusterMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processClusterMessage(String NodeAddress, ClusterMessage msg) {
				
		// Only process packets that don't come from me
		if(!msg.NodeID.equals(this.NodeId)){
		
			ClusterService.logger.debug("Processing packet from node " + msg.NodeID + "(" + NodeAddress + ")");
			
			ClusterNode origin = null;
			
			try {
				origin = new ClusterNode(NodeAddress, msg.NodeID);
			} catch (UnknownHostException e) {
				ClusterService.logger.error("Failed to instantiate node {NodeAddress=" + NodeAddress + ", NodeId=" + msg.NodeID + "}");
				ClusterService.logger.error(e.getStackTrace().toString());
			}
			
			switch (msg.type) {
				case CommunicationProtocol.JOIN_CLUSTER:
					if(this.isClusterNode(origin)){
						ClusterService.logger.info("Node " + msg.NodeID + "(" + NodeAddress + ") rejoined!");
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
