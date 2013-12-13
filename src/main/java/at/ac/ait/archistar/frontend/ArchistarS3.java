package at.ac.ait.archistar.frontend;

import java.io.File;
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
import at.ac.ait.archistar.frontend.s3.FakeBucket;
import at.ac.ait.archistar.frontend.s3.FakeRoot;
import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.middleware.distributor.BFTDistributor;
import at.ac.ait.archistar.middleware.distributor.Distributor;
import at.ac.ait.archistar.middleware.distributor.TestServerConfiguration;
import at.ac.ait.archistar.middleware.metadata.MetadataService;
import at.ac.ait.archistar.middleware.metadata.SimpleMetadataService;

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
		

		/* where to setup buckets/services? */
		ResteasyDeployment deployment = new ResteasyDeployment();
		
		List<Object> resources = new LinkedList<Object>();
		resources.add(new FakeBucket(engine));
		resources.add(new FakeRoot(engine));
		deployment.setResources(resources);
		
		netty = createServer(deployment);
		netty.start();
	}
	
	private static Set<StorageServer> createNewServers() {		
		File baseDir = new File("/tmp/test-ftp-filesystem/");
		baseDir.mkdirs();
			
		File dir1 = new File(baseDir, "1");
		dir1.mkdir();
		File dir2 = new File(baseDir, "2");
		dir2.mkdir();
		File dir3 = new File(baseDir, "3");
		dir3.mkdir();
		File dir4 = new File(baseDir, "4");
		dir4.mkdir();
			
		HashSet<StorageServer> servers = new HashSet<StorageServer>();
		servers.add(new FilesystemStorage(0, dir1));
		servers.add(new FilesystemStorage(1, dir2));
		servers.add(new FilesystemStorage(2, dir3));
		servers.add(new FilesystemStorage(3, dir4));
		return servers;
	}
	
	private static Engine createEngine() {
		TestServerConfiguration serverConfig = new TestServerConfiguration(createNewServers());
		
		serverConfig.setupTestServer(1);
		CryptoEngine crypto = new PseudoMirrorCryptoEngine();
		Distributor distributor = new BFTDistributor(serverConfig);
		MetadataService metadata = new SimpleMetadataService(serverConfig, distributor, crypto);
		return new Engine(serverConfig, metadata, distributor, crypto);
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