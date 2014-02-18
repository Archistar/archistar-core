package at.archistar.bft.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.archistar.bft.exceptions.InconsistentResultsException;
import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.ClientFragmentCommand;
import at.archistar.bft.messages.CommitCommand;
import at.archistar.bft.messages.PrepareCommand;
import at.archistar.bft.messages.PreprepareCommand;
import at.archistar.bft.messages.TransactionResult;

/**
 * This is used to store transaction information for an operation. It contains
 * a simple state model (INCOMING -> PRECOMMITED, COMMITED, JOURNAL) that actually
 * mirrors the collection that the transaction is currently in.
 * 
 * TODO: investigate if there's some memory structure that would allow to store
 *       all transactions within one 'tree'
 *       
 * @author andy
 */
public class Transaction implements Comparable<Transaction> {
	
	public enum State {INCOMING, PREPREPARED, PREPARED, COMMITED};
	
	private State state = State.INCOMING;
	
	/** all exchanged precommit commands, should be replicaCount */
	private Set<PrepareCommand> preparedCmds = new HashSet<PrepareCommand>();
	
	private Set<CommitCommand> commitedCmds = new HashSet<CommitCommand>();
	
	private long createdAt = System.currentTimeMillis();

	/* the expected error model */
	private int f = 1;
	
	private Logger logger = LoggerFactory.getLogger(Transaction.class);

	/* fragment id */
	private String fragmentid;
	
	/* my sequence number (from primary) */
	private int sequenceNr;
	
	/* sequence number of previous operation (from primary) */
	private int priorSequenceNr;
	
	private String clientOperationId;
	
	private ReentrantLock lock = new ReentrantLock();
	
	private boolean executed = false;
	
	private boolean primaryReceived = false;
	
	private int replica;

	private ClientCommand clientCmd = null;
	
	private byte[] result = null;
	
	private BftEngineCallbacks callbacks = null;
	
	/** output a more readable id for debug output */
	public String readableId() {
		
		if (clientCmd == null) {
			return "" + this.replica + "/" + this.sequenceNr + "/?/?";
		} else {
			return "" + this.replica + "/" + this.sequenceNr + "/" + this.clientCmd.getClientId() + "/" + this.clientCmd.getClientSequence();
		}
	}
	
	public Transaction(AbstractCommand cmd, int replicaId, int f, BftEngineCallbacks callbacks) {
		
		/* default stuff, valid for all commands */
		this.f = f;
		this.replica = replicaId;
		this.callbacks = callbacks;
	
		/* if there's a fragment-id, record it */
		if (cmd instanceof ClientFragmentCommand) {
			this.fragmentid = ((ClientFragmentCommand)cmd).getFragmentId();
		} else {
			this.fragmentid = null;
		}
		
		if (cmd instanceof PreprepareCommand) {
			PreprepareCommand c = (PreprepareCommand)cmd;
			this.clientOperationId = c.getClientOperationId();
			this.primaryReceived = true;
			this.priorSequenceNr = c.getPriorSequence();
			this.sequenceNr = c.getSequence();
		} else if (cmd instanceof PrepareCommand) {
			PrepareCommand c = (PrepareCommand)cmd;
			this.clientOperationId = c.getClientOperationId();
			this.priorSequenceNr = -1;
			this.sequenceNr = c.getSequence();
		} else if (cmd instanceof ClientCommand) {
			this.clientCmd = (ClientCommand)cmd;
			this.clientOperationId = this.clientCmd.getClientOperationId();
		} else {
			assert(false);
		}
	}
	
	public float getLifetime() {
		return ((float)(System.currentTimeMillis() - this.createdAt));
	}

	private boolean canAdvanceToPreprepared() {
		return state == State.INCOMING && clientCmd != null && primaryReceived;
	}
	
