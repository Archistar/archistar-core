package at.ac.ait.archistar.engine.distributor;

import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;

/**
 * Distributor is responsible of directing the different operations to
 * the different servers. Possible solutions range from simple linear
 * sequential distributions to BFT algorithms
 * 
 * Operations always perform on sets (as we seldomly want to access just
 * a single fragment on one server)
 * 
 * @author andy
 */
public interface Distributor {
	
	int putFragmentSet(Set<Fragment> fragments);
	
	public int getFragmentSet(Set<Fragment> fragments);
		
	int connectServers();
	
	int disconnectServers();
}
