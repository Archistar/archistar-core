package at.ac.ait.archistar.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.*;

import org.junit.Test;

import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.middleware.TestEngine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.distributor.TestServerConfiguration;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;

public abstract class AbstractIntegrationTest {
	
	protected final static byte[] testData = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74};
	
	protected Map<String, StorageServer> servers;
	
	protected static TestEngine engine;
	
	protected static TestServerConfiguration serverConfig;
	
	protected String randomTestFilename() {
		return UUID.randomUUID().toString();
	}
	
	@Test
	public void testConnect() {
		assertThat(engine.connect()).isEqualTo(engine.getNumberOfServers());
	}
	
	@Test
	public void testStoreOperation() {
		engine.connect();
		
		SimpleFile testObject = new SimpleFile(randomTestFilename(), testData, new HashMap<String, String>());
		
		try {
			/* get initial fragment count */
			HashMap<String, Integer> fragCount = serverConfig.getStorageFragmentCounts(); 

			/* add one fragment per storage server */
			assertThat(engine.putObject(testObject)).isEqualTo(engine.getNumberOfServers());

			/* expect the operation to be executed at (at least) f+1 nodes. The other (3f+1)-(f+1)
			 * nodes might still need longer to perform the operation or might be in error
			 * 
			 * TODO: shouldn't this be 2f+1?
			 */
			int increaseCount = 0;
			for(Map.Entry<String, Integer> m : serverConfig.getStorageFragmentCounts().entrySet()) {
				int oldValue = fragCount.get(m.getKey()).intValue();
				
				if ( m.getValue() == (oldValue +1)) {
					increaseCount++;
				}
			}
			assertThat(increaseCount).isGreaterThanOrEqualTo(2);
		} catch (DisconnectedException e) {
			fail("error while retrieving storage fragment count", e);
		}
	}
	
	@Test
	public void testStoreAndRetrieveOperation() {
		SimpleFile testObject = new SimpleFile(randomTestFilename(), testData, new HashMap<String, String>());
		String path = testObject.getPath();
		
		engine.connect();
		assertThat(engine.putObject(testObject)).isEqualTo(engine.getNumberOfServers());
		assertThat(testObject.getPath()).isEqualTo(path);
		try {
			FSObject retrObject = engine.getObject(path);
			assertThat(retrObject).isNotNull().isInstanceOf(SimpleFile.class);
			assertThat(path).isEqualTo(retrObject.getPath());
			assertThat(((SimpleFile)retrObject).getData()).isEqualTo(testData);
		} catch (DecryptionException e) {
			fail("could not decrypt fragments", e);
		}
	}

}
