package pt.ulisboa.tecnico.distributed.forwarding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.IEntityClass;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.core.annotations.LogMessageCategory;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.MatchUtils;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.IAmorphTopologyService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncType;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService;
import pt.ulisboa.tecnico.amorphous.internal.state.ISyncQueueListener;
import pt.ulisboa.tecnico.amorphous.internal.state.InvalidAmorphSyncQueueException;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;
import pt.ulisboa.tecnico.amorphous.types.NetworkHop;
import pt.ulisboa.tecnico.amorphous.types.NetworkHost;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;
import pt.ulisboa.tecnico.distributed.forwarding.types.FlowProgrammingConfirmation;
import pt.ulisboa.tecnico.distributed.forwarding.types.FlowProgrammingRequest;

@LogMessageCategory("Flow Programming")
public class Forwarding extends ForwardingBase implements IFloodlightModule, ISyncQueueListener {
	protected static Logger log = LoggerFactory.getLogger(Forwarding.class);

	protected IAmorphTopologyService amorphTopologyService;
	protected IAmorphTopologyManagerService amorphTopologyManagerService;
	protected IAmorphGlobalStateService amorphGlobalStateService;
	protected Map<U64, ArrayList<Long>> distributedFlowDependencies;
	
	//------------------------------------------------------------------------
	//							IAmorphousClusterService
	//------------------------------------------------------------------------
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// We don't export any services
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// We don't have any services
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(IRoutingService.class); // TODO Do I need it?
		l.add(ITopologyService.class);
		l.add(IDebugCounterService.class); // TODO Do I need it?
		l.add(IOFSwitchService.class);
		
		l.add(IAmorphTopologyService.class);
		l.add(IAmorphGlobalStateService.class);
		l.add(IAmorphTopologyManagerService.class);
		
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		super.init();
		this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
		this.routingEngineService = context.getServiceImpl(IRoutingService.class);  // TODO Do I need it?
		this.topologyService = context.getServiceImpl(ITopologyService.class);
		this.debugCounterService = context.getServiceImpl(IDebugCounterService.class); // TODO Do I need it?
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		
		this.distributedFlowDependencies = new ConcurrentHashMap<U64, ArrayList<Long>>();
		this.amorphTopologyService = context.getServiceImpl(IAmorphTopologyService.class);
		this.amorphTopologyManagerService = context.getServiceImpl(IAmorphTopologyManagerService.class);
		this.amorphGlobalStateService = context.getServiceImpl(IAmorphGlobalStateService.class);
		try {
			this.amorphGlobalStateService.registerSyncQueue(Forwarding.class.getName(), SyncType.GUARANTEED, this);
		} catch (InvalidAmorphSyncQueueException e) {
			Forwarding.log.error("Failed to register my own Amorphous message queue!");
		}

		Map<String, String> configParameters = context.getConfigParams(this);
		String tmp = configParameters.get("hard-timeout");
		if (tmp != null) {
			FLOWMOD_DEFAULT_HARD_TIMEOUT = Integer.parseInt(tmp);
			log.info("Default hard timeout set to {}.", FLOWMOD_DEFAULT_HARD_TIMEOUT);
		} else {
			log.info("Default hard timeout not configured. Using {}.", FLOWMOD_DEFAULT_HARD_TIMEOUT);
		}
		tmp = configParameters.get("idle-timeout");
		if (tmp != null) {
			FLOWMOD_DEFAULT_IDLE_TIMEOUT = Integer.parseInt(tmp);
			log.info("Default idle timeout set to {}.", FLOWMOD_DEFAULT_IDLE_TIMEOUT);
		} else {
			log.info("Default idle timeout not configured. Using {}.", FLOWMOD_DEFAULT_IDLE_TIMEOUT);
		}
		tmp = configParameters.get("priority");
		if (tmp != null) {
			FLOWMOD_DEFAULT_PRIORITY = Integer.parseInt(tmp);
			log.info("Default priority set to {}.", FLOWMOD_DEFAULT_PRIORITY);
		} else {
			log.info("Default priority not configured. Using {}.", FLOWMOD_DEFAULT_PRIORITY);
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		super.startUp();
	}

	//------------------------------------------------------------------------
	
	//------------------------------------------------------------------------
	//							ForwardingBase
	//------------------------------------------------------------------------
	
