package at.ac.ait.archistar.engine.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.RemoteFragment;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.engine.crypto.CryptoEngine;
import at.ac.ait.archistar.engine.crypto.DecryptionException;
import at.ac.ait.archistar.engine.dataobjects.FSObject;
import at.ac.ait.archistar.engine.distributor.Distributor;
import at.ac.ait.archistar.engine.distributor.ServerConfiguration;

/**
 * The metadata  service is responsible for storing all meta-information
 * about filesystem layout, versions, etc.
 * 
 * TODO: think about when to remove a mapping from the database
 * TODO: remove direct distributor access
 * 
 * @author Andreas Happe <andreashappe@snikt.net>
 */
public class SimpleMetadataService implements MetadataService {
	
	private Map<String, Set<Fragment>> database;
	
	private final Distributor distributor;
	
	private final ServerConfiguration servers;
	
	private final CryptoEngine crypto;
	
	private Logger logger = LoggerFactory.getLogger(SimpleMetadataService.class);
	
	public SimpleMetadataService(ServerConfiguration servers, Distributor distributor, CryptoEngine crypto) {
		this.distributor = distributor;
		this.servers = servers;
		this.crypto = crypto;
	}
	
	private Set<Fragment> getNewDistributionSet() {
		HashSet<Fragment> distribution = new HashSet<Fragment>();
		for(StorageServer s : this.servers.getOnlineStorageServers()) {
			distribution.add(new RemoteFragment(UUID.randomUUID().toString(), s));
		}
		return distribution;
	}

	private Set<Fragment> getNewDistributionSet(String fragmentId) {
		HashSet<Fragment> distribution = new HashSet<Fragment>();
		for(StorageServer s : this.servers.getOnlineStorageServers()) {
			distribution.add(new RemoteFragment(fragmentId, s));
		}
		return distribution;
	}
	
	private byte[] serializeDatabase() {		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			
			oos.writeInt(database.size());
			for(Entry<String, Set<Fragment>> es : database.entrySet()) {
				oos.writeObject(es.getKey());
				oos.writeInt(es.getValue().size());
				for(Fragment f : es.getValue()) {
					oos.writeObject(f.getFragmentId());
					oos.writeObject(f.getStorageServer().getId());
				}
			}
			oos.close();
			return baos.toByteArray();
		} catch(IOException e) {
			assert(false);
		}
		return null;
	}
	
	@Override
	public int connect() {
		
		int result = distributor.connectServers();
		
		/* get a new distribution set and set fragment-id to index */
		Set<Fragment> index = getNewDistributionSet("index");
		
		/* use crypto engine to retrieve data */
		distributor.getFragmentSet(index);
		byte[] data = null;
		
		try {
			data = this.crypto.decrypt(index);
		} catch (DecryptionException e) {
			logger.warn("error during decryption");
			data = null;
		}

		/* now either rebuild database or create a new one */
		if (data != null) {
			database = deserializeDatabase(data);
		} else {
			this.database = new HashMap<String, Set<Fragment>>();
			synchronize();
		}
		return result;
	}
	
	private Map<String, Set<Fragment>> deserializeDatabase(byte[] readBlob) {
		
		HashMap<String, Set<Fragment>> database = null;
		
		try{
			ByteArrayInputStream door = new ByteArrayInputStream(readBlob);
			ObjectInputStream reader = new ObjectInputStream(door);
			
			int mappingCount = reader.readInt();
			 database = new HashMap<String, Set<Fragment>>();
			
			for(int i = 0; i < mappingCount; i++) {
				String filename = (String) reader.readObject();
				int fragmentCount = reader.readInt();
				HashSet<Fragment> map = new HashSet<Fragment>();
				for(int j=0; j < fragmentCount; j++) {
					String id = (String)reader.readObject();
					String serverid = (String)reader.readObject();
					map.add(new RemoteFragment(id, servers.getStorageServer(serverid)));
				}
				database.put(filename, map);
			}
		}catch (Exception e){
			assert(false);
		}
		return database;
	}

	/**
	 * clear our local cache/directory database
	 */
	@Override
	public int disconnect() {
		synchronize();
		return 0;
	}

	@Override
	public Set<Fragment> getDistributionFor(String path) {
		
		Set<Fragment> distribution = database.get(path);

		/* if we have no mapping, create one */
		if (distribution == null) {
			distribution = getNewDistributionSet();
			database.put(path, distribution);
			synchronize();
		}		
		return distribution;
	}

	/**
	 * as we are non-persistent we do not need any forced synchronization
	 * 
	 * TODO: can we move that to the distributor?
	 * 
	 */
	@Override
	public int synchronize() {
		
		/* this should actually be a merge not a simple sync (for multi-user usage) */
		Set<Fragment> index = getNewDistributionSet("index");
		byte[] data = serializeDatabase();
		
		this.crypto.encrypt(data, index);
		distributor.putFragmentSet(index);
		
		return 0;
	}

	@Override
	public int delete(FSObject obj) {
		
		if (this.database.containsKey(obj.getPath())) {
			this.database.remove(obj.getPath());
		}
		
		synchronize();
		return 0;
	}

	@Override
	public Map<String, String> stat(String path) {
		
		if (this.database.containsKey(path)) {
			return new HashMap<String, String>();
		} else {
			return null;
		}
	}

	@Override
	public Set<String> list(String path) {
		Set<String> initialResult =  this.database.keySet();
		
		Set<String> result  = new HashSet<String>();
		for(String key : initialResult) {
			if (path != null) {
				
				System.err.println("strcmp: " + path + " vs " + key);
				
				if(key.startsWith(path)) {
					result.add(key);
				}
			} else {
				result.add(key);
			}
		}
		return result;
	}
}
