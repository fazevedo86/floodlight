/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.routing.Link;

import org.projectfloodlight.openflow.types.DatapathId;

import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public interface IAmorphTopologyManagerService extends IFloodlightService {

	public enum UpdateSource {
		LOCAL,
		REMOTE
	}
	
	/**
	 * Add a switch to the topology that is connected to the local instance
	 * 
	 * @param switchId The switch to be added
	 */
	public boolean addLocalSwitch(DatapathId ofswitch);

	/**
	 * Add a switch to the topology that is connected to a remote amorphous instance
	 * 
	 * @param switchId The raw datapathid of the switch to be addded
	 * @param AmorphousNodeId The node id of the remote amorphous instance
	 * @return
	 */
	public boolean addRemoteSwitch(NetworkNode ofswitch, String AmorphousNodeId);
	
	/**
	 * Remove a switch from the topology that was connected to the local instance
	 * 
	 * @param switchId The switch to be removed
	 * @param src The source of the switch removal event
	 */
	public boolean removeLocalSwitch(DatapathId ofswitch);

	/**
	 * Remove a switch from the topology that was connected to a remote amorphous instance
	 * 
	 * @param switchId The raw datapathid of the switch to be added
	 * @param AmorphousNodeId The node id of the remote amorphous instance
	 * @return
	 */
	public boolean removeRemoteSwitch(NetworkNode ofswitch, String AmorphousNodeId);
	
	/**
	 * Add a new link between switches or try to update its bandwidth if it's already registered.
	 * If any of the switches isn't registered yet in the
	 * network graph the call will fail.
	 * 
	 * @param link The link to be added
	 */
	public boolean addLocalSwitchLink(Link link);
	
	/**
	 * 
	 * @param link
	 * @param AmorphousNodeId
	 * @return
	 */
	public boolean addRemoteSwitchLink(NetworkLink link, String AmorphousNodeId);

	/**
	 * Remove a link between switches
	 * 
	 * @param link The link to be removed
	 */
	public boolean removeLocalSwitchLink(Link link);
	
	public boolean removeRemoteSwitchLink(NetworkLink link, String AmorphousNodeId);
	
	/**
	 * Add a host to the topology
	 * 
	 * @param Host The host to be added
	 * @return True if the host was successfully added to the topology, False otherwise
	 */
//	public boolean addLocalHost(IDevice Host);
//	
//	public boolean addRemoteHost(String HostMAC, Integer HostIP, Short HostVLAN, String AmorphousNodeId);
	
	/**
	 * Updates a host that is currently in the topology
	 * 
	 * @param Host The host to be updated
	 * @return True if the host was successfully updated in the topology, False otherwise
	 */
//	public boolean updateLocalHost(IDevice Host);
//	
//	public boolean updateRemoteHost(String HostMAC, Integer HostIP, Short HostVLAN, String AmorphousNodeId);
	
	/**
	 * Removes a host from the topology
	 * 
	 * @param Host The host to be removed
	 * @param src The source of the host removal event
	 * @return True if the host was successfully removed from the topology, False otherwise
	 */
//	public boolean removeLocalHost(IDevice Host);
//
//	public boolean removeRemoteHost(String HostMAC, Short HostVLAN, String AmorphousNodeId);
}