package at.ac.ait.archistar.engine.messages;

import at.archistar.bft.messages.ClientFragmentCommand;

/**
 * This command is used by a client to forward an update/write
 * operation to the replicas.
 * 
 * @author andy
 */
public class WriteCommand extends ClientFragmentCommand {

	private static final long serialVersionUID = -6269916515655616482L;
	
	public WriteCommand(int clientId, int clientSequence, String fragmentId, byte[] data) {
		
		super(clientId, clientSequence, fragmentId);
		this.payload = data;
	}
	
	public byte[] getData() {
		return this.payload;
	}
	
	public String toString() {
		return getClientSequence() + ": write";
	}
}
