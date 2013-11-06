package at.ac.ait.archistar.cryptoengine;

import static org.fest.assertions.api.Assertions.*;

import java.security.GeneralSecurityException;

import org.junit.Test;

import at.archistar.crypto.SecretSharing;
import at.archistar.crypto.ShamirPSS;
import at.archistar.crypto.WeakSecurityException;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.random.FakeRandomSource;


public class TestSecretSharingIntegeration {

	@Test
	public void simpleRoundTest() throws WeakSecurityException, GeneralSecurityException {
		byte data[] = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
			
		SecretSharing algorithm = new ShamirPSS(4, 3, new FakeRandomSource());
		
		Share shares[] = algorithm.share(data);
			
		byte reconstructedData[] = algorithm.reconstruct(shares);
		assertThat(reconstructedData).isEqualTo(data);
	}

}
