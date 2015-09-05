/**
 * @author filipe.azevedo@tecnico.ulisboa.pt
 * Instituto Superior Tecnico - 2015
 */

package pt.ulisboa.tecnico.amorphous.internal.cluster.ipv4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.IAmorphClusterMessage;
import pt.ulisboa.tecnico.amorphous.internal.cluster.messages.InvalidAmorphClusterMessageException;

public class MessageCodec {
	
	public static IAmorphClusterMessage getDecodedMessage(byte[] encodedMessage) throws InvalidAmorphClusterMessageException{
		IAmorphClusterMessage decodedMessage;
		ObjectInputStream inputStream;
		
		try {
			inputStream = new ObjectInputStream(new ByteArrayInputStream( encodedMessage ));
			decodedMessage = (IAmorphClusterMessage) inputStream.readObject();
		} catch (IOException|ClassNotFoundException e) {
			throw new InvalidAmorphClusterMessageException(e.getMessage());
		}
		
		return decodedMessage;
	}
	
	public static byte[] getEncodedMessage(IAmorphClusterMessage message) throws InvalidAmorphClusterMessageException{
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objOutputStream;
		
		try {
			objOutputStream = new ObjectOutputStream(baOutputStream);
			objOutputStream.writeObject(message);
			objOutputStream.flush();
		} catch (IOException e) {
			throw new InvalidAmorphClusterMessageException(e.getMessage());
		}

		return baOutputStream.toByteArray();
	}
	
}
