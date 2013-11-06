package at.ac.ait.archistar.middleware.crypto;

import java.security.GeneralSecurityException;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.Fragment.EncryptionScheme;
import at.archistar.crypto.SecretSharing;
import at.archistar.crypto.WeakSecurityException;
import at.archistar.crypto.data.Share;
import at.archistar.helper.ShareSerializer;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.frontend.FSObject;

public class SecretSharingCryptoEngine implements CryptoEngine {
	
	private final CustomSerializer serializer;
	
	private final SecretSharing sharingAlgorithm;
	
	public SecretSharingCryptoEngine(CustomSerializer serializer, SecretSharing sharingAlgorithm) {
		this.serializer = serializer;
		this.sharingAlgorithm = sharingAlgorithm; 
	}

	public FSObject decrypt(Set<Fragment> input) throws DecryptionException {
		
		Share[] shares = new Share[input.size()];
		
		int i = 0;
		for(Fragment f: input) {
			shares[i++] = ShareSerializer.deserializeShare(f.getData());
		}
		
		byte[] combined = null;
		try {
			combined = this.sharingAlgorithm.reconstruct(shares);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		return serializer.deserialize(combined);
	}

	public Set<Fragment> encrypt(FSObject data, Set<Fragment> fragments) {
	
		byte[] originalContent = serializer.serialize(data);
		
		try {
			Share[] shares = this.sharingAlgorithm.share(originalContent);
			Fragment[] fs = fragments.toArray(new Fragment[0]);
			assertThat(fs.length == shares.length);
			assertThat(fragments.size() == 4);
			
			for(int i=0; i < fs.length; i++) {
				
				Fragment f = fs[i];
				
				byte[] binData = ShareSerializer.serializeShare(shares[i]);
				assert(binData != null);
				assert(binData.length != 0);
				
				f.setData(binData);
				f.setEncryptionScheme(EncryptionScheme.SHAMIR);
			}
		} catch (WeakSecurityException e) {
			e.printStackTrace();
			assert(false);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			assert(false);
		}
		return fragments;
	}
}