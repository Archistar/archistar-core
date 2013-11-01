package at.ac.ait.archistar.backendserver.storageinterface;

import java.util.HashMap;
import java.util.UUID;

/**
 * provide a simple storage provide through an in-memory database
 * (honestly this is just a java hashmap)
 * 
 * @author andy
 */
public class MemoryStorage implements StorageServer {

	private HashMap<String, byte[]> data;
	
	private boolean connected = false;
	
	private String id;
	
	private int internalBFTId;
	
	public MemoryStorage(int bftId) {
		this.id = UUID.randomUUID().toString();
		this.internalBFTId = bftId;
	}
	
	/* memory storage does not need any connect operation */
	public int connect() {
		this.data = new HashMap<String, byte[]>();
		this.connected = true;
		return 0;
	}

	public int disconnect() {
		
		this.data.clear();
		this.connected = false;
		return 0;
	}

	public byte[] putBlob(String path, byte[] blob) throws DisconnectedException {
		
		if(!this.connected) {
			throw new DisconnectedException();
		}
                
		this.data.put(path, blob);
        return blob;
	}

	public byte[] getBlob(String path) throws DisconnectedException {
		
		if(!this.connected) {
			throw new DisconnectedException();
		}
                
        if(!this.data.containsKey(path)) {
        	return null;
        }

		return this.data.get(path);
	}

	public boolean isConnected() {
		return this.connected;
	}

	public int getFragmentCount() {
		return this.data.size();
	}

	public String getId() {
		return id;
	}

	@Override
	public int getBFTId() {
		return this.internalBFTId;
	}
}
