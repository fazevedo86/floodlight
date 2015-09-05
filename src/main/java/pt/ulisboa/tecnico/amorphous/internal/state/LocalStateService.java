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

import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.routing.Link;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.WeightedMultigraph;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.Amorphous;
import pt.ulisboa.tecnico.amorphous.IAmorphTopologyListner;
import pt.ulisboa.tecnico.amorphous.IAmorphTopologyService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService.UpdateSource;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode.NodeType;

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

		this.networkPolicies = new ConcurrentHashMap<Class<? extends IFloodlightModule>, Serializable>();
		this.remoteEventListeners = new ConcurrentSkipListSet<IAmorphTopologyListner>();
		this.localEventListeners = new ConcurrentSkipListSet<IAmorphTopologyListner>();
	}
	
	public void updateSwitchServRef(IOFSwitchService switchService){
		this.switchService = switchService;
	}
	
	@Override
	public boolean isSwitchRegistered(DatapathId OFSwitchId){
		return this.isSwitchRegistered(new NetworkNode(OFSwitchId.getLong(),NodeType.OFSWITCH));
	}
	
	@Override
	public boolean isSwitchRegistered(NetworkNode node){
		return this.networkGraph.containsVertex(node) && (this.localSwitches.containsKey(node) || this.remoteSwitchAffinity.containsKey(node));
	}
	
	@Override
	public boolean isSwitchLinkRegistered(Link link){
		try{
			return this.isSwitchLinkRegistered(link.getSrc().getLong(), this.switchService.getSwitch(link.getSrc()).getPort(link.getSrcPort()).getName(), 
				link.getDst().getLong(), this.switchService.getSwitch(link.getDst()).getPort(link.getDstPort()).getName());
		} catch(NullPointerException npe){
			LocalStateService.logger.error("Tried to acceess the SwitchService through a null reference!");
			return false;
		}
	}
	
	@Override
	public boolean isSwitchLinkRegistered(Long srcId, String srcPortName, Long dstId, String dstPortName){
		NetworkLink link = new NetworkLink(srcId, dstId, srcPortName, 0, dstPortName, 0, 0L);
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

	
	
	
	@Override
	public boolean addLocalSwitch(DatapathId switchId){
		NetworkNode node = new NetworkNode(switchId.getLong(), NodeType.OFSWITCH);
		
		// Assume that if we get a new local registration then it's best to delete the switch beforehand
		if(this.isSwitchRegistered(node)){
			this.removeSwitch(node);
		}
		
		if(this.networkGraph.addVertex(node)){
			this.localSwitches.put(node, switchId);
			
			// fire event listeners
			for(IAmorphTopologyListner eventListner : this.localEventListeners)
				eventListner.switchAdded(switchId);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean addRemoteSwitch(NetworkNode node, String AmorphousNodeId){
		// remove the previous associated data beforehand	
		if(this.isSwitchRegistered(node)){
			this.removeSwitch(node);
		}
		
		if(this.networkGraph.addVertex(node)){
			this.remoteSwitchAffinity.put(node, AmorphousNodeId);
			
			// fire event listeners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.switchAdded(DatapathId.of(node.getNodeId()));
			
			return true;
		}
		
		return false;
	}
	
	
	@Override
	public boolean removeLocalSwitch(DatapathId switchId){
		NetworkNode node = new NetworkNode(switchId.getLong(), NodeType.OFSWITCH);
		if(this.localSwitches.containsKey(node)){
			this.removeSwitchHosts(node);
			this.localSwitches.remove(switchId);
			this.networkGraph.removeVertex(node);
			
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.localEventListeners)
				eventListner.switchRemoved(switchId);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean removeRemoteSwitch(NetworkNode node, String AmorphousNodeId){
		if(this.remoteSwitchAffinity.containsKey(node) && this.remoteSwitchAffinity.get(node).equals(AmorphousNodeId)){
			this.removeSwitchHosts(node);
			this.remoteSwitchAffinity.remove(node.getNodeId());
			this.networkGraph.removeVertex(node);
			
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.switchRemoved(DatapathId.of(node.getNodeId()));
			
			return true;
		}
		
		return false;
	}
	
	private void removeSwitch(NetworkNode node){
		this.removeLocalSwitch(DatapathId.of(node.getNodeId()));
		this.removeRemoteSwitch(node, this.remoteSwitchAffinity.get(node));
	}
	
	private void removeSwitchHosts(NetworkNode node){
		Set<NetworkLink> connectedNodes = new HashSet<NetworkLink>(this.networkGraph.degreeOf(node));
		connectedNodes.addAll(this.networkGraph.incomingEdgesOf(node));
		connectedNodes.addAll(this.networkGraph.outgoingEdgesOf(node));
		
		for(NetworkLink edge : connectedNodes){
			NetworkNode peer  = (this.networkGraph.getEdgeSource(edge).compareTo(node) == 0 ? this.networkGraph.getEdgeSource(edge) : this.networkGraph.getEdgeTarget(edge));
			if(peer.getNodeType().equals(NodeType.GENERIC_DEVICE)){
				if(this.networkGraph.getAllEdges(node, peer).size() == this.networkGraph.degreeOf(peer))
					this.networkGraph.removeVertex(peer);
			}
		}
	}
	
	
	@Override
	public boolean addLocalSwitchLink(Link link){
		boolean success = false;
		
		if( !this.isSwitchLinkRegistered(link) && (this.isSwitchRegistered(link.getSrc()) || this.isSwitchRegistered(link.getDst())) ){
			NetworkNode src = new NetworkNode(link.getSrc().getLong(),NodeType.OFSWITCH);
			NetworkNode dst = new NetworkNode(link.getDst().getLong(),NodeType.OFSWITCH);
			
			// Add the link
			success = this.networkGraph.addEdge(src, dst, this.networkLinkFromLink(link));
		}
		
		if(success){
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.linkAdded(link);
		}
		
		return success;
	}
	
	@Override
	public boolean addRemoteSwitchLink(NetworkLink link, String AmorphousNodeId){
		boolean success = false;
		
		NetworkNode src = new NetworkNode(link.getNodeA(),NodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(link.getNodeA(),NodeType.OFSWITCH);
		
		if( !this.isSwitchLinkRegistered(link.getNodeA(), link.getNodeAPort(), link.getNodeB(), link.getNodeBPort()) && (this.isSwitchRegistered(src) || this.isSwitchRegistered(dst)) ){
			// Add the link
			success = this.networkGraph.addEdge(src, dst, link);
		}
		
		if(success){
			// fire event listners
			for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
				eventListner.linkAdded(this.linkFromNetworkLink(link));
		}
		
		return success;
	}
	
	
	@Override
	public boolean removeLocalSwitchLink(Link lnk){
		NetworkNode src = new NetworkNode(lnk.getSrc().getLong(),NodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(lnk.getDst().getLong(),NodeType.OFSWITCH);
		if(this.localSwitches.containsKey(src) || this.localSwitches.containsKey(dst)){
		
			NetworkLink link = this.networkLinkFromLink(lnk);
			if(this.networkGraph.removeEdge(link)){
				// fire event listners
				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
					eventListner.linkRemoved(lnk);
				
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean removeRemoteSwitchLink(NetworkLink link, String AmorphousNodeId){
		NetworkNode src = new NetworkNode(link.getNodeA(), NodeType.OFSWITCH);
		NetworkNode dst = new NetworkNode(link.getNodeB(), NodeType.OFSWITCH);
		
		if((this.remoteSwitchAffinity.containsKey(src) && this.remoteSwitchAffinity.get(src).equals(AmorphousNodeId)) ||
				(this.remoteSwitchAffinity.containsKey(dst) && this.remoteSwitchAffinity.get(dst).equals(AmorphousNodeId))){
		
			if(this.networkGraph.removeEdge(link)){
				// fire event listners
				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
					eventListner.linkRemoved(this.linkFromNetworkLink(link));
				
				return true;
			}
		}
		
		return false;
	}
	
	
	private NetworkLink networkLinkFromLink(Link link){
		return new NetworkLink(link.getSrc().getLong(), link.getDst().getLong(), 
				this.switchService.getSwitch(link.getSrc()).getPort(link.getSrcPort()).getName(),
				link.getSrcPort().getPortNumber(),
				this.switchService.getSwitch(link.getDst()).getPort(link.getDstPort()).getName(),
				link.getDstPort().getPortNumber(),
				this.switchService.getSwitch(link.getSrc()).getPort(link.getSrcPort()).getCurrSpeed());
	}
	
	private Link linkFromNetworkLink(NetworkLink link){
		return new Link(DatapathId.of(link.getNodeA()), OFPort.of(link.getNodeAPortNumber()), 
				DatapathId.of(link.getNodeB()), OFPort.of(link.getNodeBPortNumber()));
	}
	
	

//	@Override
//	public boolean addHost(IDevice Host, String AmorphNodeId, UpdateSource src) {
//		// TODO is there any real use case for checking the node id?!
//		if(this.addHost(Host)){
//			// fire event listeners
//			if(src.equals(UpdateSource.LOCAL))
//				for(IAmorphTopologyListner eventListner : this.localEventListeners)
//					eventListner.hostAdded(Host);
//			else if(src.equals(UpdateSource.REMOTE))
//				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
//					eventListner.hostAdded(Host);
//			return true;
//		}
//		return false;
//	}
//	
//	private boolean addHost(IDevice Host){
//		boolean hostAdded = false;
//		for(SwitchPort port : Host.getAttachmentPoints()){
//			if( this.isSwitchRegistered(port.getSwitchDPID()) ) {
//				if( !this.hosts.containsKey(port.getSwitchDPID()) )
//					this.hosts.put(port.getSwitchDPID(), new ConcurrentSkipListSet<IDevice>());
//				this.hosts.get(port.getSwitchDPID()).add(Host);
//				hostAdded = true;
//			}
//		}
//		return hostAdded;
//	}
//
//	@Override
//	public boolean removeHost(IDevice Host, String AmorphNodeId, UpdateSource src) {
//		// TODO is there any real use case for checking the node id?!
//		if(this.removeHost(Host)){
//			// fire event listeners
//			if(src.equals(UpdateSource.LOCAL))
//				for(IAmorphTopologyListner eventListner : this.localEventListeners)
//					eventListner.hostRemoved(Host);
//			else if(src.equals(UpdateSource.REMOTE))
//				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
//					eventListner.hostRemoved(Host);
//			return true;
//		}
//		return false;
//	}
//	
//	private boolean removeHost(IDevice Host){
//		boolean hostRemoved = false;
//		for( Set<IDevice> switchHosts : this.hosts.values()){
//			for(IDevice switchHost : switchHosts){
//				Set<IDevice> toBeRemoved = new HashSet<IDevice>();
//				if(switchHost.getDeviceKey().equals(Host.getDeviceKey())){
//					toBeRemoved.add(switchHost);
//				}
//				hostRemoved |= switchHosts.removeAll(toBeRemoved);
//			}
//		}
//		return hostRemoved;
//	}
//
//	@Override
//	public boolean updateHost(IDevice Host, String AmorphNodeId, UpdateSource src) {
//		// TODO is there any real use case for checking the node id?!
//		if(this.removeHost(Host) && this.addHost(Host)){
//			// fire event listeners
//			if(src.equals(UpdateSource.LOCAL))
//				for(IAmorphTopologyListner eventListner : this.localEventListeners)
//					eventListner.hostUpdated(Host);
//			else if(src.equals(UpdateSource.REMOTE))
//				for(IAmorphTopologyListner eventListner : this.remoteEventListeners)
//					eventListner.hostUpdated(Host);
//			return true;
//		}
//		return false;
//	}

}
