package at.ac.ait.archistar.storage;

import org.junit.Before;

import at.ac.ait.archistar.backendserver.storageinterface.MemoryStorage;

public class MemoryStoreTest extends AbstractStorageTest {
     
    @Before
    public void prepareData() {
        store = new MemoryStorage(0);
    }
}