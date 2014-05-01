package at.ac.ait.archistar.backendserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.CheckpointMessage;

/**
 * This class responds to the different error situations
 * TODO: maybe use singleton pattern for this?
 * @author andy
 */
public class SecurityMonitor {
	
	private Logger logger = LoggerFactory.getLogger(SecurityMonitor.class);
	
	private final OzymandiasServer myself;
	
	public SecurityMonitor(OzymandiasServer myself) {
		this.myself = myself;
	}

	/**
	 * TODO: this should be called if the sequence nrs start to differ
	 * 
	 * what to do: executed operations should be the same at 2f+1 replicas. As they are the same
	 *             everywhere we should be able to perform the missing operations and continue
	 *             
	 *             everything before "2f+1 prepared operations" should be able to be redone by
	 *             resetting the transactions/operations state to INCOMING and forcing the new
	 *             primary to resend the PREPREPARE message with n = max(sequence)+1. This will
	 *             lead to holes in our sequence numbers -- but then, this should not be a problem.
	 * 
	 * @return
	 */
	public boolean replicasMightBeMalicous() {
		logger.warn("primary might be malicous?");
		
		myself.getBftEngine().tryAdvanceEra();
		assert(false);
		return true;
	}
	
	public void unreachableCodePath() {
		assert(false);
	}
	
	public void localMalicousErrorDetected() {
		assert(false);
	}
	
	/**
	 * TODO: this should be called if my execution log seems to be running behind the
	 *       other replicas
	 * @return
	 */
	public void myselfNeedsReplay() {
		assert(false);
	}

	/**
	 * a checkpoint message was not consistent with already received checkpoint messages
	 * 
	 * @param msg
	 */
	public void invalidCheckpointMessage(CheckpointMessage msg) {
		/* wrong message data type */
		logger.error("invalid checkpoint message server " + myself.getReplicaId() + " message:" + msg);
		assert(false);
	}

	public void invalidMessageReceived(AbstractCommand msg) {
		logger.error("invalid message received on " + myself.getReplicaId() + " message:" + msg);
		assert(false);
	}
}
