package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class OutboundSocket {
	
	protected static final Logger logger = LoggerFactory.getLogger(OutboundSocket.class);
	
	private final Socket socket;
	private final CommunicationProtocol commProto;

	public OutboundSocket(InetAddress dst, int port) {
		try {
			this.socket = new Socket(dst, port );
			this.commProto = new CommunicationProtocol(this.socket);
		} catch (IOException e) {
			OutboundSocket.logger.error(e.getMessage());
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	/**
	 * Sends a message to the Amorphous node on the other end of the socket
	 * @param msg The message to be sent
	 * @throws IOException 
	 */
	public void sendMessage(byte[] msg) throws IOException {
		this.commProto.sendMessage(msg);
	}
	
	public void closeConnection(){
		this.commProto.closeConnection();
	}

}
