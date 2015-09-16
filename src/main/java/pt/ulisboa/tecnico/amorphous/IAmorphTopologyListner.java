/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous;

import java.util.EventListener;

import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.routing.Link;

import org.projectfloodlight.openflow.types.DatapathId;

import pt.ulisboa.tecnico.amorphous.types.NetworkHost;

// TODO Have this interface extend the IListener<String> and implement ListenerDispatcher on LocalStateService
public interface IAmorphTopologyListner extends EventListener {

	public void switchAdded(DatapathId switchId);

	public void switchRemoved(DatapathId switchId);
	
	public void linkAdded(Link link);
	
	public void linkRemoved(Link link);
	
	public void hostAdded(NetworkHost host);
	
	public void hostUpdated(NetworkHost host);
	
	public void hostRemoved(NetworkHost host);
}
