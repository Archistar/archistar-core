package at.ac.ait.archistar.bft.ozymandias.server;

public class PerformanceWatcher extends Thread {

	private OzymandiasServer server;
	
	private boolean shutdown = false;
	
	public PerformanceWatcher(OzymandiasServer server) {
		
		this.server = server;
		setDaemon(true); 
		start(); 
	}
	
	public void setShutdown() {
		this.shutdown = true;
	}
	
	public void run() {
		while(!this.shutdown) {
			
			server.checkCollections();
			
			if (server.isPrimary()) {
				server.outputTransactionCount();
				server.outputTiming();
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				if (!this.shutdown) {
					e.printStackTrace();
				}
			}
		}
	}
}