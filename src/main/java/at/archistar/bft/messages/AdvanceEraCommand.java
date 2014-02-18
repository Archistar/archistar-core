package at.archistar.bft.messages;



public class AdvanceEraCommand extends IntraReplicaCommand {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4254847418079542679L;
	
	private final int newEra;

	public AdvanceEraCommand(int sourceReplicaId, int sequenceId, int viewNr, int newEra) {
		super(sourceReplicaId, sequenceId, viewNr);
		this.newEra = newEra;
	}
	
	public int getNewEra() {
		return this.newEra;
	}
}