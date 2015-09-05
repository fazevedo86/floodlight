/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.devicemanager.IDevice;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.types.NetworkNode;

public class AddHost implements IAmorphStateMessage<NetworkNode> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7817703665630531213L;

	public AddHost() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getOriginatingNodeId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getMessageType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getPayloadType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkNode getPayload() {
		// TODO Auto-generated method stub
		return null;
	}

}
