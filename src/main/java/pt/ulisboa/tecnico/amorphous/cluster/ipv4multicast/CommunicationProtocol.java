package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.JoinClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.LeaveClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.NewOFSwitchConnection;

public class CommunicationProtocol {

	public static final String FIELD_DELIMITER = "|";
	public static final int JOIN_CLUSTER = 1;
	public static final int LEAVE_CLUSTER = 2;
	public static final int NEW_OF_CONNECTION = 3;
	
	public static String getFormatedMessage(ClusterMessage msg){		
		switch (msg.type) {
			case CommunicationProtocol.JOIN_CLUSTER:
				JoinClusterMessage joinmsg = (JoinClusterMessage)msg;
				return CommunicationProtocol.genJoinClusterMessage(joinmsg.NodeID);
				
			case CommunicationProtocol.LEAVE_CLUSTER:
				LeaveClusterMessage leavemsg = (LeaveClusterMessage)msg;
				return CommunicationProtocol.genLeaveClusterMessage(leavemsg.NodeID);
				
			case CommunicationProtocol.NEW_OF_CONNECTION:
				NewOFSwitchConnection ofconnmsg = (NewOFSwitchConnection)msg;
				return CommunicationProtocol.genNewOFSwitchConnection(ofconnmsg.NodeID, ofconnmsg.OFSwitchID);
	
			default:
				return "";
		}
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
		return Integer.valueOf(msg.substring(0, msg.indexOf(CommunicationProtocol.FIELD_DELIMITER)));
	}
	
	public static ClusterMessage getMessage(String receivedMsg){
		String[] msgFields = receivedMsg.split(CommunicationProtocol.FIELD_DELIMITER);
		
		ClusterMessage msg = null;
		
		switch (CommunicationProtocol.getMessageType(receivedMsg)) {
			case CommunicationProtocol.JOIN_CLUSTER:
				msg = new JoinClusterMessage(msgFields[1]);
				break;
	
			case CommunicationProtocol.LEAVE_CLUSTER:
				msg = new LeaveClusterMessage(msgFields[1]);
				break;
				
			case CommunicationProtocol.NEW_OF_CONNECTION:
				msg = new NewOFSwitchConnection(msgFields[1],msgFields[2]);
				((NewOFSwitchConnection)msg).OFSwitchID = msgFields[2];
				break;
				
			default:
				break;
		}
		
		return msg;
	}
}
