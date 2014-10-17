package at.ac.ait.archistar.engine.dataobjects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Map;

/**
 * Provide a simple FSFile implementation for the prototype
 *
 * @author andy
 */
public class SimpleFile implements Serializable, FSObject {

    private static final long serialVersionUID = 961269599570217856L;
    
    private final String path;
    
    private final byte[] data;
    
    private final Map<String, String> metadata;

    public SimpleFile(String path, byte[] data, Map<String, String> metadata) {
        this.path = path;
        this.data = data.clone();
        this.metadata = metadata;
    }

    @Override
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    @Override
    public String setMetaData(String key, String value) {
        return this.metadata.put(key, value);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getData() {
        return this.data;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
