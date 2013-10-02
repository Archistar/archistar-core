package at.ac.ait.archistar.bft.ozymandias.server;

import at.ac.ait.archistar.storage.DisconnectedException;

/**
 * This is the interface that all storage server will have to implement. These
 * are the calls that will be routed through the BFT network.
 * 
 * @author andy
 */
public interface ExecutionHandler {

	public byte[] putBlob(String fragmentid, byte[] data) throws DisconnectedException;

	public byte[] getBlob(String fragmentid) throws DisconnectedException;
}
