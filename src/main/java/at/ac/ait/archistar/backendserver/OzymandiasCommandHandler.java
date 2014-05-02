package at.ac.ait.archistar.backendserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.IntraReplicaCommand;
import at.archistar.bft.server.BftEngine;

/**
 * This handles all incoming commands (from clients or servers). Mostly all
 * commands are forwarded to the server process.
 *
 * @author andy
 */
@Sharable
public class OzymandiasCommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

    private Logger logger = LoggerFactory.getLogger(OzymandiasCommandHandler.class);

    private OzymandiasServer parentSystem;

    private BftEngine engine;

    public OzymandiasCommandHandler(OzymandiasServer ozzy, BftEngine engine) {
        super();
        this.parentSystem = ozzy;

        this.engine = engine;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) {

        logger.debug("server {} received {}", parentSystem.getReplicaId(), msg);

        if (msg instanceof IntraReplicaCommand) {
            this.engine.processIntraReplicaCommand((IntraReplicaCommand) msg);
        } else if (msg instanceof ClientCommand) {
            this.parentSystem.setClientSession(((ClientCommand) msg).getClientId(), ctx);
            this.engine.processClientCommand((ClientCommand) msg);
        } else {
            assert (false);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
