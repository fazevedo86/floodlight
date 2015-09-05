package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InboundSocket extends Thread {

	protected static final Logger logger = LoggerFactory.getLogger(InboundSocket.class);
	
	protected ServerSocket srvSocket = null;
	protected final int srvPort;
	protected AtomicBoolean isRunning = null;
	
	public InboundSocket(int localPort) {
		this.srvPort = localPort;
		this.isRunning = new AtomicBoolean(false);
	}
	
	public boolean startSocket() {
		if(this.isRunning.compareAndSet(false, true)) {
			// Create the socket
			try {
				this.srvSocket = new ServerSocket(this.srvPort);
				InboundSocket.logger.info("Started server on " + this.srvSocket.getInetAddress().getHostAddress() + ":" + this.srvSocket.getLocalPort());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		
		return this.isRunning.get();
	}
	
	public boolean stopSocket() {
		if(this.isRunning.compareAndSet(true, false)){
			try {
				this.srvSocket.close();
			} catch (IOException e) {
				InboundSocket.logger.warn(e.getMessage());
			}
		}
		
		return this.isRunning.get();
	}
	
	public boolean isActive() {
		return this.isRunning.get() || this.isAlive();
	}
	
	public boolean isServerRunning() {
		return this.isRunning.get() && this.srvSocket.isClosed();
	}

	@Override
	public void run() {
		
		if(!this.isServerRunning() && !this.startSocket()){
			return;
		}
		
		try { 
            while (this.isRunning.get()) {
	            new CommunicationProtocol(this.srvSocket.accept()).start();
	        }
	    } catch (IOException e) {
	    	InboundSocket.logger.warn(e.getMessage());
        } finally {
        	this.stopSocket();
        }
	}
	
}
