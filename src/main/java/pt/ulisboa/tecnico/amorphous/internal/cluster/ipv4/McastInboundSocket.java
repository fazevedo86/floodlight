/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class McastInboundSocket extends Thread {
	
	protected static final Logger logger = LoggerFactory.getLogger(McastInboundSocket.class);
	private static volatile McastInboundSocket instance = null;
	
	protected MulticastSocket srvMcastSocket = null;
	protected volatile AtomicBoolean isRunning;
	
	public static McastInboundSocket getInstance(){
		if(McastInboundSocket.instance == null)
			try {
				new McastInboundSocket();
			} catch(InstantiationException | UnknownHostException e) {
				McastInboundSocket.logger.info(e.getMessage());
			}
		
		return McastInboundSocket.instance;
	}
	
	public McastInboundSocket() throws UnknownHostException, InstantiationException {
		synchronized(McastInboundSocket.class){
			if(McastInboundSocket.instance == null){
				McastInboundSocket.instance = this;
			} else {
				throw new InstantiationException("An error occurred while creating an instance of " + McastInboundSocket.class.toString() + ": An instance already exists.");
			}
		}
		
		this.isRunning = new AtomicBoolean(false);
	}

	public boolean startSocket() {
		if(this.isRunning.compareAndSet(false, true)) {
			synchronized (this.isRunning) {
				if(this.srvMcastSocket == null){
					try {
						this.srvMcastSocket = new MulticastSocket(ClusterCommunicator.getInstance().getClusterPort());
					} catch (IOException e) {
						McastInboundSocket.logger.error("Error starting Amorphous Cluster Listner for group " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + " on port " + ClusterCommunicator.getInstance().getClusterPort() + ": " + e.getMessage());
						this.isRunning.set(false);
					}
				}
				
				try {
					// Always join the local subnet multicast group
					this.srvMcastSocket.joinGroup(ClusterCommunicator.getInstance().getLocalMulticastGroup());
					// Join the global group
					this.srvMcastSocket.joinGroup(ClusterCommunicator.getInstance().getGlobalMulticastGroup());
					McastInboundSocket.logger.info("Joined Amorphous cluster " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + " on port " + ClusterCommunicator.getInstance().getClusterPort());
				} catch (IOException e) {
					McastInboundSocket.logger.error("Error binding Amorphous Cluster Listner to group " + ClusterCommunicator.getInstance().getGlobalMulticastGroup().getHostAddress() + ": " + e.getMessage());
					this.isRunning.set(false);
				}	
			}
		}
		
		return this.isRunning.get();
	}
	
	public boolean stopSocket() {
		return this.isRunning.compareAndSet(true, false);
	}
	
	public boolean isActive() {
		return this.isRunning.get() || this.isAlive();
	}
	
	/**
	 * Start listening for incoming messages
	 */
	@Override
	public void run(){
		
		if(!this.isActive() && !this.startSocket()){
			return;
		}
		
		ClusterCommunicator clusterComm = ClusterCommunicator.getInstance();
		byte[] inputBuffer = null;
		DatagramPacket rcvPacket = null;
		String packetContent = null;
		InetAddress nodeAddress = null;
		
		// While the server is running listen for incoming packets
		while(this.isActive()){
			inputBuffer = new byte[ClusterCommunicator.DATAGRAM_MTU];
			rcvPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
			try {
				this.srvMcastSocket.receive(rcvPacket);
				nodeAddress = rcvPacket.getAddress();
				packetContent = new String(rcvPacket.getData(),0,rcvPacket.getLength());
				
				McastInboundSocket.logger.debug("Got a new packet from " + nodeAddress + ": " + packetContent);
				
				// Process the packet
				clusterComm.registerInboundMessage(nodeAddress, rcvPacket.getData());
			} catch (IOException e) {
				McastInboundSocket.logger.error(e.getMessage());
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
