package pt.ulisboa.tecnico.distributed.forwarding.types;

import java.util.Map;

import org.projectfloodlight.openflow.types.U64;

import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;

public class FlowProgrammingConfirmation implements IAmorphStateMessage<Long> {

	private static final long serialVersionUID = -7899428888564668647L;

	protected final Long rawCookie;
	protected final Long rawDatapathId;
	protected final Boolean success;
	
	public FlowProgrammingConfirmation(U64 cookie, Long rawDatapathId, Boolean SuccessfullyProgrammed) {
		this.rawCookie = cookie.getValue();
		this.rawDatapathId = rawDatapathId;
		this.success = SuccessfullyProgrammed;
	}

	@Override
	public String getOriginatingNodeId() {
		return null;
	}

	@Override
	public Map<String, Integer> getVectorClock() {
		return null;
	}

	@Override
	public Class<FlowProgrammingConfirmation> getMessageType() {
		return FlowProgrammingConfirmation.class;
	}

	@Override
	public Class<Long> getPayloadType() {
		return Long.class;
	}

	@Override
	public Long getPayload() {
		return this.rawCookie;
	}
	
	public U64 getCookie(){
		return U64.of(this.rawCookie);
	}
	
	public Long getRawDatapathId(){
		return this.rawDatapathId;
	}
	
	public Boolean isSuccessfullyProgrammed(){
		return this.success;
	}

}
