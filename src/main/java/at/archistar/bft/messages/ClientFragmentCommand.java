package at.archistar.bft.messages;



public abstract class ClientFragmentCommand extends ClientCommand {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8569203487109177534L;
	private String fragmentId;
	
	public ClientFragmentCommand(int clientId, int clientSequence, String fragmentid) {
		super(clientId, clientSequence);
		this.fragmentId = fragmentid;
	}

	public String getFragmentId() {
		return this.fragmentId;
	}
}
