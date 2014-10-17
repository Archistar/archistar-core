package at.ac.ait.archistar.backendserver;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import at.ac.ait.archistar.backendserver.storageinterface.DisconnectedException;
import at.ac.ait.archistar.engine.messages.ReadCommand;
import at.ac.ait.archistar.engine.messages.WriteCommand;
import at.ac.ait.archistar.trustmanager.SSLContextFactory;
import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.CheckpointMessage;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.IntraReplicaCommand;
import at.archistar.bft.messages.TransactionResult;
import at.archistar.bft.server.BftEngine;
import at.archistar.bft.server.BftEngineCallbacks;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
 * This is the main archistar/bft server application logic
 *
 * @author andy
 */
public class OzymandiasServer implements Runnable, BftEngineCallbacks {

    /**
     * my internal server id
     */
    private final int serverId;

    /**
     * a serverid -> port mapping for our servers
     */
    private final Map<Integer, Integer> serverList;

    /**
     * upon which port is the server listening
     */
    private final int port;

    private final Map<Integer, ChannelHandlerContext> clientMap = new HashMap<>();

    /**
     * used for server-to-server communication
     */
    private final ServerServerCommunication servers;

    /**
     * the executor is responsible for actually performing data I/O operations
     * on the server
     */
    private final ExecutionHandler executor;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    /**
     * this is the listening server channel (will be configured to use the
     * configured server port)
     */
    private ChannelFuture serverChannel;

    /**
     * this encapsulates the server's BFT state. It is responsible for creating
     * a distributed "shared" state between all replicas/servers
     */
    private final BftEngine bftEngine;

    private final SecurityMonitor secMonitor;

    /**
     * max message size in bytes
     */
    public final static int maxObjectSize = 10 * 1024 * 1024;

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

    /**
     * setup the server listening ports and handler routines
     */
    @Override
    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    public void run() {
        /* start up netty listener */
        final OzymandiasCommandHandler handler = new OzymandiasCommandHandler(this);

        try {
            ServerBootstrap b = new ServerBootstrap();

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
                                    new ObjectDecoder(maxObjectSize, ClassResolvers.cacheDisabled(null)),
                                    handler);
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
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

    /**
     * connect to all configured BFT replicas
     * @throws java.lang.InterruptedException
     */
    public void connectServers() throws InterruptedException {
        this.servers.connect();
    }

    /**
     * shutdown server process (listeners and handlers
     */
    public void shutdown() {
        ChannelFuture cf = serverChannel.channel().close();
        cf.awaitUninterruptibly();
    }

    /**
     * return the servers id
     * 
     * @return server replica id
     */
    public int getReplicaId() {
        return this.serverId;
    }

    /**
     * this handler is called by the BFT engine as soon as a command can be
     * executed upon a replica (as it was received on enough replicas in the
     * same order)
     * 
     * @param cmd the to be executed command
     * @return the executed commands result
     */
    @Override
    public byte[] executeClientCommand(ClientCommand cmd) {
        byte[] binResult = null;

        try {
            if (cmd instanceof WriteCommand) {
                byte[] data = ((WriteCommand) cmd).getData();
                binResult = this.executor.putBlob(((WriteCommand) cmd).getFragmentId(), data);
            } else if (cmd instanceof ReadCommand) {
                binResult = this.executor.getBlob(((ReadCommand) cmd).getFragmentId());
            }
        } catch (DisconnectedException ex) {
            assert (false);
        }
        return binResult;
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
        ctx.writeAndFlush(transactionResult);
    }

    public void setClientSession(int clientId, ChannelHandlerContext ctx) {
        if (!this.clientMap.containsKey(clientId)) {
            this.clientMap.put(clientId, ctx);
        }
    }

    void processIntraReplicaCommand(IntraReplicaCommand intraReplicaCommand) {
        this.bftEngine.processIntraReplicaCommand(intraReplicaCommand);
    }

    void processClientCommand(ClientCommand clientCommand) {
        this.bftEngine.processClientCommand(clientCommand);
    }

    void tryAdvanceEra() {
        this.bftEngine.tryAdvanceEra();
    }
}
