package at.ac.ait.archistar.integration;

import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import at.ac.ait.archistar.backendserver.storageinterface.MemoryStorage;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.middleware.TestEngine;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.middleware.distributor.BFTDistributor;
import at.ac.ait.archistar.middleware.distributor.Distributor;
import at.ac.ait.archistar.middleware.distributor.TestServerConfiguration;
import at.ac.ait.archistar.middleware.metadata.MetadataService;
import at.ac.ait.archistar.middleware.metadata.SimpleMetadataService;

public class MemoryOnlyTest extends AbstractIntegrationTest {
	
	@BeforeClass
	public static void prepareServer() {
		/* test configuration */
		HashSet<StorageServer> servers = new HashSet<StorageServer>();
		servers.add(new MemoryStorage(0));
		servers.add(new MemoryStorage(1));
		servers.add(new MemoryStorage(2));
		servers.add(new MemoryStorage(3));			
		serverConfig = new TestServerConfiguration(servers);
		serverConfig.setupTestServer(1);
		
		CryptoEngine crypto = new PseudoMirrorCryptoEngine();
		Distributor distributor = new BFTDistributor(serverConfig);
		MetadataService metadata = new SimpleMetadataService(serverConfig, distributor, crypto);
		engine = new TestEngine(serverConfig, metadata, distributor, crypto);
	}
	
	@AfterClass
	public static void shutdownServers() {
		serverConfig.teardownTestServer();
	}
}
