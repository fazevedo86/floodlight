package pt.ulisboa.tecnico.amorphous.cluster.ipv4multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterListner extends Thread {

	protected static final Logger logger = LoggerFactory.getLogger(ClusterListner.class);
	
	protected final InetAddress localmcastGroup;
	protected final InetAddress mcastGroup;
	protected final int dstPort;
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	
	public ClusterListner(String mcastGroupIP, int Port) throws UnknownHostException {
		if(Port < ClusterService.MIN_PORT || Port > ClusterService.MAX_PORT){
			throw new UnknownHostException("Invalid port was specified for multicast group " + mcastGroupIP + ": " + Port);
		}
		
		this.localmcastGroup = InetAddress.getByName(ClusterService.LOCAL_MCAST_GROUP);
		this.mcastGroup = InetAddress.getByName(mcastGroupIP);
		this.dstPort = Port;

		this.isRunning = new AtomicBoolean(false);
	}

	public synchronized boolean startListner() {
		if(this.isRunning.compareAndSet(false, true)) {
			try {
				this.srvMcastSocket = new MulticastSocket(this.dstPort);
			} catch (IOException e) {
				ClusterListner.logger.error("Error starting Amorphous Cluster Listner for group " + this.mcastGroup.getHostAddress() + " on port " + this.dstPort + ": " + e.getMessage());
				this.isRunning.set(false);
			}
			
			try {
				// Always join the local subnet multicast group
				this.srvMcastSocket.joinGroup(this.localmcastGroup);

				// Join the global group
				this.srvMcastSocket.joinGroup(this.mcastGroup);
				ClusterListner.logger.info("Joined Amorphous cluster " + this.mcastGroup.getHostAddress() + " on port " + this.srvMcastSocket.getLocalPort());
			} catch (IOException e) {
				ClusterListner.logger.error("Error binding Amorphous Cluster Listner to group " + this.mcastGroup.getHostAddress() + ": " + e.getMessage());
				this.isRunning.set(false);
			}
		}
		
		return this.isRunning.get();
	}
	
	public synchronized boolean stopListner() {
		return this.isRunning.compareAndSet(true, false);
	}
	
	public synchronized boolean isListening() {
		return this.isRunning.get();
	}
	
	@Override
	public void run(){
		
		if(!this.isListening() && !this.startListner()){
			return;
		}
		
		byte[] inputBuffer = null;
		DatagramPacket rcvPacket = null;
		String packetContent = null;
		InetAddress nodeAddress = null;
		
		// While the server is running listen for incoming packets
		while(this.isListening()){
			inputBuffer = new byte[1500]; // standard MTU
			rcvPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
			try {
				this.srvMcastSocket.receive(rcvPacket);
				nodeAddress = rcvPacket.getAddress();
				packetContent = new String(rcvPacket.getData(),0,rcvPacket.getLength());
				
				ClusterListner.logger.debug("Got a new packet from " + nodeAddress + ": " + packetContent);
				
				// Process the packet
				ClusterService.getInstance().processClusterMessage(nodeAddress.getHostAddress(), CommunicationProtocol.getMessage(packetContent));
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Leave the group and close the socket
		try {
			this.srvMcastSocket.leaveGroup(this.mcastGroup);
			this.srvMcastSocket.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
