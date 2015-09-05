/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class McastOutboundSocket extends Thread {
	
	protected static final Logger logger = LoggerFactory.getLogger(McastOutboundSocket.class);
	private static volatile McastOutboundSocket instance = null;
	
	private static final int PACKET_TTL = 255;
	
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	
	protected volatile Queue<Packet> msgQueue;
	
	
	public static McastOutboundSocket getInstance(){
		if(McastOutboundSocket.instance == null)
			try {
				new McastOutboundSocket();
			} catch(InstantiationException e) {
				McastOutboundSocket.logger.info(e.getMessage());
			}
		
		return McastOutboundSocket.instance;
	}
	
	public McastOutboundSocket() throws InstantiationException {
		synchronized(McastOutboundSocket.class){
			if(McastOutboundSocket.instance == null){
				McastOutboundSocket.instance = this;
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + McastOutboundSocket.class.toString() + ": An instance already exists.");
			}
		}
		
		this.isRunning = new AtomicBoolean(false);
		this.msgQueue = new ConcurrentLinkedQueue<Packet>();
	}
	
	public boolean startSocket() {
		if(this.isRunning.compareAndSet(false, true)) {
			synchronized (this.isRunning) {
				if(this.srvMcastSocket == null){
					try {
						this.srvMcastSocket = new MulticastSocket(ClusterCommunicator.getInstance().getClusterPort() - 1);
						this.srvMcastSocket.setTimeToLive(McastOutboundSocket.PACKET_TTL);
					} catch (IOException e) {
						McastInboundSocket.logger.error("Error starting Amorphous Cluster outbound Socket: " + e.getMessage());
						this.isRunning.set(false);
					}
				}
			}
		}
		return this.isRunning.get();
	}
	
	public boolean stopSocket() {
		if(this.isRunning.get()){
			this.isRunning.set(false);
			return this.isRunning.get();
		} else {
			return false;
		}
	}
	
	public boolean isActive() {
		return this.isRunning.get() || this.isAlive();
	}
	
	protected void sendMessage(Packet packet) throws MessageTooLargeException {
		if(packet.getPayload().length > ClusterCommunicator.DATAGRAM_MTU)
			throw new MessageTooLargeException("Message too large to be sent by ipv4 multicast (" + packet.getPayload().length + " bytes)");
		
		this.msgQueue.add(packet);
	}
	
	private void dispatchPacket(Packet pkt){
		DatagramPacket mcastPacket = new DatagramPacket(pkt.getPayload(), pkt.getPayload().length, pkt.getDestination(), ClusterCommunicator.getInstance().getClusterPort());

		// Send the packet
		try {
			this.srvMcastSocket.send(mcastPacket);
			McastOutboundSocket.logger.debug("ClusterMessage sent: \"" + pkt.getPayload() + "\" to " + pkt.getDestination().getHostAddress());
		} catch (IOException e) {
			McastOutboundSocket.logger.error("Caught an exception while trying to send the message \"" + pkt.getPayload() + "\" to " + pkt.getDestination().getHostAddress() + ": " + e.getMessage());
			McastOutboundSocket.logger.error(e.getStackTrace().toString());
		}
	}
	
	/**
	 * Start sending out messages
	 */
	@Override
	public void run(){
		if(!this.isActive() && !this.startSocket()){
			return;
		}
		
		McastInboundSocket.logger.info("Started sending messages to the Amorphous cluster");
		
		while(this.isActive() || !this.msgQueue.isEmpty()){		
			// Wait for it...
			while(this.msgQueue.isEmpty()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
					McastOutboundSocket.logger.error(e.getMessage());
				}
			}
			// Important packets first
			while(!this.msgQueue.isEmpty()) {
				this.dispatchPacket(this.msgQueue.poll());
			}
		}
	}
}
