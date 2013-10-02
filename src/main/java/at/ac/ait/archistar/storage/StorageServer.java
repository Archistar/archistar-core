package at.ac.ait.archistar.storage;

import at.ac.ait.archistar.bft.ozymandias.server.ExecutionHandler;

/**
 * Interface to a storage server.
 * 
 * @author andy
 */
public interface StorageServer extends ExecutionHandler {

	int getFragmentCount() throws DisconnectedException;
	
	int connect();

	int disconnect();

	boolean isConnected();

	String getId();
	
	int getBFTId();
}