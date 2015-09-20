/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.internal.IAmorphGlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.IAmorphousClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.internal.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;
import pt.ulisboa.tecnico.amorphous.internal.state.GlobalStateService;
import pt.ulisboa.tecnico.amorphous.internal.state.messages.IAmorphStateMessage;

public class ClusterCommunicator extends Thread {
	
	class InboundMessage {
		public InetAddress origin;
		public IAmorphClusterMessage msg;
		
		public InboundMessage(InetAddress origin, IAmorphClusterMessage msg){
			this.origin = origin;
			this.msg = msg;
		}
	}
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterCommunicator.class);
	private static volatile ClusterCommunicator instance = null;

	public static final int DATAGRAM_MTU = 1472; // standard MTU of 1500 bytes - 28 bytes of UDP header overhead
	public static final String LOCAL_MCAST_GROUP = "224.0.0.1";
	public static final int MIN_PORT = 1025;
	public static final int MAX_PORT = 65534;

	protected final McastInboundSocket inMcastSocket;
	protected final McastOutboundSocket outMcastSocket;
	protected final InboundSocket inSocket;
	protected final InetAddress localmcastGroup;
	protected final InetAddress mcastGroup;
	protected final int clusterPort;
	protected volatile Queue<InboundMessage> inboundMsgQueue;
	
	
	public static ClusterCommunicator getInstance(){
		return ClusterCommunicator.instance;
	}
	
	public ClusterCommunicator() throws InstantiationException {
		throw new InstantiationException("An error occurred while creating an instance of " + ClusterCommunicator.class.toString() + ": Please use a constructor with an apropriate amount of arguments.");
	}
	
	public ClusterCommunicator(String mcastGroupIP, int Port) throws UnknownHostException, InstantiationException {
		synchronized(ClusterCommunicator.class){
			if(ClusterCommunicator.instance == null){
				if(Port < ClusterCommunicator.MIN_PORT || Port > ClusterCommunicator.MAX_PORT){
					throw new UnknownHostException("Invalid port was specified for multicast group " + mcastGroupIP + ": " + Port);
				}
				this.clusterPort = Port;
				this.mcastGroup = InetAddress.getByName(mcastGroupIP);
				this.localmcastGroup = InetAddress.getByName(ClusterCommunicator.LOCAL_MCAST_GROUP);
				
				ClusterCommunicator.instance = this;
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + ClusterCommunicator.class.toString() + ": An instance already exists.");
			}
		}
		this.inMcastSocket = new McastInboundSocket();
		this.outMcastSocket = new McastOutboundSocket();
		this.inSocket = new InboundSocket(this.getClusterPort());
		
		this.inboundMsgQueue = new ConcurrentLinkedQueue<InboundMessage>();
	}
	
	/**
	 * Start listening for incoming messages and sending out messages
	 * @return
	 */
	public boolean initCommunications() {
		// Boot the multicast group listner
		this.inMcastSocket.startSocket();
		if( this.inMcastSocket.startSocket() && this.outMcastSocket.startSocket() && this.inSocket.startSocket() ){
			this.inMcastSocket.start();
			this.outMcastSocket.start();
			this.inSocket.start();
			this.start();
			return true;
		}
		return false;
	}
	
	public boolean stopCommunications() {
		// Boot the multicast group listner
		return this.inMcastSocket.stopSocket() && this.outMcastSocket.stopSocket() && this.inSocket.stopSocket();
	}
	
	/**
	 * Determine if communications are still active.
	 * Communications are active if a socket is still active or if there are still
	 * inbound messages left to be processed
	 * @return
	 */
	public boolean isCommunicationActive(){
		return this.inMcastSocket.isActive() || this.outMcastSocket.isActive() || this.inSocket.isActive() || !this.inboundMsgQueue.isEmpty();
	}
	
	public int getClusterPort(){
		return this.clusterPort;
	}
	
	public InetAddress getGlobalMulticastGroup(){
		return this.mcastGroup;
	}
	
	public InetAddress getLocalMulticastGroup(){
		return this.localmcastGroup;
	}
	
	/**
	 * Sends message to all the members of the Amorphous cluster
	 * @param msg The message to be sent
	 * @throws InvalidAmorphClusterMessageException 
	 */
	public void sendMessage(IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException{

		try {
			this.outMcastSocket.sendMessage(new Packet(this.localmcastGroup, MessageCodec.getEncodedMessage(msg)));
			this.outMcastSocket.sendMessage(new Packet(this.mcastGroup, MessageCodec.getEncodedMessage(msg)));
		} catch (MessageTooLargeException e) {
			// TODO Retry sending using TCP
			ClusterCommunicator.logger.error(e.getMessage());
		}
	}
	
	/**
	 * Sends a message to a given Amorphous node
	 * @param node The Amorphous node
	 * @param msg The message to be sent
	 * @throws InvalidAmorphClusterMessageException
	 */
	public void sendMessage(ClusterNode node, IAmorphClusterMessage msg) throws InvalidAmorphClusterMessageException{
		OutboundSocket os = new OutboundSocket(node.getNodeIP(), ClusterCommunicator.getInstance().getClusterPort());
		try {
			os.sendMessage(MessageCodec.getEncodedMessage(msg));
		} catch (IOException e) {
			throw new InvalidAmorphClusterMessageException(e.getMessage());
		}
	}
	
	public void registerInboundMessage(InetAddress originNodeAddress, byte[] payload){
		IAmorphClusterMessage inMsg;
		try {
			inMsg = MessageCodec.getDecodedMessage(payload);
		} catch (InvalidAmorphClusterMessageException e) {
			ClusterCommunicator.logger.error(e.getMessage());
			return;
		}
		
		// Only process messages that didn't came from me (special case for multicast)
		if(!inMsg.getOriginatingNodeId().equals(ClusterService.getInstance().getNodeId()))
			this.inboundMsgQueue.add(new InboundMessage(originNodeAddress, inMsg));
	}
	
	/**
	 * Start dispatching received messages
	 */
	@Override
	public void run() {
		IAmorphousClusterService cs = ClusterService.getInstance();
		IAmorphGlobalStateService gss = GlobalStateService.getInstance();
		
		while(this.isCommunicationActive()){
			// Wait for it...
			while(this.inboundMsgQueue.isEmpty()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
					ClusterCommunicator.logger.error(e.getMessage());
				}
			}
			// Process all queued inbound messages
			while(!this.inboundMsgQueue.isEmpty()){
				InboundMessage inmsg = this.inboundMsgQueue.poll();
				
				// Figure out if its a state or cluster message and dispatch it accordingly
				try{
					if(inmsg.msg instanceof IAmorphStateMessage)
						gss.processStateMessage(inmsg.origin, inmsg.msg);
					else
						cs.processClusterMessage(inmsg.origin, inmsg.msg);
				} catch(InvalidAmorphClusterMessageException e) {
					ClusterCommunicator.logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		}
	}

}
