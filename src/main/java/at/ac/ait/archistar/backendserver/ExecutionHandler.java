package at.ac.ait.archistar.backendserver;

import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;

/**
 * These are the operations that BFT servers (replicas) will call to perform I/O
 * storage operations
 *
 * @author andy
 */
public interface ExecutionHandler {

    /**
     * write/persist data on the replica (note: this could also mean that the
     * replica uses some external storage as S3 to persist the data)
     *
     * @param fragmentid the unique id under which data should be stored
     * @param data the to be stored data
     * @return the stored data
     * @throws DisconnectedException
     */
    public byte[] putBlob(String fragmentid, byte[] data) throws DisconnectedException;

    /**
     * retrieve persisted data given a storage id (fragment id)
     *
     * NOTE: i need to rewrite this to make error cases clearer (how to signal
     * if there was no stored data given a fragment id, etc)
     *
     * @param fragmentid which data should be retrieved
     * @return the retrieved data
     * @throws DisconnectedException
     */
    public byte[] getBlob(String fragmentid) throws DisconnectedException;
}
