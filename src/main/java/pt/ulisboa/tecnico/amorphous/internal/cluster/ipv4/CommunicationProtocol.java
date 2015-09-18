package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommunicationProtocol extends Thread {

	protected static final Logger logger = LoggerFactory.getLogger(CommunicationProtocol.class);
	
	private final Socket socket;
	private DataOutputStream outStream;
	private BufferedInputStream inStream;
	
	public CommunicationProtocol(Socket socket) throws IOException {
		super(CommunicationProtocol.class.getSimpleName() + " " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		
		this.socket = socket;
		this.outStream = new DataOutputStream( this.socket.getOutputStream() );
		this.inStream = new BufferedInputStream( this.socket.getInputStream() );
	}
	
	/**
	 * Read a given amount of bytes from the socket
	 * 
	 * @param length the amount of bytes to be read
	 * @return a byte array of size length containing the read bytes
	 * @throws IOException 
	 * @throws IllegalArgumentException if the parameter length is less than 1
	 */
	private byte[] readFromSocket(int length) throws IOException{
		if(length < 1)
			throw new IllegalArgumentException("Tried to read " + length + " bytes from a socket");
			
		byte[] buffer = new byte[length];
		int readBytes = 0;
		
		try{
			while(readBytes < length)
				readBytes += this.inStream.read(buffer, readBytes, length - readBytes);
		} catch(IndexOutOfBoundsException e){
			CommunicationProtocol.logger.error("The connection " + this.socket.getInetAddress().getHostAddress() + ":" + this.socket.getPort() + " has been closed.");
			this.endComm();
			return null;
		}
		
		return buffer;
	}
	
	/**
	 * Accepts an incoming message
	 */
	private void acceptMessage(){
		int dataLen = 0;
		byte[] payload = null;
		
		try {
			dataLen = ByteBuffer.wrap(this.readFromSocket(4)).getInt();
			
			payload = this.readFromSocket(dataLen);
			
			// Check for null payload (triggered by an attempt to read from a closed connection)
			if(payload == null)
				return;
			
		} catch(IllegalArgumentException ile){
			CommunicationProtocol.logger.info(ile.getMessage());
		} catch(IOException ioe) {
			CommunicationProtocol.logger.error(ioe.getMessage());
		} catch(NullPointerException e){
			
		}
		
		// Process the packet if we have one to process
		if(payload != null)
			ClusterCommunicator.getInstance().registerInboundMessage(this.socket.getInetAddress(), payload);
	}
	
	/**
	 * Sends a message to the socket
	 * @param msg the message to be sent
	 * @throws IOException
	 */
	public void sendMessage(byte[] msg) throws IOException {
		if(this.socket.isOutputShutdown())
			throw new SocketException("Unable to send message to " + this.socket.getInetAddress().getHostAddress() + ": The socket is closed!");
		
		byte[] dataLen = ByteBuffer.allocate(4).putInt(msg.length).array();
		
		this.outStream.write(dataLen);
		this.outStream.write(msg);
	}
	
	/**
	 * Shuts down the input and output streams
	 */
	private void endComm(){
		try {
			this.socket.shutdownInput();
			this.socket.shutdownOutput();
		} catch (IOException e) {
			CommunicationProtocol.logger.info(e.getMessage());
		}
	}
	
	/**
	 * Shuts down the input and output stream and closes the socket
	 */
	public void closeConnection(){
		this.endComm();
		try {
			this.socket.close();
		} catch (IOException e) {
			CommunicationProtocol.logger.info(e.getMessage());
		}
	}
	
	@Override
	public void run(){
		while(!this.socket.isInputShutdown()){
			this.acceptMessage();
		}
		
		this.closeConnection();
	}

}
