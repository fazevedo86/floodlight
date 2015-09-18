/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous;

import java.util.EventListener;

import pt.ulisboa.tecnico.amorphous.types.NetworkHost;
import pt.ulisboa.tecnico.amorphous.types.NetworkLink;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

// TODO Have this interface extend the IListener<String> and implement ListenerDispatcher on LocalStateService
public interface IAmorphTopologyListner extends EventListener {

	public void switchAdded(NetworkNode ofswitch);

	public void switchRemoved(NetworkNode ofswitch);
	
	public void linkAdded(NetworkLink link);
	
	public void linkRemoved(NetworkLink link);
	
	public void hostAdded(NetworkHost host, NetworkLink attachmentPoint);
	
	public void hostRemoved(NetworkHost host);
}
