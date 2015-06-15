package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InboundSocket extends Thread {

	protected static final Logger logger = LoggerFactory.getLogger(InboundSocket.class);
	private static volatile InboundSocket instance = null;
	
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	
	public static InboundSocket getInstance(){
		return InboundSocket.instance;
	}
	
	public InboundSocket() throws UnknownHostException, InstantiationException {
		synchronized(InboundSocket.class){
			if(InboundSocket.instance == null){
				InboundSocket.instance = this;
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + InboundSocket.class.toString() + ": An instance already exists.");
			}
		}
		
		this.isRunning = new AtomicBoolean(false);
	}

	public synchronized boolean startSocket() {
		if(this.isRunning.compareAndSet(false, true)) {
			if(this.srvMcastSocket == null){
				try {
					this.srvMcastSocket = new MulticastSocket(ClusterCommunicator.getInstance().getClusterPort());
				} catch (IOException e) {
					InboundSocket.logger.error("Error starting Amorphous Cluster Listner for group " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + " on port " + ClusterCommunicator.getInstance().getClusterPort() + ": " + e.getMessage());
					this.isRunning.set(false);
				}
			}
			
			try {
				// Always join the local subnet multicast group
				this.srvMcastSocket.joinGroup(ClusterCommunicator.getInstance().getLocalMulticastGroup());
				// Join the global group
				this.srvMcastSocket.joinGroup(ClusterCommunicator.getInstance().getGlobalMulticastGroup());
				InboundSocket.logger.info("Joined Amorphous cluster " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + " on port " + ClusterCommunicator.getInstance().getClusterPort());
			} catch (IOException e) {
				InboundSocket.logger.error("Error binding Amorphous Cluster Listner to group " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + ": " + e.getMessage());
				this.isRunning.set(false);
			}
		}
		
		return this.isRunning.get();
	}
	
	public synchronized boolean stopSocket() {
		return this.isRunning.compareAndSet(true, false);
	}
	
	public synchronized boolean isActive() {
		return this.isRunning.get() || this.isAlive();
	}
	
	@Override
	public void run(){
		
		if(!this.isActive() && !this.startSocket()){
			return;
		}
		
		byte[] inputBuffer = null;
		DatagramPacket rcvPacket = null;
		String packetContent = null;
		InetAddress nodeAddress = null;
		
		// While the server is running listen for incoming packets
		while(this.isActive()){
			inputBuffer = new byte[1500]; // standard MTU
			rcvPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
			try {
				this.srvMcastSocket.receive(rcvPacket);
				nodeAddress = rcvPacket.getAddress();
				packetContent = new String(rcvPacket.getData(),0,rcvPacket.getLength());
				
				InboundSocket.logger.debug("Got a new packet from " + nodeAddress + ": " + packetContent);
				
				// Process the packet
				ClusterCommunicator.getInstance().registerInboundMessage(nodeAddress, CommunicationProtocol.getMessage(packetContent));
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Leave the group and close the socket
		try {
			this.srvMcastSocket.leaveGroup(ClusterCommunicator.getInstance().getLocalMulticastGroup());
			this.srvMcastSocket.leaveGroup(ClusterCommunicator.getInstance().getGlobalMulticastGroup());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
