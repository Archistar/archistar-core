package at.ac.ait.archistar.engine.serverinterface;

import at.ac.ait.archistar.backendserver.OzymandiasServer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLEngine;

import at.ac.ait.archistar.trustmanager.SSLContextFactory;
import at.archistar.bft.client.ClientResult;
import at.archistar.bft.client.ResultManager;
import at.archistar.bft.exceptions.InconsistentResultsException;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.TransactionResult;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslHandler;

/**
 * this is the main interface for Clients contacting replicas
 * 
 * @author andy
 */
public class OzymandiasClient {
	
	private Map<Integer, Integer> serverList;
	
	private Map<Integer, Channel> channelList;
    
    private int f = 1;
    
    private EventLoopGroup group;
    
    private ResultManager resultManager;
    
    public OzymandiasClient(Map <Integer, Integer> serverList, int f, NioEventLoopGroup group) {
        this.serverList = serverList;
        this.channelList = new HashMap<Integer, Channel>();
        this.f = f;
        this.group = group;
        this.resultManager = new ResultManager();
    }
    
    /**
     * Sends a message and waits for all replicas replies
     * 
     * @param msg the message to be sent
     * @return 
     */
    public ClientResult sendRoundtripMessage(Map<Integer, ClientCommand> msg) {

    	/* TODO: check if all clientIds and clientSequences are the same */
    	int clientId = msg.get(0).getClientId();
    	int clientSequence = msg.get(0).getClientSequence();
    	
    	/* create a new operation wait object */
    	ClientResult result = this.resultManager.addClientOperation(f, clientId, clientSequence);
    	
    	/* asynchronously send a message to all replicas */ 
    	for(Entry<Integer, ClientCommand> e: msg.entrySet()) {
    		channelList.get(e.getKey()).writeAndFlush(e.getValue());
    	}

    	/* wait for answers */
    	result.waitForEnoughAnswers();
    	
    	return result;
    }
    
    public void connect() throws Exception {
    	for(Entry<Integer, Integer> e : this.serverList.entrySet()) {
    		int serverId = e.getKey();
    		int serverPort = e.getValue();
    		
    		this.channelList.put(serverId, connectServer(serverPort));
    	}
    }
    
    private Channel connectServer(int port) throws Exception {
    	
    	final OzymandiasClientHandler handler = new OzymandiasClientHandler(this);
    	
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
            	
            	SSLEngine engine = SSLContextFactory.getClientContext().createSSLEngine();
            	engine.setUseClientMode(true);
            	
                ch.pipeline().addLast(
                		new SslHandler(engine),
                        new ObjectEncoder(),
                        new ObjectDecoder(OzymandiasServer.maxObjectSize, ClassResolvers.cacheDisabled(null)),
                        handler);
            }
         });
        
        return b.connect("127.0.0.1", port).sync().channel();
    }

    /**
     * adds a replica's result. This is used to determine when enough results for determining
     * an operations result were received
     * 
     * @param clientId the result's client id
     * @param clientSequence the result's client sequence
     * @param tx the result
     * @throws InconsistentResultsException seems like a faulty replica did send something unexpected
     */
	public void addReplicaResult(int clientId, int clientSequence, TransactionResult tx) throws InconsistentResultsException {
		resultManager.addClientResponse(clientId, clientSequence, tx);
	}
}
