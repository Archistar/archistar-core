package at.ac.ait.archistar.backendserver;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLEngine;

import at.ac.ait.archistar.trustmanager.SSLContextFactory;
import at.archistar.bft.messages.AbstractCommand;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class is responsible for sending messages to all replicas currently
 * within the replica system.
 *
 * @author andy
 */
public class ServerServerCommunication {

    /**
     * mapping from replica id -> port. Note: currently only connections to localhost are supported
     */
    private final Map<Integer, Integer> serverList;

    /**
     * opened channels
     */
    private final Set<Channel> channels;

    /**
     * my server/replica Id -- this is used so that we are not connecting to
     * ourself
     */
    private int myServerId = -1;

    private EventLoopGroup loopGroup = null;

    public ServerServerCommunication(int myId, Map<Integer, Integer> serverList, EventLoopGroup elg) {
        this.serverList = serverList;
        this.channels = new HashSet<>();
        this.myServerId = myId;
        this.loopGroup = elg;
    }

    /**
     * connects to all replicas
     *
     * @throws InterruptedException
     */
    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    public void connect() throws InterruptedException {
        for (Entry<Integer, Integer> e : this.serverList.entrySet()) {
            int replicaId = e.getKey();
            int replicaPort = e.getValue();

            if (replicaId != myServerId) {
                Bootstrap b = new Bootstrap();
                b.group(loopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                // enable SSL/TLS support
                                SSLEngine engine = SSLContextFactory.getClientContext().createSSLEngine();
                                engine.setUseClientMode(true);

                                ch.pipeline().addLast(
                                        new SslHandler(engine),
                                        new ObjectEncoder(),
                                        new ObjectDecoder(OzymandiasServer.maxObjectSize, ClassResolvers.cacheDisabled(null)));
                            }
                        });

                /* wait till server is connected */
                ChannelFuture f = null;
                do {
                    f = b.connect("127.0.0.1", replicaPort);
                    f.await();
                } while (!(f.isDone() && f.isSuccess()));

                this.channels.add(f.sync().channel());
            }
        }
    }

    /**
     * sends a message to all connected replicas
     *
     * @param cmd the to be sent message
     */
    public void send(AbstractCommand cmd) {
        for (Channel channel : this.channels) {
            channel.writeAndFlush(cmd);
        }
    }
}
