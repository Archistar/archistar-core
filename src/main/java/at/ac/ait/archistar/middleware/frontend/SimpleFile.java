package at.ac.ait.archistar.middleware.frontend;

import java.io.Serializable;
import java.util.Map;

/**
 * Provide a simple FSFile implementation for the prototype
 * 
 * @author andy
 */
public class SimpleFile implements FSFile, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 961269599570217856L;
	private String path;
	private byte[] data;
	private Map<String, String> metadata;

	public SimpleFile(String path, byte[] data, Map<String, String> metadata) {
		this.path = path;
		this.data = data;
		this.metadata = metadata;
	}

	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	public String setMetaData(String key, String value) {
		return this.metadata.put(key, value);
	}

	public byte[] getData() {
		return this.data;
	}

	public String getPath() {
		return this.path;
	}
}
