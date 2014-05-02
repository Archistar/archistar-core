package at.ac.ait.archistar.backendserver.fragments;

import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;

/**
 * This is a standard fragment, ie. one piece of data on one storage server.
 *
 * @author andy
 */
public interface Fragment {

    public enum EncryptionScheme {
        NONE,
        SHAMIR
    };

    /**
     * @param data the data that should be stored within the fragment
     * @return the stored data
     */
    byte[] setData(byte[] data);

    /**
     * @return the data that we believe to be on the storage server
     */
    byte[] getData();

    /**
     * @return is this fragment already stored/synced with the backend? storage
     * server
     */
    boolean isSynchronized();

    /**
     * @param value set the synchronized state (is the fragment in-sync with the
     * storage server side representation) to true
     * @return the new state
     */
    boolean setSynchronized(boolean value);

    /**
     * @return fragments ID upon the server
     */
    String getFragmentId();

    /**
     * @return returns the used storage server
     */
    StorageServer getStorageServer();

    /**
     * sets the fragment id
     *
     * @param string the new fragment id
     * @return the set fragment id
     */
    String setFragmentId(String string);

    /**
     * which encryption scheme was used for this fragment
     * @return the used encryption scheme
     */
    EncryptionScheme getEncryptionScheme();

    /**
     * sets the to-be-used encryption scheme
     * 
     * @param scheme the scheme which will be used to encrypt the data
     */
    void setEncryptionScheme(EncryptionScheme scheme);
}
