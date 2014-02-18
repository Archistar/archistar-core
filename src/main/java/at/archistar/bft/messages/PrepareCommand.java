package at.archistar.bft.messages;



/**
 * With this command replicas are exchanging information about
 * planed execution commands. This implicitly orders incoming
 * commands (a bit).
 * 
 * @author andy
 */
public class PrepareCommand extends IntraReplicaCommand {

	/** message digest, used to verify message */
	private String digest;
	
	public PrepareCommand(int viewNr, int sequence, int replicaId, String digest) {
		super(replicaId, sequence, viewNr);
		this.digest = digest;
	}

	private static final long serialVersionUID = 1L;
	
	public String toString() {
		return getSequence() + ": prepare";
	}
	
	public String getClientOperationId() {
		return this.digest;
	}
}