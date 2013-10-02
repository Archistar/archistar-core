package at.ac.ait.archistar.bft.ozymandias.server;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.ait.archistar.SecurityMonitor;
import at.ac.ait.archistar.bft.ozymandias.InconsistentResultsException;
import at.ac.ait.archistar.bft.ozymandias.commands.AbstractCommand;
import at.ac.ait.archistar.bft.ozymandias.commands.client.*;
import at.ac.ait.archistar.bft.ozymandias.commands.replica.*;
import at.ac.ait.archistar.bft.ozymandias.faketls.SSLContextFactory;
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
public class OzymandiasServer implements Runnable {

	private final int serverId;
	private final Map<Integer, Integer> serverList;
    private final int port;
    
    /** used for server-to-server communication */
    private ServerServerCommunication servers;
    
    private int maxSequence = 0;
    private int f = 1;
    private int viewNr = 0;
     
    /** (client-side operation id) -> transaction mapping */
    private SortedMap<String, Transaction> collClientId;
    
    /** (internal id aka. sequence) -> transaction mapping */
    private SortedMap<Integer, Transaction> collSequence;
    
    final private ReentrantLock lockCollections = new ReentrantLock();
    
    /** used to output performance numbers every couple of seconds */
    private PerformanceWatcher watcher = null;
    
    private ExecutionHandler executor;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;
    private ServerBootstrap b;
    
    private final CheckpointManager checkpoints;
    
    private final SecurityMonitor secMonitor;
    
	private Logger logger = LoggerFactory.getLogger(OzymandiasServer.class);

    public OzymandiasServer(int myServerId, Map<Integer, Integer> serverList, int f, ExecutionHandler executor, NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
            	
        this.f = f;
    	this.serverList = serverList;
    	this.serverId = myServerId;
        this.port = this.serverList.get(serverId);
        this.servers = new ServerServerCommunication(serverId, this.serverList, this.workerGroup);
        this.collClientId = new TreeMap<String, Transaction>();
        this.collSequence = new TreeMap<Integer, Transaction>();
        this.executor = executor;
        this.secMonitor = new SecurityMonitor(this);
        this.checkpoints = new CheckpointManager(myServerId, servers, secMonitor, f);
    }
    
    public boolean checkEraOfMessage(IntraReplicaCommand cmd) {
    	return cmd.getViewNr() >= viewNr;
    }
    
