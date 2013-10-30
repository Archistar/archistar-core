package at.ac.ait.archistar.middleware.crypto;

import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.frontend.FSObject;

/**
 * This just takes a user-supplied FSObject, serializes it
 * (using CustomSerializer) and fills in duplicates of the serialized
 * data into all fragments.
 * 
 * @author andy
 *
 */
public class PseudoMirrorCryptoEngine implements CryptoEngine {
	
	private CustomSerializer serializer;
	
	public PseudoMirrorCryptoEngine(CustomSerializer serializer) {
		this.serializer = serializer;
	}

	public FSObject decrypt(Set<Fragment> input) throws DecryptionException {
		
		for(Fragment f : input) {
			// TODO: test against the data of other fragments
			// TODO: do real encryption
			
			if (f.isSynchronized()) {
				FSObject result = serializer.deserialize(f.getData());
				return result;
			}
		}
		
		throw new DecryptionException();
	}

	public Set<Fragment> encrypt(FSObject data, Set<Fragment> fragments) {
		
        for(Fragment f : fragments) {
        	f.setData(serializer.serialize(data));
        }
		return fragments;
	}

}
