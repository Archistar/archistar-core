package at.archistar.bft.messages;

import java.util.Map;
import java.util.Map.Entry;


public class CheckpointMessage extends IntraReplicaCommand {
	
	private int lastExecutedSequence;
	
	private Map<Integer, String> executedCommands;

	public CheckpointMessage(int sourceReplicaId, int sequenceId, int viewNr,
			int lastExecutedSequence, Map<Integer, String> operations) {
		super(sourceReplicaId, sequenceId, viewNr);
		
		this.lastExecutedSequence = lastExecutedSequence;
		this.executedCommands = operations;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1125162800001556097L;
	
	public Integer getLastExecutedSequence() {
		return this.lastExecutedSequence;
	}
	
	public Map<Integer, String> getExecutedCommands() {
		return this.executedCommands;
	}

	public boolean compatibleWith(CheckpointMessage other) {
		if (this.lastExecutedSequence == other.getLastExecutedSequence()) {
			if (other.getExecutedCommands().size() == this.executedCommands.size()) {
				for(Entry<Integer, String> e : other.getExecutedCommands().entrySet()) {
					if (!e.getValue().equalsIgnoreCase(this.executedCommands.get(e.getKey()))) {
						return false;
					}
				}
			} else {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
}
