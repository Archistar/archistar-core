package at.archistar.bft.messages;



/**
 * this is a command that was sent between replicas
 * @author andy
 */
public class IntraReplicaCommand extends AbstractCommand {

	private static final long serialVersionUID = 7605418133233561434L;
	
	/** which replica has sent the message */
	private int sourceReplicaId;
	
	/** what's the sequence id (should be the sequence nr of the pre-prepare */
	private int sequenceId;
	
	/** in which view are we in */
	private int viewNr;
	
	public IntraReplicaCommand(int sourceReplicaId, int sequenceId, int viewNr) {
		this.sourceReplicaId = sourceReplicaId;
		this.sequenceId = sequenceId;
		this.viewNr = viewNr;
	}

	public int getViewNr() {
		return this.viewNr;
	}
	
	public int getSequence() {
		return this.sequenceId;
	}
	
	public int getSourceReplicaId() {
		return this.sourceReplicaId;
	}
}