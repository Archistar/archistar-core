package at.ac.ait.archistar.engine.messages;

import at.archistar.bft.messages.ClientFragmentCommand;

/**
 * This command is used by a client to forward an update/write
 * operation to the replicas.
 * 
 * @author andy
 */
public class ReadCommand extends ClientFragmentCommand {

	private static final long serialVersionUID = -6269916515655616482L;
	

	public ReadCommand(int clientId, int clientSequence, String fragmentId) {
		
		super(clientId, clientSequence, fragmentId);
	}
	
	public String toString() {
		return getClientSequence() + ": read";
	}

	@Override
	public byte[] getPayload() {
		return null;
	}
}