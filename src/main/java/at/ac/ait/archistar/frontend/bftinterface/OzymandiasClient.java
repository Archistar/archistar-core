package at.ac.ait.archistar.frontend.bftinterface;

import at.ac.ait.archistar.backendserver.OzymandiasServer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLEngine;

import at.ac.ait.archistar.bft.commands.AbstractCommand;
import at.ac.ait.archistar.bft.commands.ClientCommand;
import at.ac.ait.archistar.middleware.InconsistentResultsException;
import at.ac.ait.archistar.middleware.commands.TransactionResult;
import at.ac.ait.archistar.trustmanager.SSLContextFactory;
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
    
    private Channel channel;
    
    private Bootstrap b;
    
    private int f = 1;
    
    private EventLoopGroup group;
    
    private Map<Integer, ClientResult> results;

    public OzymandiasClient(Map <Integer, Integer> serverList, int f, NioEventLoopGroup group) {
        this.serverList = serverList;
        this.channelList = new HashMap<Integer, Channel>();
        this.results = new HashMap<Integer, ClientResult>();
        this.f = f;
        this.group = group;
    }
    
    /**
     * asynchronously sends a message to all replicas
     * 
     * @param msg the message to be sent
     */
    public void sendMessage(Map<Integer, AbstractCommand> msg) {
    	for(Entry<Integer, AbstractCommand> e: msg.entrySet()) {
    		channelList.get(e.getKey()).write(e.getValue());
    	}
    }
    
    /**
     * Sends a message and waits for all replicas replies
     * 
     * @param msg the message to be sent
     * @return 
     */
    public ClientResult sendRoundtripMessage(Map<Integer, ClientCommand> msg) {
    	
    	int clientId = msg.get(0).getClientId();
    	int clientSequence = msg.get(0).getClientSequence();
    	ClientResult result = new ClientResult(f, clientId, clientSequence);
   		this.results.put(clientSequence, result);
   		
    	for(Entry<Integer, ClientCommand> e: msg.entrySet()) {
    		channelList.get(e.getKey()).write(e.getValue());
    	}
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
    	
        b = new Bootstrap();
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

    public void run() throws Exception {
    	try {
            // Start the connection attempt.
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
    
    /**
     * 
     * @param i 
     * @param clientId for which client operation should be waited
     * @param msg 
     * 
     * @return true if 3f+1 messages were received
     * @throws InconsistentResultsException 
     */
	public boolean positiveResultCountReached(int clientId, int clientSequence, TransactionResult tx) throws InconsistentResultsException {
		return this.results.get(clientSequence).addResult(clientId, clientSequence, tx);
	}
}
