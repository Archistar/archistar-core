package at.archistar.bft.client;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import at.archistar.bft.exceptions.InconsistentResultsException;
import at.archistar.bft.messages.TransactionResult;

/**
 * This class is used to transform the asynchronous client-replica
 * operations into synchronous operations.
 * 
 * @author andy
 */
public class ClientResult {
	
	private final Lock lock = new ReentrantLock();
	
	private final Condition condition = lock.newCondition();
	
	private int f;

	private int clientId;
	
	private int clientSequence;
	
	private HashMap<Integer, TransactionResult> results= new HashMap<Integer, TransactionResult>();
	
	public ClientResult(int f, int clientId, int clientSequence) {
		this.f = f;
		this.clientId = clientId;
		this.clientSequence = clientSequence;
	}
	
	public boolean addResult(int clientId, int clientSequence, TransactionResult tx) throws InconsistentResultsException {
		
		lock.lock();
	
		/* consistency checks */
		if (this.clientId != clientId || this.clientSequence != clientSequence) {
			lock.unlock();
			throw new InconsistentResultsException();
		}
	
		results.put(tx.getReplicaId(), tx);
		/* NOTE: this condition is highly dependent upon the used secret-sharing mechanism
		 * TODO: move this somehow into crypto engine? */
		if (results.size() >= (2*f+1)) {
			condition.signal();
			lock.unlock();
			return true;
		}
		lock.unlock();
		return false;
	}
	
	public void waitForEnoughAnswers() {
		lock.lock();
		
		if (results.size() < (f+1)) {
			try {
				condition.await();
				lock.unlock();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public boolean containsDataForServer(int bftId) {
		return this.results.containsKey(bftId);
	}

	public byte[] getDataForServer(int bftId) {
		return this.results.get(bftId).getPayload();
	}
}