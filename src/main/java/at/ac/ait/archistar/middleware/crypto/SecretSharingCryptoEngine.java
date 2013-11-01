package at.ac.ait.archistar.middleware.crypto;

import java.security.GeneralSecurityException;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.Fragment.EncryptionScheme;
import at.ac.ait.archistar.crypto.SecretSharing;
import at.ac.ait.archistar.crypto.ShamirPSS;
import at.ac.ait.archistar.crypto.WeakSecurityException;
import at.ac.ait.archistar.crypto.data.Share;
import at.ac.ait.archistar.crypto.random.FakeRandomSource;
import at.ac.ait.archistar.crypto.random.RandomSource;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.frontend.FSObject;

public class SecretSharingCryptoEngine implements CryptoEngine {
	
	private final CustomSerializer serializer;
	
	private final int n = 4;
	
	private final int k = 3;
	
	private final RandomSource rng = new FakeRandomSource();
	
	private final SecretSharing sharingAlgorithm;
	
	public SecretSharingCryptoEngine(CustomSerializer serializer) {
		this.serializer = serializer;
		this.sharingAlgorithm =  new ShamirPSS(n, k, rng);
	}

	public FSObject decrypt(Set<Fragment> input) throws DecryptionException {
		
		for(Fragment f: input) {
			assertThat(f.getEncryptionScheme()).isEqualTo(EncryptionScheme.SHAMIR);
			assertThat(f.getData() != null && f.getData().length != 0);
			assertThat(f.getMetaData("xValue")).isGreaterThanOrEqualTo(0);
			assertThat(f.getMetaData("xValue")).isLessThanOrEqualTo(4);
		}
		assertThat(input.size() == 4);
		
		Share[] shares = new Share[4];
		Fragment[] f = input.toArray(new Fragment[0]);
		
		assert(shares.length == f.length);
		
		for(int i=0; i < shares.length; i++) {
			/* create share */
			byte[] shareContent = f[i].getData();
			shares[i] = new Share(f[i].getMetaData("xValue"), shareContent, null, f[i].getMetaData("ContentLength"), Share.Type.SHAMIR);
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
				
				byte[] binData = shares[i].yValues;
				assert(binData != null);
				assert(binData.length != 0);
				f.setData(binData);
				f.setMetaData("xValue", shares[i].xValue);
				f.setMetaData("ContentLength", shares[i].contentLength);
				f.setEncryptionScheme(EncryptionScheme.SHAMIR);
			}
		} catch (WeakSecurityException e) {
			e.printStackTrace();
			assert(false);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			assert(false);
		}
		
		assert(fragments.size() > 0);
		for(Fragment f : fragments) {
			assert(f.getData() != null);
			assert(f.getData().length != 0);
		}
		
		return fragments;
	}

}
