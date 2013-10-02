package at.ac.ait.archistar.bft.ozymandias.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import at.ac.ait.archistar.bft.ozymandias.commands.AbstractCommand;
import at.ac.ait.archistar.bft.ozymandias.commands.replica.AdvanceEraCommand;
import at.ac.ait.archistar.bft.ozymandias.commands.replica.CheckpointMessage;
import at.ac.ait.archistar.bft.ozymandias.commands.replica.IntraReplicaCommand;

/**
 * This handles all incoming commands (from clients or servers). Mostly
 * all commands are forwarded to the server process.
 * 
 * @author andy
 */
@Sharable
public class OzymandiasCommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

	private Logger logger = LoggerFactory.getLogger(OzymandiasCommandHandler.class);

    private OzymandiasServer parentSystem;
       
    public OzymandiasCommandHandler(OzymandiasServer ozzy) {
    	super();
    	this.parentSystem = ozzy;
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, AbstractCommand msg) {
    	
    	logger.debug("server {} received {}", parentSystem.getReplicaId(), msg);
    	
    	/** check if the message was sent during an older era/view */
    	if (msg instanceof IntraReplicaCommand) {
    		if (!this.parentSystem.checkEraOfMessage((IntraReplicaCommand) msg)) {
    			logger.warn("message from old era detected");
    			return;
    		}
    	}
    	
    	if (msg instanceof CheckpointMessage) {
    		this.parentSystem.addCheckpointMessage((CheckpointMessage)msg);
    	} else if (msg instanceof AdvanceEraCommand) {
    		this.parentSystem.advanceToEra(((AdvanceEraCommand)msg).getNewEra());
    	} else {
    		/* this locks t */
    		Transaction t = this.parentSystem.getTransaction(msg, ctx);
    	
    		this.parentSystem.handleMessage(t, msg);
    	
    		t.unlock();
    		this.parentSystem.cleanupTransactions(t);
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}