package at.ac.ait.archistar.middleware.distributor;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import at.ac.ait.archistar.backendserver.OzymandiasServer;
import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.middleware.distributor.ServerConfiguration;

/** this adds support for creating/stopping test servers as well as a couple of debug
 *  routines for writing test cases
 * 
 * @author andy
 */
public class TestServerConfiguration extends ServerConfiguration {
	
	private HashSet<OzymandiasServer> servers;
	
	private Thread[] replicas;
	
	public TestServerConfiguration(Set<StorageServer> servers) {
		super(servers);
	}
	
	/** TODO: should we move the setupTestServer / teardownTestServer methods into a subclass? */
	public void setupTestServer(int f) {
		
		int i = 0;
		int servercount = serverMapId.size();
		
    	replicas = new Thread[servercount];
    	servers = new HashSet<OzymandiasServer>();
		
		for(StorageServer s : serverMapId.values()) {
			s.connect();
			
    		OzymandiasServer tmp = new OzymandiasServer(s.getBFTId(), getBFTServerNetworkPortMap(), f, s, new NioEventLoopGroup(), new NioEventLoopGroup());
    		servers.add(tmp);
    		Thread thr = new Thread(tmp);
    		replicas[i++] = thr;
    		thr.start();
		}
		
		/* connect servers */
    	for(OzymandiasServer o: this.servers) {
    		try {
				o.connectServers();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public void teardownTestServer() {
		for(OzymandiasServer s : servers) {
    		s.shutdown();
    	}
    	
    	for(Thread s : replicas) {
    		try {
				s.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public HashMap<String, Integer> getStorageFragmentCounts() throws DisconnectedException {
		
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		
		for(StorageServer s : getOnlineStorageServers()) {
			result.put(s.getId(), s.getFragmentCount());
		}
		
		return result;
	}
}