	@Override
	public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		// We found a routing decision (i.e. Firewall is enabled... it's the only thing that makes RoutingDecisions)
		if (decision != null) {
			if (log.isTraceEnabled()) {
				log.trace("Forwaring decision={} was made for PacketIn={}", decision.getRoutingAction().toString(), pi);
			}

			switch(decision.getRoutingAction()) {
			case NONE:
				// don't do anything
				return Command.CONTINUE;
			case FORWARD_OR_FLOOD:
			case FORWARD:
				doForwardFlow(sw, pi, cntx, false);
				return Command.CONTINUE;
			case MULTICAST:
				// treat as broadcast
				doFlood(sw, pi, cntx);
				return Command.CONTINUE;
			case DROP:
				doDropFlow(sw, pi, decision, cntx);
				return Command.CONTINUE;
			default:
				log.error("Unexpected decision made for this packet-in={}", pi, decision.getRoutingAction());
				return Command.CONTINUE;
			}
		} else {
			// No routing decision was found. Forward to destination or flood if bcast or mcast.
			if (log.isTraceEnabled()) {
				log.trace("No decision was made for PacketIn={}, performing distributed forwarding", pi);
			}

			if (eth.isBroadcast() || eth.isMulticast()) {
				doFlood(sw, pi, cntx);
			} else {
				doForwardFlow(sw, pi, cntx, false);
			}
		}

