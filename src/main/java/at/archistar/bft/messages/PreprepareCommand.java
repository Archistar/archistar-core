package at.archistar.bft.messages;



/**
 * This command is utilized by the primary (currently always replica 0)
 * to forward a generated sequence number to all replicas.
 * 
 * @author andy
 */
public class PreprepareCommand extends IntraReplicaCommand {

	private static final long serialVersionUID = 5708527225627396654L;
	
	private int priorSequence;
		
	private String clientOperationId;
	
	/** TODO: this should be a hash over <REQUEST, o, s, c> */
	@SuppressWarnings("unused")
	private int cmdIdentifier;
	
	public PreprepareCommand(int viewNr, int sequence, int replicaId, String clientOperationId, int priorSequence) {
		super(replicaId, sequence, viewNr);
	
		this.priorSequence = priorSequence;
		this.clientOperationId = clientOperationId;
	}
	
	public String toString() {
		return getSequence() + "/" + clientOperationId +  ": preprepare";
	}

	public int getPriorSequence() {
		return this.priorSequence;
	}
	
	public String getClientOperationId() {
		return this.clientOperationId;
	}
}
