package at.ac.ait.archistar.backendserver;

import at.archistar.bft.server.BftEngine;

public class PerformanceWatcher extends Thread {

	private BftEngine server;
	
	private boolean shutdown = false;
	
	public PerformanceWatcher(BftEngine server) {
		
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
				server.outputDebugInformation();
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