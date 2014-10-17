package at.ac.ait.archistar.backendserver.fragments;

import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * minimal implementation of the Fragment interface for use with the Archistar
 * prototype
 *
 * @author andy
 */
public class RemoteFragment implements Fragment {

    private String fragmentId;

    private byte[] data;

    private boolean syncedToServer;

    private StorageServer server;

    private EncryptionScheme scheme;

    public RemoteFragment(String fragmentId) {
        this.fragmentId = fragmentId;
        this.syncedToServer = false;
        this.scheme = EncryptionScheme.NONE;
    }

    public RemoteFragment(String fragmentId, StorageServer server) {
        this.fragmentId = fragmentId;
        this.server = server;
        this.syncedToServer = false;
        this.scheme = EncryptionScheme.NONE;
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] setData(byte[] data) {
        this.data = data.clone();
        this.syncedToServer = false;
        return this.data;
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getData() {
        return this.data;
    }

    @Override
    public boolean isSynchronized() {
        return this.syncedToServer;
    }

    @Override
    public String getFragmentId() {
        return fragmentId;
    }

    @Override
    public StorageServer getStorageServer() {
        return this.server;
    }

    @Override
    public boolean setSynchronized(boolean value) {
        return this.syncedToServer = value;
    }

    @Override
    public String setFragmentId(String fragmentId) {
        return this.fragmentId = fragmentId;
    }

    @Override
    public EncryptionScheme getEncryptionScheme() {
        return this.scheme;
    }

    @Override
    public void setEncryptionScheme(EncryptionScheme scheme) {
        this.scheme = scheme;
    }
}
