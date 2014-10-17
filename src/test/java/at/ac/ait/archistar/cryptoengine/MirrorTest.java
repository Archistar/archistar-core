package at.ac.ait.archistar.cryptoengine;

import static org.fest.assertions.api.Assertions.*;

import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.ait.archistar.engine.crypto.PseudoMirrorCryptoEngine;
import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.data.ShamirShare;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.exceptions.ReconstructionException;
import at.archistar.crypto.exceptions.WeakSecurityException;

public class MirrorTest {

    private static CryptoEngine cryptoEngine;
    private final static byte[] mockSerializedData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    @BeforeClass
    public static void onceSetup() {
        // GIVEN some test data
        cryptoEngine = new PseudoMirrorCryptoEngine(2);
    }

    @Test
    public void testIfCryptoEngineProducesEnoughFragments() throws WeakSecurityException {
        // WHEN i encrypt some data
        Share shares[] = cryptoEngine.share(mockSerializedData);
        assertThat(shares).hasSize(2);
    }

    @Test
    public void testIfMirroringWorks() throws WeakSecurityException {
        // WHEN i encrypt data
        Share shares[] = cryptoEngine.share(mockSerializedData);

        for(Share s : shares) {
            assert(s instanceof ShamirShare);
            assertThat(((ShamirShare)s).getY()).isEqualTo(mockSerializedData);
        }
    }

    @Test
    public void testIfDecryptionProducesOriginalData() throws ReconstructionException, WeakSecurityException {
        Share shares[] = cryptoEngine.share(mockSerializedData);

        byte[] result = cryptoEngine.reconstruct(shares);
        assertThat(result).isEqualTo(mockSerializedData);
    }
}
