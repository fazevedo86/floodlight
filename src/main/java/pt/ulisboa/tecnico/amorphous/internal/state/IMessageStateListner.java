/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.state;

import java.util.EventListener;

import net.floodlightcontroller.core.IListener;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService.SyncMessageState;

public interface IMessageStateListner extends IListener<String>, EventListener {

	public void onStateUpdate(SyncMessageState status);

}
