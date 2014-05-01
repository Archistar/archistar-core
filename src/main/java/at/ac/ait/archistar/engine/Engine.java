package at.ac.ait.archistar.engine;

import java.util.Map;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.engine.crypto.CryptoEngine;
import at.ac.ait.archistar.engine.crypto.DecryptionException;
import at.ac.ait.archistar.engine.dataobjects.CustomSerializer;
import at.ac.ait.archistar.engine.dataobjects.FSObject;
import at.ac.ait.archistar.engine.dataobjects.SimpleFileInterface;
import at.ac.ait.archistar.engine.distributor.Distributor;
import at.ac.ait.archistar.engine.distributor.ServerConfiguration;
import at.ac.ait.archistar.engine.metadata.MetadataService;

/**
 * As most Archistar instances look kinda the same this class tries
 * to capture most of the similarities. This should prevent Archistar
 * clients to reimplement the same functionality over and over again.
 * 
 * @author andy
 */
public class Engine implements SimpleFileInterface {
	
	protected final ServerConfiguration servers;
	
	protected final Distributor distributor;
	
	private final MetadataService metadataService;
	
	private final CryptoEngine crypto;
	
	private final CustomSerializer serializer;

	/**
	 * Create a new Archistar engine containing the following components
	 * 
	 * @param servers which (remote) servers should be used
	 * @param naming which metadata serverice to use for naming
	 * @param distributor distribution options to use for storage 
	 * @param crypto which cryptographic directives to use for security
	 */
	public Engine(ServerConfiguration servers, MetadataService naming, Distributor distributor, CryptoEngine crypto) {
		this.servers = servers;
		this.distributor = distributor;
		this.metadataService = naming;
		this.crypto = crypto;
		this.serializer = new CustomSerializer();
	}

	@Override
	public int connect() {				
		int serverCount = this.distributor.connectServers();
		this.metadataService.connect();
		return serverCount;
	}

	@Override
	public int disconnect() {
		this.metadataService.disconnect();
		this.distributor.disconnectServers();
		return 0;
	}

	@Override
	public synchronized FSObject getObject(String path) throws DecryptionException {
		
		Set<Fragment> fragments = this.metadataService.getDistributionFor(path);
		
		assertThat(fragments).hasSize(servers.getOnlineStorageServerCount());
		boolean result = this.distributor.getFragmentSet(fragments);
		
		// TODO: just assert that nothing went wrong..
		assertThat(result).isEqualTo(true);
		byte[] decrypted =  this.crypto.decrypt(fragments);
		
		return this.serializer.deserialize(decrypted);
	}

	@Override
	public synchronized boolean putObject(FSObject obj) {
		assertThat(obj).isNotNull();
		assertThat(metadataService).isNotNull();
		
		Set<Fragment> fragments = this.metadataService.getDistributionFor(obj.getPath());
		
		byte[] serialized = this.serializer.serialize(obj);
		
		// encrypt the fragments
		Set<Fragment> encryptedData = this.crypto.encrypt(serialized, fragments);
		
		return this.distributor.putFragmentSet(encryptedData);	
	}

	@Override
	public synchronized Map<String, String> statObject(String path) {
		return metadataService.stat(path);
	}

	@Override
	public synchronized int deleteObject(FSObject obj) {
		return metadataService.delete(obj);
	}

	@Override
	public synchronized Set<String> listObjects(String path) {
		return this.metadataService.list(path);
	}
}