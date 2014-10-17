package at.ac.ait.archistar.engine;

import java.util.Map;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.engine.crypto.ArchistarSMCIntegrator;
import at.ac.ait.archistar.engine.dataobjects.CustomSerializer;
import at.ac.ait.archistar.engine.dataobjects.FSObject;
import at.ac.ait.archistar.engine.dataobjects.SimpleFileInterface;
import at.ac.ait.archistar.engine.distributor.Distributor;
import at.ac.ait.archistar.engine.distributor.ServerConfiguration;
import at.ac.ait.archistar.engine.metadata.MetadataService;
import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.exceptions.ReconstructionException;

/**
 * As most Archistar instances look kinda the same this class tries to capture
 * most of the similarities. This should prevent Archistar clients from
 * reimplementing the same functionality over and over again.
 *
 * @author andy
 */
public class Engine implements SimpleFileInterface {

    /**
     * our server configuration
     */
    protected final ServerConfiguration servers;

    /**
     * the distributor which will transfer requests over the BFT network to the
     * replicasS
     */
    protected final Distributor distributor;

    /**
     * our naming service
     */
    private final MetadataService metadataService;

    /**
     * cryptographic directives
     */
    private final CryptoEngine crypto;

    /**
     * the serializer used to tranform objects into byte arrays
     */
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
    public synchronized FSObject getObject(String path) throws ReconstructionException {

        Set<Fragment> fragments = this.metadataService.getDistributionFor(path);

        assertThat(fragments).hasSize(servers.getOnlineStorageServerCount());
        boolean result = this.distributor.getFragmentSet(fragments);

        // TODO: just assert that nothing went wrong..
        assertThat(result).isEqualTo(true);
        byte[] decrypted = ArchistarSMCIntegrator.decrypt(this.crypto, fragments);
        return this.serializer.deserialize(decrypted);
    }

    @Override
    public synchronized boolean putObject(FSObject obj) {
        assertThat(obj).isNotNull();
        assertThat(metadataService).isNotNull();

        Set<Fragment> fragments = this.metadataService.getDistributionFor(obj.getPath());

        byte[] serialized = this.serializer.serialize(obj);

        // encrypt the fragments
        Set<Fragment> encryptedData = ArchistarSMCIntegrator.encrypt(this.crypto, serialized, fragments);

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