    public void run() {
    	/* start up netty listener */        
        final OzymandiasCommandHandler handler = new OzymandiasCommandHandler(this);
        
        watcher = new PerformanceWatcher(this);
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
    
	public boolean isPrimary() {
		return this.serverId == (viewNr % (3f+1));
	}
	
	private int getPriorSequenceNumber(String fragmentId) {
		int priorSequence = -2;
		
		/* TODO: there could be sequence commands without fragment (bad timing...) */
		for (Transaction x : this.collSequence.values()) {
			if (fragmentId.equals(x.getFragmentId()) || x.getFragmentId() == null) {
				priorSequence = Math.max(priorSequence, x.getSequenceNr());
			}
		}
		
		return priorSequence;
	}

	private double lifetime = 0;
	private double count = 0;
	private int lastCommited = -1;
	
	public void outputTiming() {
		logger.info("server: {} transaction length: {}ms", this.serverId, Math.round(lifetime/count));
	}
	
	private void checkCollectionsUnlocked() {
		if (collClientId.size() >= 100 || collSequence.size() >= 100) {
			logger.info("server: {} collClient: {} collSequence: {}", this.serverId, collClientId.size(), collSequence.size());
		}
	}
	
	public Transaction getTransaction(AbstractCommand msg, ChannelHandlerContext ctx) {
		
		lockCollections.lock();
		Transaction result = null;
		
		if (msg instanceof ClientFragmentCommand) {
			ClientFragmentCommand c = (ClientFragmentCommand)msg;
			String clientOperationId = c.getClientOperationId();
			
			if (collClientId.containsKey(clientOperationId)) {
				/* there was already a preprepare request */
				result = collClientId.get(clientOperationId);
			} else {
				/* first request */
				result = new Transaction(c, this, f);
				collClientId.put(c.getClientOperationId(), result);
			}
			
			result.addClientCommand(c, ctx);
			
			if (isPrimary()) {
				result.setDataFromPreprepareCommand(maxSequence++, getPriorSequenceNumber(c.getFragmentId()));
				collSequence.put(result.getSequenceNr(), result);
	        	PreprepareCommand seq = result.createPreprepareCommand();
	        	servers.send(seq);
			}
		} else if (msg instanceof PreprepareCommand) {
			PreprepareCommand c = (PreprepareCommand)msg;
			
			String clientOperationId = c.getClientOperationId();
			int sequence = c.getSequence();
			
			boolean knownFromClientOpId = collClientId.containsKey(clientOperationId);
			boolean knownFromSequence = collSequence.containsKey(sequence);

			if (knownFromClientOpId && knownFromSequence) {
				result = collClientId.get(clientOperationId);
				result.merge(collSequence.get(sequence));
			} else if (knownFromClientOpId && !knownFromSequence) {
				result = collClientId.get(clientOperationId);
				result.setDataFromPreprepareCommand(sequence, c.getPriorSequence());
			} else if (!knownFromClientOpId && knownFromSequence) {
				result = collSequence.get(sequence);
				result.setClientOperationId(clientOperationId);
			} else {
				/* initial network package */
				result = new Transaction(c, this, f);
			}
			
			if (!isPrimary()) {
				result.setDataFromPreprepareCommand(sequence, c.getPriorSequence());
			}
			
			/* after the prepare command the transaction should be known by both client-operation-id
			 * as well as by the bft-internal sequence number */
			result.setPrepreparedReceived();
			collSequence.put(sequence, result);
			collClientId.put(clientOperationId, result);
		} else if (msg instanceof PrepareCommand) {
			PrepareCommand c = (PrepareCommand)msg;
			
			int sequence = c.getSequence();
			
			if (collSequence.containsKey(sequence)) {
				result = collSequence.get(sequence);
			} else {
				result = new Transaction(c, this, f);
				collSequence.put(sequence, result);
			}
			
			try {
				result.addPrepareCommand(c);
			} catch(InconsistentResultsException e) {
				secMonitor.replicasMightBeMalicous();
			}
		} else if (msg instanceof CommitCommand) {
			CommitCommand c = (CommitCommand)msg;
			result = collSequence.get(c.getSequence());
			result.addCommitCommand(c);
		} else {
			secMonitor.invalidMessageReceived(msg);
		}
		
		result.lock();
		
		lockCollections.unlock();
		return result;
	}
	
	public void send(AbstractCommand cmd) {
		this.servers.send(cmd);
	}
	
	public void handleMessage(Transaction t, AbstractCommand msg) {
    	
		t.tryAdvanceToPreprepared(isPrimary(), servers);
		t.tryAdvanceToPrepared(lastCommited, servers);
		if (t.tryAdvanceToCommited()) {
			/* check if we should send a CHECKPOINT message */
			checkpoints.addTransaction(t, t.getResult(), viewNr);
		
			lastCommited  = Math.max(t.getSequenceNr(), lastCommited);
			
			/* This just outputs some stupid debug information about latencies */
			count++;
			lifetime += t.getLifetime();
		}
    	t.tryMarkDelete();
	}

	public void cleanupTransactions(Transaction mightDelete) {
		
		this.lockCollections.lock();
		
		if (mightDelete.tryMarkDelete()) {
			mightDelete.lock();
			collClientId.remove(mightDelete.getClientOperationId());
			collSequence.remove(mightDelete.getSequenceNr());
			/* free transaction */
			mightDelete.unlock();
		}
		
		/* search for preparable and commitable transactions */
		Iterator<Transaction> it = collSequence.values().iterator();
		while(it.hasNext()) {
			Transaction x = it.next();
			
			x.lock();
			
			if (x.tryAdvanceToPrepared(lastCommited, servers)) {
				lastCommited = Math.max(lastCommited, x.getPriorSequenceNr());
				
				if (x.tryAdvanceToCommited()) {
					/* check if we should send a CHECKPOINT message */
					checkpoints.addTransaction(x, x.getResult(), viewNr);
		
					lastCommited  = Math.max(x.getSequenceNr(), lastCommited);
			
					/* This just outputs some stupid debug information about latencies */
					count++;
					lifetime += x.getLifetime();
				}
				
				if(x.tryMarkDelete()) {
					collClientId.remove(x.getClientOperationId());
					it.remove();
				}
			}
			x.unlock();
		}
		this.lockCollections.unlock();
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
	
	public void addCheckpointMessage(CheckpointMessage msg) {
		this.checkpoints.addCheckpointMessage(msg);
	}

	public void outputTransactionCount() {
		logger.info("successful transactions: {}", count);
	}

	public void checkCollections() {
		lockCollections.lock();
		checkCollectionsUnlocked();
		lockCollections.unlock();
	}

	public ExecutionHandler getExecutionHandler() {
		return this.executor;
	}

	public void advanceToEra(int era) {
		lockCollections.lock();
		if (this.viewNr <= era) {
			logger.warn("already in era {}", era);
		} else {
			this.viewNr = era;
			
			/* remove all non-client transactions and reset all client-ones */
			Iterator<Transaction> it = collSequence.values().iterator();
			while(it.hasNext()) {
				Transaction t = it.next();
			
				t.lock();
				if (t.hasClientInteraction()) {
					/* sets state to INCOMING, clears collections */
					t.reset();
					
					if (isPrimary()) {
						servers.send(t.createPreprepareCommand());
					
						/* generates pre-prepare commands and sets state to prepared */
						t.tryAdvanceToPreprepared(isPrimary(), servers);
					}
				} else {
					/* delete transaction */
					collClientId.remove(t.getClientOperationId());
					it.remove();
				}
				
				t.unlock();
			}
		}
		lockCollections.unlock();
	}

	public void tryAdvanceEra() {
		AdvanceEraCommand cmd = new AdvanceEraCommand(serverId, -1, viewNr, viewNr+1);
		servers.send(cmd);
		advanceToEra(viewNr+1);
	}
}