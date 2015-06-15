package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.JoinClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.LeaveClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.NewOFSwitchConnection;

public class CommunicationProtocol {

	protected static final String FIELD_DELIMITER = "|";
	protected static final String FIELD_DELIMITER_REGEX = "\\|";
	
	protected static final String IMPORTANT_MARKER = "!";
	
	public static final int JOIN_CLUSTER = 1;
	public static final int LEAVE_CLUSTER = 2;
	public static final int NEW_OF_CONNECTION = 3;
	
	public static String getFormatedMessage(ClusterMessage msg){
		String fmsg = null;
		
		switch (msg.type) {
			case CommunicationProtocol.JOIN_CLUSTER:
				JoinClusterMessage joinmsg = (JoinClusterMessage)msg;
				fmsg = CommunicationProtocol.genJoinClusterMessage(joinmsg.NodeID);
				break;
				
			case CommunicationProtocol.LEAVE_CLUSTER:
				LeaveClusterMessage leavemsg = (LeaveClusterMessage)msg;
				fmsg = CommunicationProtocol.genLeaveClusterMessage(leavemsg.NodeID);
				break;
				
			case CommunicationProtocol.NEW_OF_CONNECTION:
				NewOFSwitchConnection ofconnmsg = (NewOFSwitchConnection)msg;
				fmsg = CommunicationProtocol.genNewOFSwitchConnection(ofconnmsg.NodeID, ofconnmsg.OFSwitchID);
				break;
				
			default:
				return "";
		}
		
		return (msg.important ? CommunicationProtocol.IMPORTANT_MARKER : "") + fmsg;
	}
	
	private static String genJoinClusterMessage(String NodeID){
		StringBuilder msg = new StringBuilder();
		msg.append(CommunicationProtocol.JOIN_CLUSTER);
		msg.append(CommunicationProtocol.FIELD_DELIMITER);
		msg.append(NodeID);
		return msg.toString();
	}
	
	private static String genLeaveClusterMessage(String NodeID){
		StringBuilder msg = new StringBuilder();
		msg.append(CommunicationProtocol.LEAVE_CLUSTER);
		msg.append(CommunicationProtocol.FIELD_DELIMITER);
		msg.append(NodeID);
		return msg.toString();
	}

	private static String genNewOFSwitchConnection(String NodeID, String OFSwitchID){
		StringBuilder msg = new StringBuilder();
		msg.append(CommunicationProtocol.NEW_OF_CONNECTION);
		msg.append(CommunicationProtocol.FIELD_DELIMITER);
		msg.append(NodeID);
		msg.append(CommunicationProtocol.FIELD_DELIMITER);
		msg.append(OFSwitchID);
		return msg.toString();
	}
	
	public static int getMessageType(String msg){
		if(msg.startsWith(CommunicationProtocol.IMPORTANT_MARKER))
			return Integer.valueOf(msg.substring(1, msg.indexOf(CommunicationProtocol.FIELD_DELIMITER)));
		else
			return Integer.valueOf(msg.substring(0, msg.indexOf(CommunicationProtocol.FIELD_DELIMITER)));
	}
	
	public static ClusterMessage getMessage(String receivedMsg){
		boolean importantMsg = receivedMsg.startsWith(CommunicationProtocol.IMPORTANT_MARKER);
		if(importantMsg)
			receivedMsg = receivedMsg.substring(1);
		
		String[] msgFields = receivedMsg.split(CommunicationProtocol.FIELD_DELIMITER_REGEX);
		
		ClusterMessage msg = null;
		
		switch (CommunicationProtocol.getMessageType(receivedMsg)) {
			case CommunicationProtocol.JOIN_CLUSTER:
				msg = new JoinClusterMessage(msgFields[1],importantMsg);
				break;
	
			case CommunicationProtocol.LEAVE_CLUSTER:
				msg = new LeaveClusterMessage(msgFields[1],importantMsg);
				break;
				
			case CommunicationProtocol.NEW_OF_CONNECTION:
				msg = new NewOFSwitchConnection(msgFields[1],msgFields[2],importantMsg);
				((NewOFSwitchConnection)msg).OFSwitchID = msgFields[2];
				break;
				
			default:
				break;
		}
		
		return msg;
	}
}
