package at.ac.ait.archistar.backendserver.storageinterface;

import at.ac.ait.archistar.backendserver.ExecutionHandler;

/**
 * Interface to a storage server.
 *
 * @author andy
 */
public interface StorageServer extends ExecutionHandler {

    /**
     * returns stored fragment count -- used for debug purposes
     * 
     * @return the stored fragment count
     * @throws DisconnectedException 
     */
    int getFragmentCount() throws DisconnectedException;

    /**
     * connect to storage backend
     * 
     * @return 0 if there was no error
     */
    int connect();

    /**
     * disconnect from storage backend
     * 
     * @return 0 if there was no error
     */
    int disconnect();

    /**
     * check if storage backend is connected
     * 
     * @return true if it is connected
     */
    boolean isConnected();

    /**
     * return storage id
     * 
     * @return the storage id
     */
    String getId();

    /**
     * return internal BFT id
     * 
     * TODO: this feels weird (as the storage server should not know about the
     * BFT id, maybe this is a relict from the time when BFT itself was not
     * implemented as a storage server -- need to investigate further, maybe
     * we can remove it
     * 
     * @return the internal bft id
     */
    int getBFTId();
}
