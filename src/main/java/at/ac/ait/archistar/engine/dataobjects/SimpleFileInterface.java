package at.ac.ait.archistar.engine.dataobjects;

import at.archistar.crypto.secretsharing.ReconstructionException;
import java.util.Map;
import java.util.Set;

/**
 * this is the end-user API interface that Archistar implementations should use
 * to interact with the Archistar stack.
 *
 * @author andy
 */
public interface SimpleFileInterface {

    /**
     * connect to all servers
     */
    int connect();

    /**
     * disconnect from all servers
     */
    int disconnect();

    /**
     * retrieve some object from the servers
     */
    FSObject getObject(String path) throws ReconstructionException;

    /**
     * put some object (file or directory) onto the servers
     */
    boolean putObject(FSObject obj);

    /**
     * remove an object from all servers
     */
    int deleteObject(FSObject obj);

    /**
     * retrieve meta-data for some object
     */
    Map<String, String> statObject(String path);

    /**
     * list objects according to some path criteria
     */
    Set<String> listObjects(String path);
}
