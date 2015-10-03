package pt.ulisboa.tecnico.amorphous.internal.state;

import java.util.EventListener;

import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;

public interface ISyncQueueListener extends EventListener {

	@SuppressWarnings("rawtypes")
	public void onMessageReceived(IAmorphStateMessage message);
	
}
