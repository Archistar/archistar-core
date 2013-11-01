package at.ac.ait.archistar.frontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.ait.archistar.backendserver.storageinterface.FilesystemStorage;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.middleware.distributor.BFTDistributor;
import at.ac.ait.archistar.middleware.distributor.Distributor;
import at.ac.ait.archistar.middleware.distributor.TestServerConfiguration;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;
import at.ac.ait.archistar.middleware.metadata.MetadataService;
import at.ac.ait.archistar.middleware.metadata.SimpleMetadataService;

/**
 * simple minimal test client that uses a filesystem watcher to capture
 * all create performed within a directory (NOTE: does not capture changes
 * within a subdirectory of that directory)
 * 
 * @author andy
 */
public class ArchistarWatcher {
	
	private static Set<StorageServer> createNewServers() {		
		File baseDir = new File("/tmp/test-filesystem/" + UUID.randomUUID() + "/");
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
	
	public static void main(String[] args) throws IOException {
		
		Logger logger = LoggerFactory.getLogger(ArchistarFTP.class);
		
		logger.info("Starting archistar storage engine");
		Engine engine = createEngine();
		engine.connect();
		
		File baseDir = new File("/tmp/user-filesystem/" + UUID.randomUUID() + "/");
		baseDir.mkdirs();
		
		System.out.println("Using directory: " + baseDir.getAbsolutePath());
			
		Path myDir = Paths.get(baseDir.getAbsolutePath());
		WatchService watcher = null;
		try {
			watcher = myDir.getFileSystem().newWatchService();
			myDir.register(watcher,  StandardWatchEventKinds.ENTRY_CREATE,
									 StandardWatchEventKinds.ENTRY_DELETE,
									 StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		while(true) {
			WatchKey watchKey = null;
			
			try {
				watchKey = watcher.take();
			} catch(InterruptedException x) {
				return;
			}
			
			for(WatchEvent<?> event: watchKey.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					logger.warn("overflow");
					continue;
				}
				
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>)event;
				Path filename = ev.context();
				
				Path file = myDir.resolve(filename);
				if (!Files.isRegularFile(file)) {
					continue;
				}
			
				System.out.println("filename: " + file.toString());
				System.out.println("type: " + kind.toString());
				
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					FileInputStream in = new FileInputStream(new File(baseDir, filename.toString()));
					byte[] data = IOUtils.toByteArray(in);
					FSObject obj = new SimpleFile(filename.getFileName().toString(), data, new HashMap<String, String>());
					engine.putObject(obj);
				} else {
					logger.error("not implemented yet");
				}
			}
			
			boolean valid = watchKey.reset();
			if(!valid) {
				break;
			}
		}
	}
}