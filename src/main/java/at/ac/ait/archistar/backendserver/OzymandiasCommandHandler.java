package at.ac.ait.archistar.backendserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.IntraReplicaCommand;

/**
 * This is a netty.io message handler; it will be called for each retrieved
 * exception or message (on the bft server).
 *
 * note: I wanted to integrate this into OzymandiasServer but the attempt failed
 * due to some threading errors
 *
 * @author andy
 */
@Sharable
public class OzymandiasCommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

    private final Logger logger = LoggerFactory.getLogger(OzymandiasCommandHandler.class);

    /**
     * the replica this instance handles messages for
     */
    private final OzymandiasServer parentSystem;

    public OzymandiasCommandHandler(OzymandiasServer ozzy) {
        super();
        this.parentSystem = ozzy;
    }

    /**
     * retrieve one message and forward it to the corresponding server method
     *
     * @param ctx the context is used for establishing a return-channel for
     * signaling a client's operation result
     * @param msg the to be handled message
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) {

        logger.debug("server {} received {}", parentSystem.getReplicaId(), msg);

        if (msg instanceof IntraReplicaCommand) {
            this.parentSystem.processIntraReplicaCommand((IntraReplicaCommand) msg);
        } else if (msg instanceof ClientCommand) {
            this.parentSystem.setClientSession(((ClientCommand) msg).getClientId(), ctx);
            this.parentSystem.processClientCommand((ClientCommand) msg);
        } else {
            assert (false);
        }
    }

    /**
     * handle thrown exceptions
     *
     * TODO: actually handle them
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