	private boolean canAdvanceToPrepared(int lastCommited) {
		
		logger.debug("{}: {} - {} - {}", readableId(), state, preparedCmds.size(), priorSequenceNr == -1 || priorSequenceNr  <= lastCommited);
		
		if ( state == State.PREPREPARED && preparedCmds.size() >= 2*f ) {
			if (priorSequenceNr == -1 || priorSequenceNr  <= lastCommited) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canAdvanceToCommited() {
		logger.debug("{}: {} - {}/{} - {}/{} - {}", readableId(), state, preparedCmds.size(), commitedCmds.size(), clientCmd != null, primaryReceived, priorSequenceNr);
		if ( state == State.PREPARED && commitedCmds.size() >= (2*f+1) && !executed) {
			return true;
		}
		return false;
	}
	
	public void outputState() {
		logger.warn("{}: {} - {}/{} - {}/{} - {}", readableId(), state, preparedCmds.size(), commitedCmds.size(), clientCmd != null, primaryReceived, priorSequenceNr);
	}
	
	/** execute operation and return result */
	private byte[] execute() {
		
		assert(state == State.PREPARED);
		state = State.COMMITED;
		
		this.executed = true;
		return callbacks.executeClientCommand(clientCmd);
	}

	public String getFragmentId() {
		return this.fragmentid;
	}

	public int getSequenceNr() {
		return this.sequenceNr;
	}

	public int getPriorSequenceNr() {
		return this.priorSequenceNr;
	}
	
	public void lock() {
		this.lock.lock();
	}
	
	public void unlock() {
		this.lock.unlock();
	}

	public void setDataFromPreprepareCommand(int sequence, int priorSequence) {
		this.priorSequenceNr = priorSequence;
		this.sequenceNr = sequence;
	}

	public PreprepareCommand createPreprepareCommand() {
		PreprepareCommand seq = new PreprepareCommand(0, sequenceNr, replica, clientOperationId, priorSequenceNr);
		if (this.state == State.INCOMING) {
			this.state = State.PREPREPARED;
		} else {
			assert(false);
		}
		this.primaryReceived = true;
    	return seq;
	}

	@Override
	public int compareTo(Transaction o) {
		return sequenceNr - o.getSequenceNr();
	}

	public boolean tryMarkDelete() {
		if (state == State.COMMITED && commitedCmds.size() == (3*f+1)) {
			logger.debug("{} advance commited -> to-delete", readableId());
			return true;
		} else {
			return false;
		}
	}
	
	public void setPrepreparedReceived() {
		this.primaryReceived = true;
	}

	public void addPrepareCommand(PrepareCommand c) throws InconsistentResultsException {
		/* verify that the digest matches */
		if (this.preparedCmds.size() > 0) {
			for(PrepareCommand i : preparedCmds) {
				if (!c.getClientOperationId().equalsIgnoreCase(i.getClientOperationId())) {
					throw new InconsistentResultsException();
				}
			}
		}
		
		this.preparedCmds.add(c);
	}

	public void addCommitCommand(CommitCommand cmd) {
		this.commitedCmds.add(cmd);
	}
	
	public void addClientCommand(ClientCommand cmd) {
		this.clientCmd = cmd;
		if (cmd instanceof ClientFragmentCommand) {
			this.fragmentid = ((ClientFragmentCommand) cmd).getFragmentId();
		}
	}

	public Set<PrepareCommand> getPreparedCommands() {
		return this.preparedCmds;
	}

	public Set<CommitCommand> getCommitedCommands() {
		return this.commitedCmds;
	}

	public void setClientOperationId(String clientOperationId) {
		this.clientOperationId = clientOperationId;
	}

	public String getClientOperationId() {
		return this.clientOperationId;
	}

	public void tryAdvanceToPreprepared(boolean primary) {
    	if (canAdvanceToPreprepared()) {
    		logger.debug("{} advance incoming -> (pre-)prepared", readableId());
    		
    		assert(this.state == State.INCOMING);
    		if (primary) {
    			/* primary can directly jump to prepared */
    			this.state = State.PREPARED;
    		} else {
    			PrepareCommand cmd = new PrepareCommand(0, sequenceNr, replica, clientOperationId);
    			this.preparedCmds.add(cmd);
    			this.state = State.PREPREPARED;
    			callbacks.sendToReplicas(cmd);
    		}
    	}
	}

	public boolean tryAdvanceToPrepared(int lastCommited) {
		if (canAdvanceToPrepared(lastCommited)) {
			logger.debug("{} advance prepared -> precommited", replica, readableId());
			
			CommitCommand cmd = new CommitCommand(0, sequenceNr, replica);
			this.commitedCmds.add(cmd);
			
			assert(this.state == State.PREPREPARED);
			this.state = State.PREPARED;
			
			callbacks.sendToReplicas(cmd);
			return true;
		} else {
			return false;
		}
	}

	public boolean tryAdvanceToCommited() {
    	if (canAdvanceToCommited()) {
    		logger.debug("{} advance precommited -> commited", readableId());
    		result = execute();
    		this.callbacks.answerClient(new TransactionResult(this.clientCmd, this.replica, result));
			return true;
    	} else {
    		return false;
    	}
	}
	
	public byte[] getResult() {
		return this.result;
	}

	public boolean hasClientInteraction() {
		return clientCmd != null;
	}

	public void reset() {
		this.state = State.INCOMING;
		this.preparedCmds.clear();
		this.commitedCmds.clear();
		this.primaryReceived = false;
	}

	public void merge(Transaction tmp) {
		setDataFromPreprepareCommand(tmp.getSequenceNr(), tmp.getPriorSequenceNr());
		
		preparedCmds = tmp.getPreparedCommands();
		commitedCmds = tmp.getCommitedCommands();
	}
}