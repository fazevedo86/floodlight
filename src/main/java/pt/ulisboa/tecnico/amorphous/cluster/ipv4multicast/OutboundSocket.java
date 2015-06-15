package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.ClusterNode;
import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;

public class OutboundSocket extends Thread {
	
	class Packet{
		public InetAddress dst = null;
		public String payload = null;
		
		public Packet(InetAddress dst, String payload){
			this.dst = dst;
			this.payload = payload;
		}
	}

	protected static final Logger logger = LoggerFactory.getLogger(OutboundSocket.class);
	private static volatile OutboundSocket instance = null;
	
	private static final int PACKET_TTL = 255;
	
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	
	protected volatile Queue<Packet> outNormalMsgs;
	protected volatile Queue<Packet> outImportantMsgs;
	
	
	public static OutboundSocket getInstance(){
		return OutboundSocket.instance;
	}
	
	public OutboundSocket() throws UnknownHostException, InstantiationException {
		synchronized(OutboundSocket.class){
			if(OutboundSocket.instance == null){
				OutboundSocket.instance = this;
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + OutboundSocket.class.toString() + ": An instance already exists.");
			}
		}
		
		this.isRunning = new AtomicBoolean(false);
		this.outNormalMsgs = new ConcurrentLinkedQueue<Packet>();
		this.outImportantMsgs = new ConcurrentLinkedQueue<Packet>();
	}
	
	
	
	public synchronized boolean startSocket() {
		if(this.isRunning.compareAndSet(false, true)) {
			if(this.srvMcastSocket == null){
				try {
					this.srvMcastSocket = new MulticastSocket(ClusterCommunicator.getInstance().getClusterPort() - 1);
					this.srvMcastSocket.setTimeToLive(OutboundSocket.PACKET_TTL);
				} catch (IOException e) {
					InboundSocket.logger.error("Error starting Amorphous Cluster outbound Socket: " + e.getMessage());
					this.isRunning.set(false);
				}
			}
		}
		return this.isRunning.get();
	}
	
	public synchronized boolean stopSocket() {
		if(this.isRunning.get()){
			this.isRunning.set(false);
			return this.isRunning.get();
		} else {
			return false;
		}
	}
	
	public synchronized boolean isActive() {
		return this.isRunning.get() || this.isAlive();
	}
	
	protected void sendNormalMessage(ClusterMessage msg){
		String fmsg = CommunicationProtocol.getFormatedMessage(msg);
		this.outNormalMsgs.add(new Packet(ClusterCommunicator.getInstance().getLocalMulticastGroup(),fmsg));
		this.outNormalMsgs.add(new Packet(ClusterCommunicator.getInstance().getGlobalMulticastGroup(),fmsg));
	}

	protected void sendNormalMessage(ClusterNode node, ClusterMessage msg){
		 this.outNormalMsgs.add( new Packet(node.getNodeIP(), CommunicationProtocol.getFormatedMessage(msg)));
	}
	
	protected void sendImportantMessage(ClusterMessage msg){
		String fmsg = CommunicationProtocol.getFormatedMessage(msg);
		this.outImportantMsgs.add(new Packet(ClusterCommunicator.getInstance().getLocalMulticastGroup(),fmsg));
		this.outImportantMsgs.add(new Packet(ClusterCommunicator.getInstance().getGlobalMulticastGroup(),fmsg));
	}
	
	protected void sendImportantMessage(ClusterNode node, ClusterMessage msg){
		 this.outImportantMsgs.add( new Packet(node.getNodeIP(), CommunicationProtocol.getFormatedMessage(msg)));
	}
	
	private void sendPacket(Packet pkt){
		byte[] outputBuffer = pkt.payload.getBytes();
		DatagramPacket mcastPacket = new DatagramPacket(outputBuffer, outputBuffer.length, pkt.dst, ClusterCommunicator.getInstance().getClusterPort());

		// Send the packet
		try {
			this.srvMcastSocket.send(mcastPacket);
			OutboundSocket.logger.debug("ClusterMessage sent: \"" + pkt.payload + "\" to " + pkt.dst.getHostAddress());
		} catch (IOException e) {
			OutboundSocket.logger.error("Caught an exception while trying to send the message \"" + pkt.payload + "\" to " + pkt.dst.getHostAddress() + ": " + e.getMessage());
			OutboundSocket.logger.error(e.getStackTrace().toString());
		}
	}
	
	@Override
	public void run(){
		if(!this.isActive() && !this.startSocket()){
			return;
		}
		
		InboundSocket.logger.info("Started sending messages to the Amorphous cluster");
		
		while(this.isActive() || !this.outImportantMsgs.isEmpty()){		
			// Wait for it...
			while(this.outImportantMsgs.isEmpty() && this.outNormalMsgs.isEmpty()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
					OutboundSocket.logger.error(e.getMessage());
				}
			}
			// Important packets first
			while(!this.outImportantMsgs.isEmpty()) {
				this.sendPacket(this.outImportantMsgs.poll());
			}
			// Important packets always first
			while(!this.outNormalMsgs.isEmpty() && this.outImportantMsgs.isEmpty()) {
				this.sendPacket(this.outNormalMsgs.poll());
			}	
		}
	}
}
