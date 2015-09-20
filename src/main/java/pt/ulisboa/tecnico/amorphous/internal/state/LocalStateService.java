/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

/**
 * This class will act as a container for data collections holding the
 * relevant controller state for the amorphous clustering architecture  
 */

package pt.ulisboa.tecnico.amorphous.internal.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.routing.Link;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.ParanoidGraph;
import org.jgrapht.graph.WeightedMultigraph;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.python.modules.synchronize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.Amorphous;
import pt.ulisboa.tecnico.amorphous.IAmorphTopologyListner;
import pt.ulisboa.tecnico.amorphous.IAmorphTopologyService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService.UpdateSource;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.FullSync;
import pt.ulisboa.tecnico.amorphous.types.NetworkHost;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode.NetworkNodeType;

public class LocalStateService implements IAmorphTopologyService, IAmorphTopologyManagerService {
	
	protected static final Logger logger = LoggerFactory.getLogger(LocalStateService.class);
	
	private static LocalStateService instance;
	
	public static LocalStateService getInstance() {
		synchronized (LocalStateService.class) {
			if(LocalStateService.instance == null)
				LocalStateService.instance = new LocalStateService();
		}
		
		return LocalStateService.instance;
	}
	
	private IOFSwitchService switchService = null;
	
	// Network graph (switches + devices)
	protected volatile WeightedMultigraph<NetworkNode, NetworkLink> networkGraph;
	
	// OFSwitch remote controller affinity
	protected volatile Map<NetworkNode, String> remoteSwitchAffinity;
	
	// Local switch map
	protected volatile Map<NetworkNode, DatapathId> localSwitches;
	protected volatile Map<Long, NetworkHost> localHosts;
	
	
	// Network policies per floodlight module
	protected volatile Map<Class<? extends IFloodlightModule>, Serializable> networkPolicies;
	
	private volatile Set<IAmorphTopologyListner> remoteEventListeners;
	
	private volatile Set<IAmorphTopologyListner> localEventListeners;
	
	/**
	 * Create a new instance
	 */
	public LocalStateService() {
		this.networkGraph = new WeightedMultigraph<NetworkNode, NetworkLink>(NetworkLink.class);
		this.remoteSwitchAffinity = new ConcurrentHashMap<NetworkNode, String>();
		this.localSwitches = new ConcurrentHashMap<NetworkNode, DatapathId>();
		this.localHosts = new ConcurrentHashMap<Long, NetworkHost>();

		this.networkPolicies = new ConcurrentHashMap<Class<? extends IFloodlightModule>, Serializable>();
		this.remoteEventListeners = new ConcurrentSkipListSet<IAmorphTopologyListner>();
		this.localEventListeners = new ConcurrentSkipListSet<IAmorphTopologyListner>();
	}
	
	public void updateSwitchServRef(IOFSwitchService switchService){
		this.switchService = switchService;
	}
	
	
	//------------------------------------------------------------------------
	//							IAmorphTopologyService
	//------------------------------------------------------------------------
	
	@Override
	public boolean isSwitchRegistered(DatapathId OFSwitchId){
		return this.isSwitchRegistered(new NetworkNode(OFSwitchId.getLong(),NetworkNodeType.OFSWITCH));
	}
	
	@Override
	public boolean isSwitchRegistered(NetworkNode node){
		return this.networkGraph.containsVertex(node) && (this.localSwitches.containsKey(node) || this.remoteSwitchAffinity.containsKey(node));
	}
	
	@Override
	public boolean isSwitchLinkRegistered(Link link){
		try{
			return this.isSwitchLinkRegistered(link.getSrc().getLong(), link.getSrcPort().getPortNumber(), link.getDst().getLong(), link.getDstPort().getPortNumber());
		} catch(NullPointerException npe){
			LocalStateService.logger.error("Tried to acceess the SwitchService through a null reference!");
			return false;
		}
	}
	
	@Override
	public boolean isSwitchLinkRegistered(Long srcId, Integer srcPortNumber, Long dstId, Integer dstPortNumber){
		NetworkLink link = new NetworkLink(srcId, srcPortNumber, dstId, dstPortNumber, 0L);
		return this.networkGraph.containsEdge(link);
	}
	
