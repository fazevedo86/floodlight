package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.amorphous.cluster.messages.ClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.JoinClusterMessage;
import pt.ulisboa.tecnico.amorphous.cluster.messages.LeaveClusterMessage;

public class ClusterCommunicator extends Thread {

	protected static final Logger logger = LoggerFactory.getLogger(ClusterCommunicator.class);
	
	protected final int ttl;
	protected final InetAddress localmcastGroup;
	protected final InetAddress mcastGroup;
	protected final int dstPort;
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	protected volatile ConcurrentLinkedQueue<String> messages;
	
	public ClusterCommunicator(String mcastGroupIP, int Port) throws UnknownHostException {
		if(Port < ClusterService.MIN_PORT || Port > ClusterService.MAX_PORT){
			throw new UnknownHostException("Invalid port was specified for multicast group " + mcastGroupIP + ": " + Port);
		}
		
		this.mcastGroup = InetAddress.getByName(mcastGroupIP);
		this.localmcastGroup = InetAddress.getByName(ClusterService.LOCAL_MCAST_GROUP);
		this.dstPort = Port;
		this.ttl = 255;

		this.isRunning = new AtomicBoolean(false);
		this.messages = new ConcurrentLinkedQueue<String>();
	}
	
	public synchronized boolean startCommunicator() {
		if(this.isRunning.compareAndSet(false, true)) {
			if(this.srvMcastSocket == null){
				try {
					this.srvMcastSocket = new MulticastSocket(this.dstPort - 1);
					this.srvMcastSocket.setTimeToLive(255);
				} catch (IOException e) {
					ClusterListner.logger.error("Error starting Amorphous Cluster Communicator for group " + this.mcastGroup.getHostAddress() + ": " + e.getMessage());
					this.isRunning.set(false);
				}
			}
			this.sendMessage(new JoinClusterMessage(ClusterService.getInstance().getNodeId()));
		}
		
		return this.isRunning.get();
	}
	
	public synchronized boolean stopCommunicator() {
		if(this.isRunning.get()){
			this.sendMessage(new LeaveClusterMessage(ClusterService.getInstance().getNodeId()));
			this.isRunning.set(false);
			
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean isCommunicating() {
		return this.isRunning.get();
	}

	public boolean sendMessage(ClusterMessage msg){
		return this.messages.add(CommunicationProtocol.getFormatedMessage(msg));
	}
	
	@Override
	public void run(){
		if(!this.isCommunicating() && !this.startCommunicator()){
			return;
		}
		
		ClusterListner.logger.info("Started communicating to Amorphous cluster " + this.mcastGroup.getHostAddress() + " on port " + this.dstPort);
		
		DatagramPacket mcastPacket = null;
		String msg = null;
		byte[] outputBuffer = null;
		
		while(this.isCommunicating() || !this.messages.isEmpty()){
			
			// Wait for it...
			while(this.messages.isEmpty()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
					ClusterCommunicator.logger.error(e.getMessage());
				}
			}
			
			// Cleanup
			mcastPacket = null;
			msg = null;
			outputBuffer = null;
			
			// Packet content
			msg = this.messages.poll();
			outputBuffer = msg.getBytes();
			
			// Create the packet
			mcastPacket = new DatagramPacket(outputBuffer, outputBuffer.length, this.localmcastGroup, this.dstPort);
			
			// Send the packet
			try {
				// Send it locally	
				this.srvMcastSocket.send(mcastPacket);
				
				// Send it away
				mcastPacket.setAddress(this.mcastGroup);
				this.srvMcastSocket.send(mcastPacket);
				
				ClusterCommunicator.logger.debug("ClusterMessage sent: \"" + msg + "\"");
			} catch (IOException e) {
				ClusterCommunicator.logger.error("Caught an exception while trying to send the message \"" + msg + "\" to the Amorphous group: " + e.getMessage());
			}
			
		}
		
		this.srvMcastSocket.close();
	}
	
}
