package at.ac.ait.archistar.storage;

import org.junit.Before;

public class MemoryStoreTest extends AbstractStorageTest {
     
    @Before
    public void prepareData() {
        store = new MemoryStorage(0);
    }
}