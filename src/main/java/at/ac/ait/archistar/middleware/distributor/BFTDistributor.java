package at.ac.ait.archistar.middleware.distributor;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.frontend.bftinterface.OzymandiasClient;
import at.ac.ait.archistar.middleware.commands.ReadCommand;
import at.ac.ait.archistar.middleware.commands.WriteCommand;
import at.archistar.bft.client.ClientResult;
import at.archistar.bft.messages.ClientCommand;

/**]
 * Implements the Distributor interface via a simple BFT protocol. For
 * test purposes this actually starts f+1 BFT servers on the local machine
 * and wires it up to a BFT network.
 * 
 * @author andy
 */
public class BFTDistributor implements Distributor {
		
	protected OzymandiasClient client;
	
	private boolean alreadyConnected = false;
	
	private int clientSequence = 0;
	
	private final int clientId = 0;
	
	private final int f = 1;

	private ServerConfiguration config;
	
	public BFTDistributor(ServerConfiguration config) {
		this.config = config;
		this.client = new OzymandiasClient(config.getBFTServerNetworkPortMap(), f, new NioEventLoopGroup());
	}

	@Override
	public int putFragmentSet(Set<Fragment> fragments) {
		
		Map<Integer, ClientCommand> msg = new HashMap<Integer, ClientCommand>();
		
		for(Fragment f : fragments) {
			msg.put(f.getStorageServer().getBFTId(), new WriteCommand(clientId, clientSequence, f.getFragmentId(), f.getData()));
		}
		
		this.client.sendRoundtripMessage(msg);
		clientSequence++;
		
		/* TODO: change this to go with the 2f+1 flow */
		return fragments.size();
	}

	@Override
	public int getFragmentSet(Set<Fragment> fragments) {
		
		Map<Integer, ClientCommand> msg = new HashMap<Integer, ClientCommand>();
		
		for(Fragment f : fragments) {
			msg.put(f.getStorageServer().getBFTId(), new ReadCommand(clientId, clientSequence, f.getFragmentId()));
		}
		
		ClientResult result = this.client.sendRoundtripMessage(msg);
		
		/* merge fragment results */
		for(Fragment f : fragments) {
			int bftid = f.getStorageServer().getBFTId();
			if (result.containsDataForServer(bftid)) {
				f.setData(result.getDataForServer(bftid));
				f.setSynchronized(true);
			} else {
				f.setSynchronized(false);
			}
		}
		
		clientSequence++;
		
		/* TODO: change this to go with the 2f+1 flow */
		return fragments.size();
	}

	/* starts up virtual servers */
	@Override
	public int connectServers() {
		
		if (alreadyConnected == true) {
			return config.getOnlineStorageServerCount();
		}

		/* create and connect client */
		try {
			this.client.connect();
			alreadyConnected = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return -1;
		}
		
		return config.getOnlineStorageServerCount();
	}

	@Override
	public int disconnectServers() {
		/* TODO: how to disconnect servers? */
		return 0;
	}
}
