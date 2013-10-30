package at.ac.ait.archistar.frontend;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.mockftpserver.stub.StubFtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.ait.archistar.backendserver.storageinterface.FilesystemStorage;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.frontend.archistarftp.FakeDeleCommand;
import at.ac.ait.archistar.frontend.archistarftp.FakeLsCommand;
import at.ac.ait.archistar.frontend.archistarftp.FakeRetrCommand;
import at.ac.ait.archistar.frontend.archistarftp.FakeStorCommand;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.middleware.distributor.BFTDistributor;
import at.ac.ait.archistar.middleware.distributor.Distributor;
import at.ac.ait.archistar.middleware.distributor.TestServerConfiguration;
import at.ac.ait.archistar.middleware.metadata.MetadataService;
import at.ac.ait.archistar.middleware.metadata.SimpleMetadataService;

/**
 * This is a simple testclient that implements a fake FTP Server on port 30022.
 * 
 * @author andy
 */
public class ArchistarFTP {
	
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
		CryptoEngine crypto = new PseudoMirrorCryptoEngine(new CustomSerializer());
		Distributor distributor = new BFTDistributor(serverConfig);
		MetadataService metadata = new SimpleMetadataService(serverConfig, distributor);
		return new Engine(serverConfig, metadata, distributor, crypto);
	}
	
	public static void main(String[] args) {
		
		Logger logger = LoggerFactory.getLogger(ArchistarFTP.class);
		
		logger.info("Starting archistar storage engine");
		Engine engine = createEngine();
		engine.connect();
		
		int port =30022;
		logger.info("Starting FTP server on port " + port);
		StubFtpServer stubFtpServer = new StubFtpServer();
		stubFtpServer.setServerControlPort(30022);
		
		stubFtpServer.setCommandHandler("LIST", new FakeLsCommand(engine));
		stubFtpServer.setCommandHandler("RETR", new FakeRetrCommand(engine));
		stubFtpServer.setCommandHandler("STOR", new FakeStorCommand(engine));
		stubFtpServer.setCommandHandler("DELE", new FakeDeleCommand(engine));
		
		stubFtpServer.start();
	}
}