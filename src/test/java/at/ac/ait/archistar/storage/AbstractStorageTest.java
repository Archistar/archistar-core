package at.ac.ait.archistar.storage;

import static org.fest.assertions.api.Assertions.*;

import org.junit.Test;

import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;
import at.ac.ait.archistar.backendserver.storageinterface.InvalidFragmentNameException;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;

public abstract class AbstractStorageTest {

    protected final static byte[] testData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    protected final String fragmentId = "blub";

    protected StorageServer store;

    @Test
    public void testConnect() {
        assertThat(store.connect()).isEqualTo(0);
        try {
            // WHEN I store stuff
            byte[] result = store.putBlob(fragmentId, testData);
            // THEN the same stuff should be the result of the operation
            assertThat(result).isEqualTo(testData);
        } catch (DisconnectedException e) {
            fail("storage server disconnected", e);
        }
    }

    @Test(expected = DisconnectedException.class)
    public void testPutBlobDisconnected() throws InvalidFragmentNameException, DisconnectedException {
        store.putBlob(fragmentId, testData);
    }

    @Test
    public void testPutBlob() {
        assertThat(store.connect()).isEqualTo(0);

        try {
            assertThat(store.putBlob(fragmentId, testData)).isEqualTo(testData);
            assertThat(store.getBlob(fragmentId)).isEqualTo(testData);
        } catch (DisconnectedException e) {
            fail("storage server disconnected", e);
        }
    }
}
