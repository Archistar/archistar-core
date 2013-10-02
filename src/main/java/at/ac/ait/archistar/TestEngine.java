package at.ac.ait.archistar;

import at.ac.ait.archistar.cryptoengine.CryptoEngine;
import at.ac.ait.archistar.distributor.Distributor;
import at.ac.ait.archistar.distributor.ServerConfiguration;
import at.ac.ait.archistar.metadata.MetadataService;

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
