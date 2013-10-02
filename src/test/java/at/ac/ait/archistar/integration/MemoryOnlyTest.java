package at.ac.ait.archistar.integration;

import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import at.ac.ait.archistar.TestEngine;
import at.ac.ait.archistar.distributor.TestServerConfiguration;
import at.ac.ait.archistar.cryptoengine.CryptoEngine;
import at.ac.ait.archistar.cryptoengine.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.data.CustomSerializer;
import at.ac.ait.archistar.distributor.BFTDistributor;
import at.ac.ait.archistar.distributor.Distributor;
import at.ac.ait.archistar.metadata.MetadataService;
import at.ac.ait.archistar.metadata.SimpleMetadataService;
import at.ac.ait.archistar.storage.MemoryStorage;
import at.ac.ait.archistar.storage.StorageServer;

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
		
		CryptoEngine crypto = new PseudoMirrorCryptoEngine(new CustomSerializer());
		Distributor distributor = new BFTDistributor(serverConfig);
		MetadataService metadata = new SimpleMetadataService(serverConfig, distributor);
		engine = new TestEngine(serverConfig, metadata, distributor, crypto);
	}
	
	@AfterClass
	public static void shutdownServers() {
		serverConfig.teardownTestServer();
	}
}
