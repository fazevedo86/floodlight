package pt.ulisboa.tecnico.distributed.forwarding.types;

import java.util.Map;

import org.projectfloodlight.openflow.types.U64;

import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;

public class FlowProgrammingConfirmation implements IAmorphStateMessage<Integer> {

	private static final long serialVersionUID = -7899428888564668647L;

	protected final Integer flowId;
	protected final Long rawDatapathId;
	protected final Boolean success;
	
	public FlowProgrammingConfirmation(Integer FlowId, Long rawDatapathId, Boolean SuccessfullyProgrammed) {
		this.flowId = FlowId;
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
	public Class<Integer> getPayloadType() {
		return Integer.class;
	}

	@Override
	public Integer getPayload() {
		return this.flowId;
	}
	
	public Integer getFlowId(){
		return this.flowId;
	}
	
	public Long getRawDatapathId(){
		return this.rawDatapathId;
	}
	
	public Boolean isSuccessfullyProgrammed(){
		return this.success;
	}

}