	@Override
	public void addTopologyListner(IAmorphTopologyListner listener, EventSource src){
		switch (src) {
			case LOCAL:
				this.localEventListeners.add(listener);
				break;
			case REMOTE:
				this.remoteEventListeners.add(listener);
			case BOTH:
				this.localEventListeners.add(listener);
				this.remoteEventListeners.add(listener);
		default:
			// You're not supposed to hit this!
			break;
		}
	}

	//------------------------------------------------------------------------
	
	
	//------------------------------------------------------------------------
	//							IAmorphTopologyManagerService
	//------------------------------------------------------------------------
	
	@Override
	public synchronized boolean addLocalSwitch(DatapathId switchId){
		NetworkNode node = new NetworkNode(switchId.getLong(), NetworkNodeType.OFSWITCH);
		
		// Assume that if we get a new local registration then it's best to delete the switch beforehand
		if(this.isSwitchRegistered(node)){
			this.removeSwitch(node);
		}
		
		if(this.networkGraph.addVertex(node)){
			this.localSwitches.put(node, switchId);
			
			// fire event listeners
			for(IAmorphTopologyListner eventListner : this.localEventListeners)
				eventListner.switchAdded(node);
			
			this.printNetworkGraph();
			return true;
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean addRemoteSwitch(NetworkNode node, String AmorphousNodeId){
		// remove the previous associated data beforehand	
		if(this.isSwitchRegistered(node)){
			this.removeSwitch(node);
		}
		
		if(this.networkGraph.addVertex(node)){
			this.remoteSwitchAffinity.put(node, AmorphousNodeId);
			
			// fire event listeners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.switchAdded(node);
			
			this.printNetworkGraph();
			return true;
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean removeLocalSwitch(DatapathId switchId){
		NetworkNode node = new NetworkNode(switchId.getLong(), NetworkNodeType.OFSWITCH);
		if(this.localSwitches.containsKey(node)){
			this.removeSwitchHosts(node);
			this.localSwitches.remove(node);
			this.networkGraph.removeVertex(node);
			
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.localEventListeners)
				eventListner.switchRemoved(node);
			
			this.printNetworkGraph();
			return true;
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean removeRemoteSwitch(NetworkNode node, String AmorphousNodeId){
		if(this.remoteSwitchAffinity.containsKey(node) && this.remoteSwitchAffinity.get(node).equals(AmorphousNodeId)){
			this.removeSwitchHosts(node);
			this.remoteSwitchAffinity.remove(node.getNodeId());
			this.networkGraph.removeVertex(node);
			
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.switchRemoved(node);
			
			this.printNetworkGraph();
			return true;
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean addLocalSwitchLink(Link link){
		boolean success = false;
		NetworkNode src = new NetworkNode(link.getSrc().getLong(),NetworkNodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(link.getDst().getLong(),NetworkNodeType.OFSWITCH);
		NetworkLink lnk = this.networkLinkFromLink(link);
		
		// Check that at least one of the end-point datapaths is controlled locally
		if((this.localSwitches.containsKey(src) && (this.localSwitches.containsKey(dst) || this.remoteSwitchAffinity.containsKey(dst))) || (this.localSwitches.containsKey(dst) && (this.localSwitches.containsKey(src) || this.remoteSwitchAffinity.containsKey(src)))){
			if(!this.networkGraph.containsEdge(lnk)){
				try{
					success = this.networkGraph.addEdge(src, dst, lnk);
					
					// Set weights
					if(lnk.getLinkBandwidth() != 0L)
						this.networkGraph.setEdgeWeight(lnk, (Double)(1/lnk.getLinkBandwidth().doubleValue()) );
					else
						this.networkGraph.setEdgeWeight(lnk, Double.MAX_VALUE);

				} catch(IllegalArgumentException e){
					LocalStateService.logger.error("Unable to add link (s" + lnk.getNodeA() + "-eth" + lnk.getNodeAPortNumber() + ":s" + lnk.getNodeB() + "-eth" + lnk.getNodeBPortNumber() + ") to the topology: " + e.getMessage());
				}
			}
		}
		
		if(success){
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.localEventListeners)
				eventListner.linkAdded(lnk);
		
			this.printNetworkGraph();
		}
		
		return success;
	}
	
	@Override
	public synchronized boolean addRemoteSwitchLink(NetworkLink link, String AmorphousNodeId){
		boolean success = false;
		
		NetworkNode src = new NetworkNode(link.getNodeA(),NetworkNodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(link.getNodeA(),NetworkNodeType.OFSWITCH);
		
		if( !this.isSwitchLinkRegistered(link.getNodeA(), link.getNodeAPortNumber(), link.getNodeB(), link.getNodeBPortNumber()) ){
			// Make sure both nodes belong to the network graph beforehand
			if(!this.networkGraph.containsVertex(src))
				this.networkGraph.addVertex(src);
			if(!this.networkGraph.containsVertex(dst))
				this.networkGraph.addVertex(dst);
			
			// Add the link
			try{
				success = this.networkGraph.addEdge(src, dst, link);
				
				// Set weights
				if(link.getLinkBandwidth() != 0L)
					this.networkGraph.setEdgeWeight(link, (Double)(1/link.getLinkBandwidth().doubleValue()) );
				else
					this.networkGraph.setEdgeWeight(link, Double.MAX_VALUE);
				
			} catch(IllegalArgumentException e){
				LocalStateService.logger.error("Unable to add link (s" + link.getNodeA() + "-eth" + link.getNodeAPortNumber() + ":s" + link.getNodeB() + "-eth" + link.getNodeBPortNumber() + ") to the topology: " + e.getMessage());
			}
		}
		
		if(success){
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.linkAdded(link);
		
			this.printNetworkGraph();
		}
		
		return success;
	}
	
	@Override
	public synchronized boolean removeLocalSwitchLink(Link lnk){
		NetworkNode src = new NetworkNode(lnk.getSrc().getLong(),NetworkNodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(lnk.getDst().getLong(),NetworkNodeType.OFSWITCH);
		boolean success = false;
		
		if(this.localSwitches.containsKey(src) || this.localSwitches.containsKey(dst)){
			NetworkLink link = this.networkLinkFromLink(lnk);
			
			try{
				success = this.networkGraph.removeEdge(link);
			} catch(NullPointerException e){
				// wtf
				Set<NetworkLink> links = new HashSet<NetworkLink>();
				for(int i = this.networkGraph.getAllEdges(src, dst).size(); i > 0 && !success; i--){
					NetworkLink netlink = this.networkGraph.removeEdge(src, dst);
					if(link.equals(netlink))
						success = true;
					else
						links.add(netlink);
					
				}
				for(NetworkLink netlink : links)
					this.networkGraph.addEdge(src, dst, netlink);
				
			}
				if(success){
					// fire event listners
					for(IAmorphTopologyListner eventListner : this.localEventListeners)
						eventListner.linkRemoved(link);
					
					this.printNetworkGraph();
					return true;
				}
			
		}
		
		return success;
	}
	
	@Override
	public synchronized boolean removeRemoteSwitchLink(NetworkLink link, String AmorphousNodeId){
		NetworkNode src = new NetworkNode(link.getNodeA(), NetworkNodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(link.getNodeB(), NetworkNodeType.OFSWITCH);
		
		if((this.remoteSwitchAffinity.containsKey(src) && this.remoteSwitchAffinity.get(src).equals(AmorphousNodeId)) ||
				(this.remoteSwitchAffinity.containsKey(dst) && this.remoteSwitchAffinity.get(dst).equals(AmorphousNodeId))){
		
			if(this.networkGraph.removeEdge(link)){
				// fire event listners
				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
					eventListner.linkRemoved(link);
				
				this.printNetworkGraph();
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean addLocalHost(IDevice Host) {
		boolean success = false;
		
		if(Host.getAttachmentPoints().length == 1){
			NetworkHost host = new NetworkHost(Host.getMACAddress().getLong(), Host.getMACAddressString(), Host.getVlanId()[0].getVlan(), Host.getIPv4Addresses()[0].getInt());
			NetworkNode ofswitch = new NetworkNode(Host.getAttachmentPoints()[0].getSwitchDPID().getLong(), NetworkNodeType.OFSWITCH);
			NetworkLink link = new NetworkLink(ofswitch.getNodeId(), Host.getAttachmentPoints()[0].getPort().getPortNumber(), 
										host.getNodeId(), 0, 
										this.switchService.getSwitch(Host.getAttachmentPoints()[0].getSwitchDPID()).getPort(Host.getAttachmentPoints()[0].getPort()).getCurrSpeed());
			
			// Existence and affinity checks
			if( !this.networkGraph.containsEdge(link) && this.networkGraph.containsVertex(ofswitch) && this.localSwitches.containsKey(ofswitch) ){
				
				if(!this.localHosts.containsKey(Host.getDeviceKey()))
					this.localHosts.put(Host.getDeviceKey(), host);
				
				// Sanity check?
				if(!this.networkGraph.containsVertex(host))
					this.networkGraph.addVertex(host);
				
				// Add the host and link
				success = this.networkGraph.addEdge(ofswitch, host, link);
			}
			
			if(success){
				// fire event listners
				for(IAmorphTopologyListner eventListner : this.localEventListeners){
					eventListner.hostAdded(host, link);
				}
			
				this.printNetworkGraph();
			}
		}
		
		return success;
	}

	@Override
	public synchronized boolean addRemoteHost(NetworkHost host, NetworkLink link, String AmorphousNodeId) {
		NetworkNode ofswitch;
		
		if(link.getNodeA().equals(host.getNodeId()))
			ofswitch = new NetworkNode(link.getNodeB(), NetworkNodeType.OFSWITCH);
		else
			ofswitch = new NetworkNode(link.getNodeA(), NetworkNodeType.OFSWITCH);

		// Existence and affinity checks
		if(!this.networkGraph.containsEdge(link) && this.remoteSwitchAffinity.containsKey(ofswitch) && this.remoteSwitchAffinity.get(ofswitch).equals(AmorphousNodeId)){

			// Sanity check
			if(this.networkGraph.containsVertex(host))
				this.networkGraph.removeVertex(host);
			
			this.networkGraph.addVertex(host);
	
			if(this.networkGraph.addEdge(ofswitch, host, link)){
				// fire event listeners
				for(IAmorphTopologyListner eventListner : this.remoteEventListeners){
					eventListner.hostAdded(host, link);
				}
			
				this.printNetworkGraph();
				
				return true;
			}
		}
		
		// Fail by default
		return false;
	}

	@Override
	public synchronized boolean updateLocalHost(IDevice Host){
		if(Host.getAttachmentPoints().length == 0)
			return this.removeLocalHost(Host);
		
		return false;
	}
	
	@Override
	public synchronized boolean removeLocalHost(IDevice Host){
		// Existence and Affinity checks
		if(this.localHosts.containsKey(Host.getDeviceKey())){
			NetworkHost host = this.localHosts.get(Host.getDeviceKey());
			
			if(this.networkGraph.containsVertex(host)){
				if(this.networkGraph.removeVertex(host)){
					this.localHosts.remove(Host.getDeviceKey());
					
					// Fire event listeners
					for(IAmorphTopologyListner eventListner : this.localEventListeners){
						eventListner.hostRemoved(host);
					}
					
					this.printNetworkGraph();
					
					return true;
				}
			}
		}
		
		// Fail by default
		return false;

	}

	@Override
	public synchronized boolean removeRemoteHost(NetworkHost host, String AmorphousNodeId){
		if(this.networkGraph.containsVertex(host) && this.networkGraph.edgesOf(host).size() == 1){
			NetworkLink link = ((NetworkLink)this.networkGraph.edgesOf(host).toArray()[0]);
			NetworkNode ofswitch = new NetworkNode(link.getNodeA(), NetworkNodeType.OFSWITCH);
			
			// Figure out which is node is the switch
			if(!this.networkGraph.containsVertex(ofswitch))
				ofswitch = new NetworkNode(link.getNodeB(), NetworkNodeType.OFSWITCH);
			
			// Affinity checks
			if( this.networkGraph.containsVertex(ofswitch) && (this.remoteSwitchAffinity.containsKey(ofswitch) && this.remoteSwitchAffinity.get(ofswitch).equals(AmorphousNodeId)) ){
				this.networkGraph.removeVertex(host);
				
				// Fire event listeners
				for(IAmorphTopologyListner eventListner : this.localEventListeners){
					eventListner.hostRemoved(host);
				}
				
				this.printNetworkGraph();
				
				return true;
			}
		}
		
		// Fail by default
		return false;
	}
	
	//------------------------------------------------------------------------

	public synchronized FullSync getFullClusterState(){
		String localNodeID = ClusterService.getInstance().getNodeId();
		WeightedMultigraph<NetworkNode, NetworkLink> NetworkGraph = new WeightedMultigraph<NetworkNode, NetworkLink>(NetworkLink.class);
		Map<NetworkNode, String> switchAffinity = new ConcurrentHashMap<NetworkNode, String>(this.remoteSwitchAffinity);
		
		// add local switches with local node id
		for(NetworkNode ofswitch : this.localSwitches.keySet()){
			switchAffinity.put(ofswitch, localNodeID);
		}
		
		// Add nodes to the graph
		for(NetworkNode node : this.networkGraph.vertexSet())
			NetworkGraph.addVertex(node);
		
		// Add network links to the graph
		for(NetworkLink link : this.networkGraph.edgeSet()){
			NetworkGraph.addEdge(this.networkGraph.getEdgeSource(link), this.networkGraph.getEdgeTarget(link), link);
			NetworkGraph.setEdgeWeight(link, this.networkGraph.getEdgeWeight(link));
		}

		return new FullSync(ClusterService.getInstance().getNodeId(), switchAffinity, NetworkGraph);		
	}
	
	public synchronized void setFullClusterState(FullSync fullSync){
		LocalStateService.logger.info("Processing FullSync from node " + fullSync.getOriginatingNodeId());
		String myID = ClusterService.getInstance().getNodeId();
		WeightedMultigraph<NetworkNode, NetworkLink> newGraph = fullSync.getNetworkGraph();
		Map<NetworkNode, String> SwitchAffinity = fullSync.getSwitchAffinityMap();
		
		// Merge reliable data
		if(!this.networkGraph.edgeSet().isEmpty()){
			// Update the data structures with local nodes
			for(NetworkNode ofswitch : this.localSwitches.keySet()){
				// Cleanup
				if(newGraph.containsVertex(ofswitch)){
					SwitchAffinity.remove(ofswitch);
					newGraph.removeVertex(ofswitch);
				}
				// Register locally managed nodes
				newGraph.addVertex(ofswitch);
				SwitchAffinity.put(ofswitch, myID);
			}
			
			// Update the network graph with links for local nodes
			for(NetworkNode ofswitch : this.localSwitches.keySet()){
				for(NetworkLink link : this.networkGraph.edgesOf(ofswitch)){
					NetworkNode endpoint = this.networkGraph.getEdgeSource(link);
					if(ofswitch.equals(endpoint))
						endpoint = this.networkGraph.getEdgeTarget(link);
					
					// Register ONLY local switches and Hosts
					if( endpoint.getNodeType().equals(NetworkNodeType.GENERIC_DEVICE) || this.localSwitches.containsKey(endpoint) ){
						if(!newGraph.containsVertex(endpoint))
							newGraph.addVertex(endpoint);
						newGraph.addEdge(ofswitch, endpoint, link);
						newGraph.setEdgeWeight(link, this.networkGraph.getEdgeWeight(link));
					}
				}
			}
		}
		
		// Replace state data structures
		this.networkGraph = newGraph;
		this.remoteSwitchAffinity = SwitchAffinity;
		
		LocalStateService.logger.info("Local data structures imported from node " + fullSync.getOriginatingNodeId());
	}

	public void printNetworkGraph(){
		StringBuilder affinity = new StringBuilder("[AMORPHOUS] OFSwitch controller affinity:");
		System.out.println("\n");
		System.out.println("[AMORPHOUS] Network topology:" );

		for(NetworkNode node : this.networkGraph.vertexSet()){
			if(node.getNodeType().equals(NetworkNodeType.OFSWITCH)){
				StringBuilder connections = new StringBuilder("s" + node.getNodeId().toString());
				for(NetworkLink link : this.networkGraph.edgesOf(node)){
					
					// Source side
					NetworkNode tmpNode = this.networkGraph.getEdgeSource(link);
					if(tmpNode.equals(node)){
						connections.append(" s");
					} else {
						if(tmpNode.getNodeType().equals(NetworkNodeType.OFSWITCH)){
							connections.append(" s");
						} else {
							connections.append(" h");
						}	
					}
					connections.append(link.getNodeA()).append("-eth").append(link.getNodeAPortNumber()).append(":");
					
					// Target side
					tmpNode = this.networkGraph.getEdgeTarget(link);
					if(tmpNode.getNodeType().equals(NetworkNodeType.OFSWITCH)){
						connections.append("s");
					} else {
						connections.append("h");
					}
					connections.append(link.getNodeB()).append("-eth").append(link.getNodeBPortNumber());
				}
				System.out.println(connections);
			}
		}
		for(NetworkNode node : this.localSwitches.keySet())
			affinity.append("\ns").append(node.getNodeId()).append(" -> LOCAL");
		for(NetworkNode node : this.remoteSwitchAffinity.keySet())
			affinity.append("\ns").append(node.getNodeId()).append(" -> ").append(this.remoteSwitchAffinity.get(node));
			
		System.out.println();
		
		System.out.println(affinity);
		
		System.out.println("\n");
	}
	
	private void removeSwitch(NetworkNode node){
		this.removeLocalSwitch(DatapathId.of(node.getNodeId()));
		this.removeRemoteSwitch(node, this.remoteSwitchAffinity.get(node));
	}
	
	private void removeSwitchHosts(NetworkNode node){
		Set<NetworkLink> connectedNodes = this.networkGraph.edgesOf(node);
		
		for(NetworkLink edge : connectedNodes){
			NetworkNode peer = (this.networkGraph.getEdgeSource(edge).compareTo(node) == 0 ? this.networkGraph.getEdgeSource(edge) : this.networkGraph.getEdgeTarget(edge));
			if(peer.getNodeType().equals(NetworkNodeType.GENERIC_DEVICE)){
				if(this.networkGraph.getAllEdges(node, peer).size() == this.networkGraph.degreeOf(peer))
					this.networkGraph.removeVertex(peer);
			} else if(peer.getNodeType().equals(NetworkNodeType.OFSWITCH)){
				if(this.networkGraph.degreeOf(peer) == 0 && !this.localSwitches.containsKey(peer) && !this.remoteSwitchAffinity.containsKey(peer))
					this.networkGraph.removeVertex(peer);
			}
		}
	}
	
	public NetworkLink networkLinkFromLink(Link link){
		long linkBandwidth = 0L;
		IOFSwitch localSwitch = this.switchService.getSwitch(link.getSrc());

		// Determine bandwidth from the the locally controlled switch
		if(localSwitch == null) {
			localSwitch = this.switchService.getSwitch(link.getDst());
			if(localSwitch != null)
				linkBandwidth = localSwitch.getPort(link.getDstPort()).getCurrSpeed();
		} else {
			linkBandwidth = localSwitch.getPort(link.getSrcPort()).getCurrSpeed();
		}
		
		return new NetworkLink(link.getSrc().getLong(), link.getSrcPort().getPortNumber(),
				link.getDst().getLong(), link.getDstPort().getPortNumber(),
				linkBandwidth);
	}
	
	private Link linkFromNetworkLink(NetworkLink link){
		return new Link(DatapathId.of(link.getNodeA()), OFPort.of(link.getNodeAPortNumber()), 
				DatapathId.of(link.getNodeB()), OFPort.of(link.getNodeBPortNumber()));
	}

}