		return Command.CONTINUE;
	}

	//------------------------------------------------------------------------

	protected void doDropFlow(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		Match m = createMatchFromPacket(sw, inPort, cntx);
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd(); // this will be a drop-flow; a flow that will not output to any ports
		List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
		U64 cookie = AppCookie.makeCookie(FORWARDING_APP_ID, 0);

		fmb.setCookie(cookie)
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setMatch(m)
		.setActions(actions) // empty list
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);

		try {
			if (log.isDebugEnabled()) {
				log.debug("write drop flow-mod sw={} match={} flow-mod={}",
						new Object[] { sw, m, fmb.build() });
			}
			boolean dampened = messageDamper.write(sw, fmb.build());
			log.debug("OFMessage dampened: {}", dampened);
		} catch (IOException e) {
			log.error("Failure writing drop flow mod", e);
		}
	}

	protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		// Amorphous coordinated Flow Programming
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPv4 ipPacket = null;
		Integer srcIP = FlowProgrammingRequest.INFO_NOT_SET, dstIP = FlowProgrammingRequest.INFO_NOT_SET,
				srcPort = FlowProgrammingRequest.INFO_NOT_SET, dstPort = FlowProgrammingRequest.INFO_NOT_SET;
		Short IPProto = FlowProgrammingRequest.INFO_NOT_SET;
		
		Forwarding.log.info("Processing new Flow: etherType=" + eth.getEtherType().getValue());
		
		if(eth.getEtherType().equals(EthType.IPv4)){
			ipPacket = (IPv4)eth.getPayload();
			srcIP = ipPacket.getSourceAddress().getInt();
			dstIP = ipPacket.getDestinationAddress().getInt();
			IPProto = ipPacket.getProtocol().getIpProtocolNumber();
			if(ipPacket.getProtocol().equals(IpProtocol.TCP)){
				TCP transportPacket = (TCP) ipPacket.getPayload();
				srcPort = transportPacket.getSourcePort().getPort();
				dstPort = transportPacket.getDestinationPort().getPort();
			} else if(ipPacket.getProtocol().equals(IpProtocol.UDP)){
				UDP transportPacket = (UDP) ipPacket.getPayload();
				srcPort = transportPacket.getSourcePort().getPort();
				dstPort = transportPacket.getDestinationPort().getPort();
			}
			
			Forwarding.log.info("Processing new IPv4 Flow (proto=" + IPProto + "): src=" + srcIP + " dst=" + dstIP);
		}
		
		NetworkHost src = new NetworkHost(eth.getSourceMACAddress().getLong(), eth.getSourceMACAddress().toString(), Short.valueOf(eth.getVlanID()), srcIP);
		NetworkHost dst = new NetworkHost(eth.getDestinationMACAddress().getLong(), eth.getDestinationMACAddress().toString(), Short.valueOf(eth.getVlanID()), dstIP);
		
		// Update topology if need be
		IDevice srcHost = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);
		IDevice dstHost = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
		this.amorphTopologyManagerService.addLocalHost(srcHost);
		if (dstHost != null) {
			this.amorphTopologyManagerService.addLocalHost(srcHost);
		}
		
		// Distributed network path
		List<NetworkHop> path = this.amorphTopologyService.getNetworkPath(src, dst);
		if(!path.isEmpty() && this.amorphTopologyService.isSwitchManagedLocally(path.get(0).getSwitch())){
			Forwarding.log.info("Performing distributed forwarding!");
//			src = path.get(0).getSourceHost();
//			dst = path.get(0).getDestinationHost();
			final U64 cookie = AppCookie.makeCookie(FORWARDING_APP_ID, 1);
			this.distributedFlowDependencies.put(cookie, new ArrayList<Long>(path.size() - 1));
			
			for(int i = 1; i < path.size(); i++){
				// Generate Flow Programming Request
				FlowProgrammingRequest fpr = new FlowProgrammingRequest(cookie, path.get(i), src, dst, eth.getEtherType());
				fpr.setIPProtocol(IPProto);
				fpr.setSourceTransportPort(srcPort);
				fpr.setDestinationTransportPort(dstPort);
				if( this.amorphTopologyService.isSwitchManagedLocally(path.get(i).getSwitch()) ){
					// Execute local flow programming
					this.processFlowProgrammingRequest(fpr, null, null);
				} else {
					// Send out messages
					this.sendForwardRequest(fpr);
				}
			}
			
			// Program flow on origin ofswitch
			final OFPacketIn pin = pi;
			final FloodlightContext ctx = cntx;
			final FlowProgrammingRequest fpr = new FlowProgrammingRequest(cookie, path.get(0), src, dst, eth.getEtherType());
			fpr.setIPProtocol(IPProto);
			fpr.setSourceTransportPort(srcPort);
			fpr.setDestinationTransportPort(dstPort);
			
			(new Thread(){
				@Override
				public void run(){
					// Wait for all dependencies to be met
					while(Forwarding.this.distributedFlowDependencies.containsKey(cookie) && Forwarding.this.distributedFlowDependencies.get(cookie).size() > 0){
						// Sleep it out
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							Forwarding.log.error(e.getClass().getSimpleName() + ": " + e.getMessage());
						}
					}
					// Program flow on origin switch
					Forwarding.this.processFlowProgrammingRequest(fpr, pin, ctx);
					
					Forwarding.this.distributedFlowDependencies.remove(cookie);
				}
			}).start();
			
		} else {
			// Follow the standard process
			OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
			// Check if we have the location of the destination
			IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
	
			if (dstDevice != null) {
				IDevice srcDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);
				DatapathId srcIsland = topologyService.getL2DomainId(sw.getId());
	
				if (srcDevice == null) {
					log.debug("No device entry found for source device");
					return;
				}
				if (srcIsland == null) {
					log.debug("No openflow island found for source {}/{}",
							sw.getId().toString(), inPort);
					return;
				}
	
				// Validate that we have a destination known on the same island
				// Validate that the source and destination are not on the same switchport
				boolean on_same_island = false;
				boolean on_same_if = false;
				for (SwitchPort dstDap : dstDevice.getAttachmentPoints()) {
					DatapathId dstSwDpid = dstDap.getSwitchDPID();
					DatapathId dstIsland = topologyService.getL2DomainId(dstSwDpid);
					if ((dstIsland != null) && dstIsland.equals(srcIsland)) {
						on_same_island = true;
						if (sw.getId().equals(dstSwDpid) && inPort.equals(dstDap.getPort())) {
							on_same_if = true;
						}
						break;
					}
				}
	
				if (!on_same_island) {
					// Flood since we don't know the dst device
					if (log.isTraceEnabled()) {
						log.trace("No first hop island found for destination " +
								"device {}, Action = flooding", dstDevice);
					}
					doFlood(sw, pi, cntx);
					return;
				}
	
				if (on_same_if) {
					if (log.isTraceEnabled()) {
						log.trace("Both source and destination are on the same " +
								"switch/port {}/{}, Action = NOP",
								sw.toString(), inPort);
					}
					return;
				}
	
				// Install all the routes where both src and dst have attachment
				// points.  Since the lists are stored in sorted order we can
				// traverse the attachment points in O(m+n) time
				SwitchPort[] srcDaps = srcDevice.getAttachmentPoints();
				Arrays.sort(srcDaps, clusterIdComparator);
				SwitchPort[] dstDaps = dstDevice.getAttachmentPoints();
				Arrays.sort(dstDaps, clusterIdComparator);
	
				int iSrcDaps = 0, iDstDaps = 0;
	
				while ((iSrcDaps < srcDaps.length) && (iDstDaps < dstDaps.length)) {
					SwitchPort srcDap = srcDaps[iSrcDaps];
					SwitchPort dstDap = dstDaps[iDstDaps];
	
					// srcCluster and dstCluster here cannot be null as
					// every switch will be at least in its own L2 domain.
					DatapathId srcCluster = topologyService.getL2DomainId(srcDap.getSwitchDPID());
					DatapathId dstCluster = topologyService.getL2DomainId(dstDap.getSwitchDPID());
	
					int srcVsDest = srcCluster.compareTo(dstCluster);
					if (srcVsDest == 0) {
						if (!srcDap.equals(dstDap)) {
							Route route =
									routingEngineService.getRoute(srcDap.getSwitchDPID(), 
											srcDap.getPort(),
											dstDap.getSwitchDPID(),
											dstDap.getPort(), U64.of(0)); //cookie = 0, i.e., default route
							if (route != null) {
								if (log.isTraceEnabled()) {
									log.trace("pushRoute inPort={} route={} " +
											"destination={}:{}",
											new Object[] { inPort, route,
											dstDap.getSwitchDPID(),
											dstDap.getPort()});
								}
	
								U64 cookie = AppCookie.makeCookie(FORWARDING_APP_ID, 0);
	
								Match m = createMatchFromPacket(sw, inPort, cntx);
	
								pushRoute(route, m, pi, sw.getId(), cookie,
										cntx, requestFlowRemovedNotifn, false,
										OFFlowModCommand.ADD);
							}
						}
						iSrcDaps++;
						iDstDaps++;
					} else if (srcVsDest < 0) {
						iSrcDaps++;
					} else {
						iDstDaps++;
					}
				}
			} else {
				// Flood since we don't know the dst device
				doFlood(sw, pi, cntx);
			}			
		}
	}
	
	@Override
	public void onMessageReceived(IAmorphStateMessage message) {
		if(message instanceof FlowProgrammingRequest)
			this.receiveForwardRequest((FlowProgrammingRequest)message);
		else if (message instanceof FlowProgrammingConfirmation)
			this.receiveFlowProgrammingConfirmation((FlowProgrammingConfirmation)message);
	}
	
	protected void receiveFlowProgrammingConfirmation(FlowProgrammingConfirmation message){
		Forwarding.log.info("Received a " + FlowProgrammingConfirmation.class.getSimpleName() + " message!");
		if(this.distributedFlowDependencies.containsKey(message.getCookie())){
			this.distributedFlowDependencies.get(message.getCookie()).remove(message.getRawDatapathId());
		}
		
		if(!message.isSuccessfullyProgrammed())
			Forwarding.log.warn("Flow " + message.getCookie() + " could not be programmed on node " + DatapathId.of(message.getRawDatapathId()));
	}
	
	protected void sendForwardRequest(FlowProgrammingRequest fpr){
		try {
			this.amorphGlobalStateService.queueSyncMessage(Forwarding.class.getName(), fpr, this.amorphTopologyService.getSwitchManager(fpr.getNetworkHop().getSwitch()), null);
			try{
				this.distributedFlowDependencies.get(fpr.getCookie()).add(fpr.getNetworkHop().getSwitch().getNodeId());
			} catch(NullPointerException e){
				Forwarding.log.error("Failed to add dependency for flow " + fpr.getCookie() + ": " + e.getMessage());
			}
		} catch (InvalidAmorphSyncQueueException e) {
			Forwarding.log.error("Failed to add a " + FlowProgrammingRequest.class.getSimpleName() + " message to my Amorphous queue!");
		}
	}
	
	protected void receiveForwardRequest(FlowProgrammingRequest request){
		Forwarding.log.info("Received a " + FlowProgrammingRequest.class.getSimpleName() + " message!");
		
		boolean success = this.processFlowProgrammingRequest(request, null, null);
		
		FlowProgrammingConfirmation fpc = new FlowProgrammingConfirmation(request.getCookie(), request.getNetworkHop().getSwitch().getNodeId(), success);
		try {
			this.amorphGlobalStateService.queueSyncMessage(Forwarding.class.getName(), fpc, this.amorphTopologyService.getSwitchManager(request.getNetworkHop().getSwitch()), null);
		} catch (InvalidAmorphSyncQueueException e) {
			Forwarding.log.error("Failed to add a " + FlowProgrammingConfirmation.class.getSimpleName() + " message to my Amorphous queue!");
		}
	}
	
	protected boolean processFlowProgrammingRequest(FlowProgrammingRequest fpr, OFPacketIn pi, FloodlightContext ctx){
		Match flowMatch = this.CreateMatchFromProgrammingRequest(fpr);
		if(flowMatch == null)
			return false;
		
		return this.pushDistributedRoute(fpr.getNetworkHop(), flowMatch, fpr.getCookie(), null, null);
	}
	
	protected boolean pushDistributedRoute(NetworkHop hop, Match match, U64 cookie, OFPacketIn pi, FloodlightContext ctx){
		DatapathId switchDPID = DatapathId.of(hop.getSwitch().getNodeId());
		IOFSwitch sw = switchService.getSwitch(switchDPID);
		OFPort inboundPort = OFPort.of(hop.getInboundSwitchPort()),
				outboundPort = OFPort.of(hop.getOutboundSwitchPort());
		
		if (sw == null) {
			if (log.isWarnEnabled()) {
				log.warn("Unable to push distributed route, switch at DPID {} " + "not available", switchDPID);
			}
			return false;
		}
		
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();	
		Match.Builder mb = MatchUtils.createRetentiveBuilder(match);
		
		// set input and output ports on the switch
		mb.setExact(MatchField.IN_PORT, inboundPort);
		aob.setPort(outboundPort);
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());
		
		// compile
		fmb.setMatch(mb.build()) // was match w/o modifying input port
		.setActions(actions)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setCookie(cookie)
		.setOutPort(outboundPort)
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		
		try {
			this.messageDamper.write(sw, fmb.build());
			sw.flush();
			
			// Push packet back out
			if(pi != null && ctx != null){
				pushPacket(sw, pi, false, outboundPort, ctx);
			}
			
			return true;
		} catch (IOException e) {
			Forwarding.log.error("An error occurred while programming a flow on switch " + switchDPID + ": " + e.getMessage());
			return false;
		}
	}
	
	protected Match CreateMatchFromProgrammingRequest(FlowProgrammingRequest fpr){
		NetworkNode ofswitch = fpr.getNetworkHop().getSwitch();
		
		if(this.amorphTopologyService.isSwitchManagedLocally(ofswitch)){
			IOFSwitch sw = this.switchService.getSwitch(DatapathId.of(ofswitch.getNodeId()));
			NetworkHost src = fpr.getSourceHost();
			NetworkHost dst = fpr.getDestinationHost();
			
			OFPort inPort = OFPort.of(fpr.getNetworkHop().getInboundSwitchPort());
			VlanVid vlan = VlanVid.ofVlan(src.getVLan());
			
			
			// Create match with Ethernet fields
			Match.Builder mb = sw.getOFFactory().buildMatch();
			
			// Match Ethernet header fields 
			mb.setExact(MatchField.IN_PORT, inPort)
			.setExact(MatchField.ETH_SRC, MacAddress.of(src.getNodeId()))
			.setExact(MatchField.ETH_DST, MacAddress.of(dst.getNodeId()));
			
			// Match 802.1Q header field (Virtual LANs) if it's and 802.1Q encapsulated flow
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
			
			// Match IP header if it's an IP flow and we know the endpoints IP addresses
			IPv4Address srcIp = IPv4Address.of(src.getIPAddress());
			IPv4Address dstIp = IPv4Address.of(dst.getIPAddress());
			
			if(fpr.getEtherType().equals(EthType.IPv4)){
				Forwarding.log.info("IPv4 Flow detected: src=" + src.getIPAddress() + " dst=" + dst.getIPAddress());
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				if((!srcIp.equals(IPv4Address.NONE)) && (!srcIp.isBroadcast()) && (!dstIp.equals(IPv4Address.NONE)) && (!dstIp.isBroadcast()) ){
					// Match IP header fields
					mb.setExact(MatchField.IPV4_SRC, srcIp)
					.setExact(MatchField.IPV4_DST, dstIp);
				} else {
					Forwarding.log.error("Detected an IPv4 flow but was unable to use IP Addresses: src=" + srcIp + " dst=" + dstIp);
				}
	
				if(fpr.getIPProtocol() == 6){ // Requires better solution
					// Match TCP header fields
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
					.setExact(MatchField.TCP_SRC, TransportPort.of(fpr.getSourceTransportPort()))
					.setExact(MatchField.TCP_DST, TransportPort.of(fpr.getDestinationTransportPort()));
				} else if (fpr.getIPProtocol() == 11) {
					// Match UDP header fields
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.TCP_SRC, TransportPort.of(fpr.getSourceTransportPort()))
					.setExact(MatchField.TCP_DST, TransportPort.of(fpr.getDestinationTransportPort()));
					} else {
						Forwarding.log.error("Detected an IPv4 flow but was unable to determine IP protocol: IPproto=" + fpr.getIPProtocol());
					}
			} else if (fpr.getEtherType().equals(EthType.ARP)) {
				// Match ARP header field
				mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
			}
			return mb.build();
		}
		
		// If it's not possible to build a match, return null
		return null;
	}

	/**
	 * Instead of using the Firewall's routing decision Match, which might be as general
	 * as "in_port" and inadvertently Match packets erroneously, construct a more
	 * specific Match based on the deserialized OFPacketIn's payload, which has been 
	 * placed in the FloodlightContext already by the Controller.
	 * 
	 * @param sw, the switch on which the packet was received
	 * @param inPort, the ingress switch port on which the packet was received
	 * @param cntx, the current context which contains the deserialized packet
	 * @return a composed Match object based on the provided information
	 */
	protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
		// The packet in match will only contain the port number.
		// We need to add in specifics for the hosts we're routing between.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();

		// A retentive builder will remember all MatchFields of the parent the builder was generated from
		// With a normal builder, all parent MatchFields will be lost if any MatchFields are added, mod, del
		// TODO (This is a bug in Loxigen and the retentive builder is a workaround.)
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort)
		.setExact(MatchField.ETH_SRC, srcMac)
		.setExact(MatchField.ETH_DST, dstMac);

		if (!vlan.equals(VlanVid.ZERO)) {
			mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
		}

		// TODO Detect switch type and match to create hardware-implemented flow
		// TODO Set option in config file to support specific or MAC-only matches
		if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();
			mb.setExact(MatchField.IPV4_SRC, srcIp)
			.setExact(MatchField.IPV4_DST, dstIp)
			.setExact(MatchField.ETH_TYPE, EthType.IPv4);

			if (ip.getProtocol().equals(IpProtocol.TCP)) {
				TCP tcp = (TCP) ip.getPayload();
				mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
				.setExact(MatchField.TCP_SRC, tcp.getSourcePort())
				.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
			} else if (ip.getProtocol().equals(IpProtocol.UDP)) {
				UDP udp = (UDP) ip.getPayload();
				mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
				.setExact(MatchField.UDP_SRC, udp.getSourcePort())
				.setExact(MatchField.UDP_DST, udp.getDestinationPort());
			}	
		} else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}
		return mb.build();
	}
	
	/**
	 * Creates a OFPacketOut with the OFPacketIn data that is flooded on all ports unless
	 * the port is blocked, in which case the packet will be dropped.
	 * @param sw The switch that receives the OFPacketIn
	 * @param pi The OFPacketIn that came to the switch
	 * @param cntx The FloodlightContext associated with this OFPacketIn
	 */
	@LogMessageDoc(level="ERROR",
			message="Failure writing PacketOut " +
					"switch={switch} packet-in={packet-in} " +
					"packet-out={packet-out}",
					explanation="An I/O error occured while writing a packet " +
							"out message to the switch",
							recommendation=LogMessageDoc.CHECK_SWITCH)
	protected void doFlood(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		if (topologyService.isIncomingBroadcastAllowed(sw.getId(), inPort) == false) {
			if (log.isTraceEnabled()) {
				log.trace("doFlood, drop broadcast packet, pi={}, " +
						"from a blocked port, srcSwitch=[{},{}], linkInfo={}",
						new Object[] {pi, sw.getId(), inPort});
			}
			return;
		}

		// Set Action to flood
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		List<OFAction> actions = new ArrayList<OFAction>();
		if (sw.hasAttribute(IOFSwitch.PROP_SUPPORTS_OFPP_FLOOD)) {
			actions.add(sw.getOFFactory().actions().output(OFPort.FLOOD, Integer.MAX_VALUE)); // FLOOD is a more selective/efficient version of ALL
		} else {
			actions.add(sw.getOFFactory().actions().output(OFPort.ALL, Integer.MAX_VALUE));
		}
		pob.setActions(actions);

		// set buffer-id, in-port and packet-data based on packet-in
		pob.setBufferId(OFBufferId.NO_BUFFER);
		pob.setInPort(inPort);
		pob.setData(pi.getData());

		try {
			if (log.isTraceEnabled()) {
				log.trace("Writing flood PacketOut switch={} packet-in={} packet-out={}",
						new Object[] {sw, pi, pob.build()});
			}
			messageDamper.write(sw, pob.build());
		} catch (IOException e) {
			log.error("Failure writing PacketOut switch={} packet-in={} packet-out={}",
					new Object[] {sw, pi, pob.build()}, e);
		}

		return;
	}


}