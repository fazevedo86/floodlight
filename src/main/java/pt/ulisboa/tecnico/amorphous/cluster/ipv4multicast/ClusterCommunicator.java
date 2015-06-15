package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.cluster.ClusterService;
import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;

public class ClusterCommunicator extends Thread {
	
	class InboundMessage {
		public InetAddress origin;
		public ClusterMessage msg;
		
		public InboundMessage(InetAddress origin, ClusterMessage msg){
			this.origin = origin;
			this.msg = msg;
		}
	}
	
	protected static final Logger logger = LoggerFactory.getLogger(ClusterCommunicator.class);
	private static volatile ClusterCommunicator instance = null;

	public static final String LOCAL_MCAST_GROUP = "224.0.0.1";
	public static final int MIN_PORT = 1025;
	public static final int MAX_PORT = 65534;

	protected final InboundSocket inSocket;
	protected final OutboundSocket outSocket;
	protected final InetAddress localmcastGroup;
	protected final InetAddress mcastGroup;
	protected final int clusterPort;
	protected volatile Queue<InboundMessage> inboundImportantMsgs;
	protected volatile Queue<InboundMessage> inboundNormalMsgs;
	
	
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
		this.inSocket = new InboundSocket();
		this.outSocket = new OutboundSocket();
		
		this.inboundImportantMsgs = new ConcurrentLinkedQueue<InboundMessage>();
		this.inboundNormalMsgs = new ConcurrentLinkedQueue<InboundMessage>();
	}
	
	public boolean initCommunications() {
		// Boot the multicast group listner
		if( this.inSocket.startSocket() && this.outSocket.startSocket() ){
			this.inSocket.start();
			this.outSocket.start();
			this.start();
			return true;
		}
		return false;
	}
	
	public boolean stopCommunications() {
		// Boot the multicast group listner
		return this.inSocket.stopSocket() && this.outSocket.stopSocket();
	}
	
	public boolean isCommunicationActive(){
		return this.inSocket.isActive() || this.outSocket.isActive() || !this.inboundImportantMsgs.isEmpty() || !this.inboundNormalMsgs.isEmpty();
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
	
	public void sendMessage(ClusterMessage msg){
		if(msg.important)
			this.outSocket.sendImportantMessage(msg);
		else
			this.outSocket.sendNormalMessage(msg);
	}
	
	public void sendMessage(ClusterNode node, ClusterMessage msg){
		if(msg.important)
			this.outSocket.sendImportantMessage(node,msg);
		else
			this.outSocket.sendNormalMessage(node,msg);
	}
	
	public void registerInboundMessage(InetAddress originNodeAddress,ClusterMessage inMsg){
		InboundMessage inmsg = new InboundMessage(originNodeAddress, inMsg);
		if(inMsg.important)
			this.inboundImportantMsgs.add(inmsg);
		else
			this.inboundNormalMsgs.add(inmsg);
	}
	
	@Override
	public void run() {
		while(this.isCommunicationActive()){
			// Important messages first
			while(!this.inboundImportantMsgs.isEmpty()){
				InboundMessage inmsg = this.inboundImportantMsgs.poll();
				ClusterService.getInstance().processClusterMessage(inmsg.origin, inmsg.msg);
			}
			// Important messages always first
			while(!this.inboundNormalMsgs.isEmpty() && this.inboundImportantMsgs.isEmpty()){
				InboundMessage inmsg = this.inboundNormalMsgs.poll();
				ClusterService.getInstance().processClusterMessage(inmsg.origin, inmsg.msg);
			}
		}
	}

}
