package at.ac.ait.archistar.engine.userinterface;

import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.ait.archistar.backendserver.storageinterface.FilesystemStorage;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.engine.Engine;
import at.ac.ait.archistar.engine.distributor.BFTDistributor;
import at.ac.ait.archistar.engine.distributor.Distributor;
import at.ac.ait.archistar.engine.distributor.TestServerConfiguration;
import at.ac.ait.archistar.engine.metadata.MetadataService;
import at.ac.ait.archistar.engine.metadata.SimpleMetadataService;
import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.RabinBenOrEngine;
import at.archistar.crypto.exceptions.WeakSecurityException;
import at.archistar.crypto.random.FakeRandomSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.NoSuchAlgorithmException;

/**
 * this bootstrap a local S3 archistar instance containing one archistar
 * director with an client-side S3/HTTP/Rest interface and four backend-storage
 * servers utilizing filesystem based storage.
 * 
 * Note: maybe we should exchange the filesystem based storage with a pure
 * memory based storage as this would remove the need for writable filesystems
 * for the test case
 * 
 * @author andy
 */
public class ArchistarS3 {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        NettyJaxrsServer netty;

        Logger logger = LoggerFactory.getLogger(ArchistarS3.class);

        logger.info("Starting archistar storage engine");
        Engine engine = createEngine();
        engine.connect();


        /* setup buckets/services? */
        ResteasyDeployment deployment = new ResteasyDeployment();

        HashMap<String, FakeBucket> buckets = new HashMap<>();
        buckets.put("fake_bucket", new FakeBucket(engine));

        List<Object> resources = new LinkedList<>();
        resources.add(new FakeRoot(buckets));
        deployment.setResources(resources);

        List<Object> providers = new LinkedList<>();
        providers.add(new RedirectorFilter());
        deployment.setProviders(providers);

        netty = createServer(deployment);
        netty.start();
    }
    
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static final String BASE_PATH = "/var/spool/archistar/test-s3/";
    
    private static void createDir(Set<StorageServer> servers, File baseDir, int subdir) {
        File dir1 = new File(baseDir, Integer.toString(subdir + 1));
        if (!dir1.exists()) {
            assert(dir1.mkdir());
        }
        servers.add(new FilesystemStorage(subdir, dir1));
    }
    
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static Set<StorageServer> createNewServers() {
        File baseDir = new File(BASE_PATH);
        
        if (!baseDir.exists()) {
            assert(baseDir.mkdirs());
        }

        HashSet<StorageServer> servers = new HashSet<>();
        
        for (int i=0; i < 4; i++) {
            createDir(servers, baseDir, i);
        }
        return servers;
    }

    private static Engine createEngine() {
        NioEventLoopGroup loopGroup = new NioEventLoopGroup(16);

        TestServerConfiguration serverConfig = new TestServerConfiguration(createNewServers(), loopGroup);

        serverConfig.setupTestServer(1);
        try {
            CryptoEngine crypto = new RabinBenOrEngine(4, 3, new FakeRandomSource());
            Distributor distributor = new BFTDistributor(serverConfig, loopGroup);
            MetadataService metadata = new SimpleMetadataService(serverConfig, distributor, crypto);
            return new Engine(serverConfig, metadata, distributor, crypto);
        } catch (NoSuchAlgorithmException | WeakSecurityException ex) {
            assert(false);
        }
        return null;
    }

    private static NettyJaxrsServer createServer(ResteasyDeployment deployment) throws Exception {
        NettyJaxrsServer netty = new NettyJaxrsServer();
        netty.setDeployment(deployment);
        netty.setPort(8080);
        netty.setRootResourcePath("");
        netty.setSecurityDomain(null);
        return netty;
    }
}
