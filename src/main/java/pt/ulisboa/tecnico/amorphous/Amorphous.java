package pt.ulisboa.tecnico.amorphous;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.sdnplatform.sync.ISyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.IAmorphousCluster;
import pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast.ClusterService;


public class Amorphous implements IFloodlightModule {

	protected static final Logger logger = LoggerFactory.getLogger(Amorphous.class);
	protected Map<String, String> config;
	protected ISyncService syncService;
	protected IAmorphousCluster amorphcluster;
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<Class<? extends IFloodlightService>>();
		
		// Make Amorphous depend on the synchronization service
		dependencies.add(ISyncService.class);

		return dependencies;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Get the modules configuration
		this.config = context.getConfigParams(this);
		Amorphous.logger.debug("Initializing Amourphous with config: " + this.config.toString());
		
		// Get the sync service
		this.syncService = context.getServiceImpl(ISyncService.class);
		
		// Create a new amorphous cluster class
		try {
			this.amorphcluster = new ClusterService(UUID.randomUUID().toString(), this.config.get("group"), Integer.valueOf(this.config.get("port")));
		} catch (NumberFormatException | UnknownHostException | InstantiationException e) {
			Amorphous.logger.error(e.getClass().getName() + ": " + e.getMessage());
			throw new FloodlightModuleException(e.getClass().getName() + ": " + e.getMessage());
		}
		
		// Log
		Amorphous.logger.info("Initializing local Amorphous node (" + this.amorphcluster.getNodeId() + ")");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		this.amorphcluster.startClusterService();
	}

}
