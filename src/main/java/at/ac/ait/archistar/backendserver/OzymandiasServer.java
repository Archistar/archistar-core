package at.ac.ait.archistar.backendserver;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;
import at.ac.ait.archistar.bft.BftEngine;
import at.ac.ait.archistar.bft.BftEngineCallbacks;
import at.ac.ait.archistar.bft.checkpointing.CheckpointMessage;
import at.ac.ait.archistar.bft.commands.AbstractCommand;
import at.ac.ait.archistar.bft.commands.ClientCommand;
import at.ac.ait.archistar.bft.commands.IntraReplicaCommand;
import at.ac.ait.archistar.middleware.commands.ReadCommand;
import at.ac.ait.archistar.middleware.commands.TransactionResult;
import at.ac.ait.archistar.middleware.commands.WriteCommand;
import at.ac.ait.archistar.trustmanager.SSLContextFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslHandler;

/**
 * This is the main application logic of the BFT server.
 * 
 * @author andy
 */
public class OzymandiasServer implements Runnable, BftEngineCallbacks {

	private final int serverId;
	private final Map<Integer, Integer> serverList;
    private final int port;
    
    private Map<Integer, ChannelHandlerContext> clientMap = new HashMap<Integer, ChannelHandlerContext>();
    
    /** used for server-to-server communication */
    private ServerServerCommunication servers;
    
    /** used to output performance numbers every couple of seconds */
    private PerformanceWatcher watcher = null;
    
    private ExecutionHandler executor;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;
    private ServerBootstrap b;
    
    private BftEngine bftEngine;
    
    private final SecurityMonitor secMonitor;
    
    public OzymandiasServer(int myServerId, Map<Integer, Integer> serverList, int f, ExecutionHandler executor, NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
            	
    	this.serverList = serverList;
    	this.serverId = myServerId;
        this.port = this.serverList.get(serverId);
        this.servers = new ServerServerCommunication(serverId, this.serverList, this.workerGroup);
        this.executor = executor;
        this.secMonitor = new SecurityMonitor(this);
        this.bftEngine = new BftEngine(serverId, f, this);
    }
    
    public void run() {
    	/* start up netty listener */        
        final OzymandiasCommandHandler handler = new OzymandiasCommandHandler(this, bftEngine);
        
        watcher = new PerformanceWatcher(bftEngine);
        try {
            b = new ServerBootstrap();
                        
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                	SSLEngine engine = SSLContextFactory.getServerContext().createSSLEngine();
                	engine.setUseClientMode(false);
                	
                    ch.pipeline().addLast(
                    		new SslHandler(engine),
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            handler);
                }
             }).option(ChannelOption.SO_BACKLOG,  128)
               .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Bind and start to accept incoming connections.
            serverChannel = b.bind(port).sync();
            
            // wait until the server socket is closed
            serverChannel.channel().closeFuture().sync();
        } catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void connectServers() throws InterruptedException {
    	this.servers.connect();
    }
    
	
	public void send(AbstractCommand cmd) {
		this.servers.send(cmd);
	}
	
	public void shutdown() {
		try {
			this.watcher.setShutdown();
			this.watcher.interrupt();
			this.watcher.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ChannelFuture cf = serverChannel.channel().close();
		cf.awaitUninterruptibly();
	}

	public int getReplicaId() {
		return this.serverId;
	}
	
	public ExecutionHandler getExecutionHandler() {
		return this.executor;
	}
	
	public byte[] executeClientCommand(ClientCommand cmd) {
		byte[] binResult = null;
		
		try {
			if(cmd instanceof WriteCommand) {
				byte[] data = ((WriteCommand) cmd).getData();
				binResult = this.executor.putBlob(((WriteCommand) cmd).getFragmentId(), data);
			} else if (cmd instanceof ReadCommand) {
				binResult = this.executor.getBlob(((ReadCommand) cmd).getFragmentId());
			}
		} catch (DisconnectedException ex) {
			assert(false);
		}
		return binResult;
	}

	public BftEngine getBftEngine() {
		return this.bftEngine;
	}

	@Override
	public void invalidMessageReceived(AbstractCommand msg) {
		this.secMonitor.invalidMessageReceived(msg);
		
	}

	@Override
	public void replicasMightBeMalicous() {
		this.secMonitor.replicasMightBeMalicous();
	}

	@Override
	public void sendToReplicas(IntraReplicaCommand cmd) {
		this.servers.send(cmd);
	}

	@Override
	public void invalidCheckpointMessage(CheckpointMessage msg) {
		this.secMonitor.invalidCheckpointMessage(msg);
	}

	@Override
	public void answerClient(TransactionResult transactionResult) {
		ChannelHandlerContext ctx = this.clientMap.get(transactionResult.getClientId());
		ctx.write(transactionResult);
	}

	public void setClientSession(int clientId, ChannelHandlerContext ctx) {
		if (!this.clientMap.containsKey(clientId)) {
			this.clientMap.put(clientId, ctx);
		}
	}
}