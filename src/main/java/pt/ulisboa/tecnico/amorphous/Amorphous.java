package pt.ulisboa.tecnico.amorphous;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.sdnplatform.sync.ISyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ClusterListner;
import pt.ulisboa.tecnico.amorphous.cluster.ClusterNode;

public class Amorphous implements IFloodlightModule {

	protected static final Logger logger = LoggerFactory.getLogger(Amorphous.class);
	protected Map<String, String> config;
	protected ISyncService syncService;
	protected Set<ClusterNode> nodes;
	protected ClusterListner listner;
	
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
		
		// Initialize the cluster node set
		this.nodes = new ConcurrentSkipListSet<ClusterNode>();
		
		// Get the sync service
		this.syncService = context.getServiceImpl(ISyncService.class);
		
		// Log
		Amorphous.logger.info("Initializing local Amorphous node (" + this.syncService.getLocalNodeId() + ")");

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
	}

}
