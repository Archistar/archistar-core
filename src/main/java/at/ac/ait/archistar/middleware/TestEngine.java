package at.ac.ait.archistar.middleware;

import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.distributor.Distributor;
import at.ac.ait.archistar.middleware.distributor.ServerConfiguration;
import at.ac.ait.archistar.middleware.metadata.MetadataService;

/**
 * This engine adds some features needed for testing.
 * 
 * Mostly added to not further mess up the Engine code
 * 
 * @author andy
 */
public class TestEngine extends Engine {
	public TestEngine(ServerConfiguration servers, MetadataService naming, Distributor distributor, CryptoEngine crypto) {
		super(servers, naming, distributor, crypto);
	}
	
	public int getNumberOfServers() {
		return servers.getOnlineStorageServerCount();
	}

	public Distributor getDistributor() {
		return this.distributor;
	}

}
