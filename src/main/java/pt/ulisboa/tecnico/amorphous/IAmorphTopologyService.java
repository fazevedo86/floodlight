/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Link;

import org.projectfloodlight.openflow.types.DatapathId;

import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public interface IAmorphTopologyService extends IFloodlightService {
	
	public enum EventSource {
		LOCAL,
		REMOTE,
		BOTH
	}
	
	/**
	 * Check if a switch is currently part of the network graph
	 * 
	 * @param switchId The switch
	 * @return True if the switch is registered in the graph, False otherwise
	 */
	public boolean isSwitchRegistered(DatapathId OFSwitchId);

	/**
	 * Check if a switch is currently part of the network graph
	 * @param switchId The datapath id of the switch
	 * @return True if the switch is registered in the network graph, False otherwise
	 */
	public boolean isSwitchRegistered(NetworkNode ofswitch);

	/**
	 * Check if a link between two switches is currently part of the network graph
	 * 
	 * @param switchId The switch
	 * @return True if the link is registered in the network graph, False otherwise
	 */
	public boolean isSwitchLinkRegistered(Link link);
	
	/**
	 * Check if a link between two switches is currently part of the network graph
	 * 
	 * @param srcId The datapath id of the source switch
	 * @param srcPortName The port on the source switch
	 * @param dstId The datapath id of the destination switch
	 * @param dstPortName The port on the destination switch
	 * @return True if the link is registered in the graph, False otherwise
	 */
	public boolean isSwitchLinkRegistered(Long srcId, String srcPortName, Long dstId, String dstPortName);

	/**
	 * Add a listener for topology events.
	 * The listeners will be notified AFTER the topology has been updated
	 * 
	 * @param listener The listener
	 * @param src The event source
	 */
	public void addTopologyListner(IAmorphTopologyListner listener, EventSource src);

}