/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state.messages;

import java.io.Serializable;

import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;

public interface IAmorphStateMessage<T extends Serializable> extends IAmorphClusterMessage {

	/**
	 * Gets the Class type of the message
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends IAmorphStateMessage> getMessageType();
	
	/**
	 * Gets the type for the payload
	 * @return
	 */
	public Class<T> getPayloadType();
	
	/**
	 * Gets the message payload
	 * @return
	 */
	public T getPayload();
	
}