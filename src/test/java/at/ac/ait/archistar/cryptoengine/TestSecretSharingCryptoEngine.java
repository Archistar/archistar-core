package at.ac.ait.archistar.cryptoengine;

import static org.fest.assertions.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.RemoteFragment;
import at.ac.ait.archistar.engine.crypto.ArchistarCryptoEngine;
import at.ac.ait.archistar.engine.crypto.DecryptionException;
import at.ac.ait.archistar.engine.crypto.SecretSharingCryptoEngine;
import at.archistar.crypto.RabinBenOrEngine;
import at.archistar.crypto.exceptions.WeakSecurityException;
import at.archistar.crypto.random.FakeRandomSource;
import java.security.NoSuchAlgorithmException;

public class TestSecretSharingCryptoEngine {

    private static ArchistarCryptoEngine cryptoEngine;
    private final static byte[] mockSerializedData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    @BeforeClass
    public static void onceSetup() throws WeakSecurityException, NoSuchAlgorithmException {
        cryptoEngine = new SecretSharingCryptoEngine(new RabinBenOrEngine(4, 3, new FakeRandomSource()));
    }

    @Test
    public void testIfDecryptionProducesOriginalData() {

        Set<Fragment> distribution = new HashSet<>();
        distribution.add(new RemoteFragment("frag-1"));
        distribution.add(new RemoteFragment("frag-2"));
        distribution.add(new RemoteFragment("frag-3"));
        distribution.add(new RemoteFragment("frag-4"));

        Set<Fragment> encrypted = cryptoEngine.encrypt(mockSerializedData, distribution);

        assertThat(encrypted.size()).isEqualTo(4);

        for (Fragment f : encrypted) {
            assertThat(f.getData()).isNotNull();
            assertThat(f.getData()).isNotEmpty();
        }

        byte[] result = null;
        try {
            result = cryptoEngine.decrypt(encrypted);
        } catch (DecryptionException e) {
            fail("error while decryption", e);
        }
        assertThat(result).isNotNull().isEqualTo(mockSerializedData);
    }
}
