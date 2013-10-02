package at.ac.ait.archistar.storage;

import org.junit.Before;

public class JetS3tStorageTest extends AbstractStorageTest {
	@Before
	public void prepareData() {
		store = new JetS3tStorage(0, "xxx", "yyy", "testme.snikt.net"); 
	}
}