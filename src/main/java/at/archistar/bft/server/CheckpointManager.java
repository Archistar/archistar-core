package at.archistar.bft.server;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.archistar.bft.helper.DigestHelper;
import at.archistar.bft.messages.CheckpointMessage;

public class CheckpointManager {
	
	private final Logger logger = LoggerFactory.getLogger(CheckpointManager.class);
	
	private final int serverId;
	
	private final int f;
	
    private int lowWaterMark = -1;
    
    /** this stores operation's results -- will be used for check-pointing */
    private SortedMap<Integer, String> collResults;
    
    private SortedMap<Integer, Set<CheckpointMessage>> unstableCheckpoints;
    
	final private int PERIOD_TIME = 128;
	
	final private BftEngineCallbacks callbacks;
	
	public CheckpointManager(int serverId, BftEngineCallbacks callbacks, int f) {
		this.serverId = serverId;
        this.collResults = new TreeMap<Integer, String>();
        this.unstableCheckpoints = new TreeMap<Integer, Set<CheckpointMessage>>();
        this.f = f;
        this.callbacks = callbacks;
	}
	
	public synchronized void addCheckpointMessage(CheckpointMessage msg) {
		if(msg.getLastExecutedSequence() > lowWaterMark) {
			addCheckpointMessageToLog(msg);
		} else {
			/* this was already committed, discard message */
			// TODO: should we do some notification of cleanups now?
		}
	}

	private void addCheckpointMessageToLog(CheckpointMessage msg) {
		
		int sequence = msg.getLastExecutedSequence();
		Set<CheckpointMessage> currentMessages  = null;
		
		if (unstableCheckpoints.containsKey(sequence)) {
			currentMessages = unstableCheckpoints.get(sequence);
		} else {
			currentMessages = new HashSet<CheckpointMessage>();
		}
		
		/* check if new checkpoint message fits the existing ones */
		for(CheckpointMessage old : currentMessages) {
			if (!old.compatibleWith(msg)) {
				callbacks.invalidCheckpointMessage(msg);
			}
		}
		
		currentMessages.add(msg);
		unstableCheckpoints.put(sequence, currentMessages);
		
		if (unstableCheckpoints.size() >= 10) {
			logger.warn("server {}: unstableCheckpoint count: {}", serverId, unstableCheckpoints.size());
		}
		
		/**
		 * TODO: what is the highest-unstable logic doing? It looks weird, can we remove it?
		 */
		
		int highestUnstable = -1;
		Iterator<Entry<Integer, Set<CheckpointMessage>>> it = unstableCheckpoints.entrySet().iterator();
		while(it.hasNext()) {
			Entry<Integer, Set<CheckpointMessage>> e = it.next();
			if (e.getValue().size() >= (2*f+1)) {
				/* check if this is the youngest checkpoint in there */
				if (highestUnstable == -1 || highestUnstable <= e.getKey()) {
					lowWaterMark = e.getKey();
					it.remove();
				} else {
					logger.warn("highest unstable {} < new checkpoint {}", highestUnstable, e.getKey());
				}
			} else {
				highestUnstable = e.getKey();
			}
		}
	}
	
	/* create a hash for the result */
	private static String createHash(int sequence, byte[] data) {
		
		MessageDigest md = DigestHelper.createMd();
		md.update(ByteBuffer.allocate(4).putInt(sequence).array());
		if (data!= null) {
			md.update(data);
		}
		
		/* store the hash */
		return new String(md.digest());
	}
	
	public synchronized void addTransaction(Transaction t, byte[] result, int viewNr) {
		this.collResults.put(t.getSequenceNr(), createHash(t.getSequenceNr(), result));
		
		if(t.getSequenceNr() % PERIOD_TIME == 0) {
			sendCheckpointMessage(viewNr, t.getSequenceNr());
		}
	}
	
	private void sendCheckpointMessage(int viewNr, int sequence) {
		CheckpointMessage msg = new CheckpointMessage(serverId, -10, viewNr, sequence, collResults);
		collResults = new TreeMap<Integer, String>();
			
		addCheckpointMessageToLog(msg);
			
		/* send CHECKPOINT message */
		callbacks.sendToReplicas(msg);
	}
}
