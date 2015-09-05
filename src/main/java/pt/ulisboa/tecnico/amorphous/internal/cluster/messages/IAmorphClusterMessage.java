/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.messages;

import java.io.Serializable;
import java.util.Map;

public interface IAmorphClusterMessage extends Serializable {

	/**
	 * Gets the Class type of the message
	 * @return
	 */
	public Class<? extends IAmorphClusterMessage> getMessageType();
	
	/**
	 * Gets the nodeId from where the message originated
	 * @return
	 */
	public String getOriginatingNodeId();
	
	/**
	 * Gets the vector clock of the originating node when the message was issued
	 * @return
	 */
	public Map<String,Integer> getVectorClock();
	
}
