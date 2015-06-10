package pt.ulisboa.tecnico.amorphous.cluster;

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
	
	protected final int ttl;
	protected final InetAddress mcastGroup;
	protected MulticastSocket srvMcastSocket = null;
	protected final int dstPort;
	protected AtomicBoolean isRunning;
	
	public ClusterListner(String mcastGroupIP, int Port) throws UnknownHostException {
		this.ttl = 255;
		this.mcastGroup = InetAddress.getByName(mcastGroupIP);
		
		if(Port < -1 || Port > 65535)
			throw new UnknownHostException("Unknown port " + Port + " for multicast group " + mcastGroupIP);
		
		this.dstPort = Port;
		this.isRunning = new AtomicBoolean(false);
	}

	public synchronized boolean startListner() {
		if(this.dstPort > 1024 && this.isRunning.compareAndSet(false, true)) {
			try {
				this.srvMcastSocket = new MulticastSocket(this.dstPort);
				this.srvMcastSocket.joinGroup(this.mcastGroup);
				ClusterListner.logger.info("Joined Amorphous cluster " + this.mcastGroup.getHostAddress() + " on port " + this.srvMcastSocket.getLocalPort());
			} catch (IOException e) {
				System.out.println(e.getMessage());
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
		
		// While the server is running listen for incoming packets
		while(this.isListening()){
			inputBuffer = new byte[1500]; // MAX standard MTU
			rcvPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
			try {
				this.srvMcastSocket.receive(rcvPacket);
				ClusterListner.logger.debug("Got a new packet from " + rcvPacket.getAddress() + ": " + new String(rcvPacket.getData(),0,rcvPacket.getLength()));
				// ToDo: Process the packet
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
