package at.ac.ait.archistar.backendserver.storageinterface;

import at.ac.ait.archistar.backendserver.ExecutionHandler;

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