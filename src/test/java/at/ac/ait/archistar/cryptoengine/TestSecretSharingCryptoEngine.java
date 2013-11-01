package at.ac.ait.archistar.cryptoengine;

import static org.mockito.Mockito.*;
import static org.fest.assertions.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.RemoteFragment;
import at.ac.ait.archistar.middleware.CustomSerializer;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.crypto.SecretSharingCryptoEngine;
import at.ac.ait.archistar.middleware.frontend.FSObject;

public class TestSecretSharingCryptoEngine {
	
	private static FSObject testData;
	private static CryptoEngine cryptoEngine;
	private final static byte[] mockSerializedData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
	
	@BeforeClass
	public static void onceSetup() {
		// GIVEN some test data
		testData = mock(FSObject.class);
		CustomSerializer serializer = mock(CustomSerializer.class);
		when(serializer.serialize(testData)).thenReturn(mockSerializedData);
		when(serializer.deserialize(mockSerializedData)).thenReturn(testData);		
		cryptoEngine = new SecretSharingCryptoEngine(serializer);
	}
	
	@Test
	public void testIfDecryptionProducesOriginalData() {
		
		Set<Fragment> distribution = new HashSet<Fragment>();
		distribution.add(new RemoteFragment("frag-1"));
		distribution.add(new RemoteFragment("frag-2"));
		distribution.add(new RemoteFragment("frag-3"));
		distribution.add(new RemoteFragment("frag-4"));
		
		Set<Fragment> encrypted = cryptoEngine.encrypt(testData, distribution);
		
		assertThat(encrypted.size()).isEqualTo(4);
		
		for(Fragment f : encrypted) {
			assertThat(f.getData()).isNotNull();
			assertThat(f.getData()).isNotEmpty();
			assertThat(f.getMetaData("xValue")).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(distribution.size());
		}
		
		FSObject result = null;
		try {
			result = cryptoEngine.decrypt(encrypted);
		} catch (DecryptionException e) {
			fail("error while decryption", e);
		}
		assertThat(result).isNotNull().isEqualTo(testData);
	}

}
