/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IShutdownListener;
import net.floodlightcontroller.core.IShutdownService;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.IEntityClass;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.IAmorphTopologyService.EventSource;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphTopologyManagerService.UpdateSource;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.internal.state.GlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.state.LocalStateService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphousClusterService;


public class Amorphous implements IFloodlightModule, IOFSwitchListener, ITopologyListener, IDeviceListener  {

	protected static final Logger logger = LoggerFactory.getLogger(Amorphous.class);
	protected Map<String, String> config;
	
	private final String sessionId;
	
	protected IOFSwitchService switchService;
	protected IShutdownService shutdownService;
	protected ITopologyService topologyService;
	protected IDeviceService deviceManagerService;
	protected IAmorphousClusterService amorphcluster;
	
	protected LocalStateService localStateService;
	protected GlobalStateService globalStateService;
	

	public Amorphous() {
		this.sessionId = UUID.randomUUID().toString();
	}
	
	//------------------------------------------------------------------------
	//							IFloodlightModule
	//------------------------------------------------------------------------
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> exportedServices = new ArrayList<Class<? extends IFloodlightService>>(2);
		exportedServices.add(IAmorphTopologyManagerService.class);
		exportedServices.add(IAmorphTopologyService.class);
		
		return exportedServices;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		this.localStateService = LocalStateService.getInstance();
		this.globalStateService = GlobalStateService.getInstance();
		
		Map<Class<? extends IFloodlightService>, IFloodlightService> implementedServices = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		implementedServices.put(IAmorphTopologyService.class, (IAmorphTopologyService)this.localStateService);
		implementedServices.put(IAmorphTopologyManagerService.class, this.localStateService);
		
		return implementedServices;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<Class<? extends IFloodlightService>>();
		
		// We need the switch service in order to monitor switches connecting and disconnecting
		dependencies.add(IOFSwitchService.class);
		
		// and the topology service in order to track changes in the topology
		dependencies.add(ITopologyService.class);
		
		// and the device manager service in order to track hosts
		dependencies.add(IDeviceService.class);
		
		// also the shutdown service in order to perform a clean shutdown
		dependencies.add(IShutdownService.class);

		return dependencies;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Get the configuration for this module
		this.config = context.getConfigParams(this);
		Amorphous.logger.debug("Initializing Amourphous with config: " + this.config.toString());
		
		// Get the switch service service
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.localStateService.updateSwitchServRef(this.switchService);
		// Get the topology service
		this.topologyService = context.getServiceImpl(ITopologyService.class);
		// Get the device manager service
		this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
		// Get the shutdown service
		this.shutdownService = context.getServiceImpl(IShutdownService.class);
		
		// Create a new amorphous cluster class
		try {
			this.amorphcluster = new ClusterService(this.sessionId, this.config.get("group"), Integer.valueOf(this.config.get("port")), Integer.valueOf(this.config.get("helloInterval")));
		} catch (NumberFormatException | UnknownHostException | InstantiationException e) {
			Amorphous.logger.error(e.getClass().getName() + ": " + e.getMessage());
			throw new FloodlightModuleException(e.getClass().getName() + ": " + e.getMessage());
		}
		
		// Log
		Amorphous.logger.info("Initializing local Amorphous node (" + this.amorphcluster.getNodeId() + ")");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Register listners
		this.shutdownService.registerShutdownListener(new IShutdownListener() {
        	@Override
        	public void floodlightIsShuttingDown(){
        		System.out.println("\n\nEXECUTE ORDER 66\n\n");
        		Amorphous.logger.info("EXECUTE ORDER 66");
        		Amorphous.this.amorphcluster.stopClusterService();
        	}
        });
		
		this.localStateService.addTopologyListner(this.globalStateService, EventSource.LOCAL);
		this.globalStateService.setTopologyManager(this.localStateService);
		this.globalStateService.setClusterService(this.amorphcluster);
		
        this.switchService.addOFSwitchListener(this);
        this.topologyService.addListener(this);
        this.deviceManagerService.addListener(this);
        
        // Boot the cluster
		if(this.amorphcluster.startClusterService()) {
			Amorphous.logger.info("Amorphous cluster successfully initialized!");
			this.globalStateService.start();
		} else {
			Amorphous.logger.error("Fatal error while booting Amorphous: Unable to initialize cluster service");
		}
		
	}
	
	//------------------------------------------------------------------------

	
	//------------------------------------------------------------------------
	//							IOFSwitchListner
	//------------------------------------------------------------------------
	
	@Override
	public void switchAdded(DatapathId switchId) {
		// Switch connected
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		this.localStateService.removeLocalSwitch(switchId);
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// Switch now being managed
		this.localStateService.addLocalSwitch(switchId);
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// Update properties such as bandwidth
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// ?
	}

	//------------------------------------------------------------------------
	
	
	//------------------------------------------------------------------------
	//							ITopologyListner
	//------------------------------------------------------------------------
	
	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		Link lnk;
		
		for(LDUpdate update : linkUpdates)
			switch(update.getOperation()){
			
				case LINK_REMOVED:
					lnk = new Link(update.getSrc(), update.getSrcPort(), update.getDst(), update.getDstPort());
					this.localStateService.removeLocalSwitchLink(lnk);
					break;
					
				case LINK_UPDATED:
					lnk = new Link(update.getSrc(), update.getSrcPort(), update.getDst(), update.getDstPort());
					this.localStateService.addLocalSwitchLink(lnk);
					break;
				
				default:
					// Nothing to see here folks, move along...
					break;
			}
	}

	//------------------------------------------------------------------------

	//------------------------------------------------------------------------
	//							IDeviceListener
	//------------------------------------------------------------------------
	
	@Override
	public String getName() {
		return Amorphous.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) {
		// We don't care about callback ordering
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) {
		// We don't care about callback ordering
		return false;
	}

	@Override
	public void deviceAdded(IDevice device) {		
		this.localStateService.addLocalHost(device);
	}

	@Override
	public void deviceRemoved(IDevice device) {
		this.localStateService.removeLocalHost(device);
	}

	@Override
	public void deviceMoved(IDevice device) {
		this.localStateService.updateLocalHost(device);
	}

	@Override
	public void deviceIPV4AddrChanged(IDevice device) {
		this.localStateService.updateLocalHost(device);
	}

	@Override
	public void deviceVlanChanged(IDevice device) {
		this.localStateService.updateLocalHost(device);
	}
	
	//------------------------------------------------------------------------
	
}
