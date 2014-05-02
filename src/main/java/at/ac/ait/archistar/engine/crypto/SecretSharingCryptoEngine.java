package at.ac.ait.archistar.engine.crypto;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.Fragment.EncryptionScheme;
import at.archistar.crypto.SecretSharing;
import at.archistar.crypto.WeakSecurityException;
import at.archistar.crypto.data.Share;
import at.archistar.helper.ShareSerializer;

/**
 * wrapper for using an archistar-smc secret-sharing algorithm with the
 * archistar-core system
 *
 * @author andy
 */
public class SecretSharingCryptoEngine implements CryptoEngine {

    private final SecretSharing sharingAlgorithm;

    public SecretSharingCryptoEngine(SecretSharing sharingAlgorithm) {
        this.sharingAlgorithm = sharingAlgorithm;
    }

    @Override
    public byte[] decrypt(Set<Fragment> input) throws DecryptionException {

        Share[] shares = new Share[input.size()];

        int i = 0;
        for (Fragment f : input) {
            if (f.getData() != null) {
                shares[i++] = ShareSerializer.deserializeShare(f.getData());
            }
        }

        if (i == 0) {
            return null;
        } else if (i != input.size()) {
            shares = Arrays.copyOf(shares, i);
        }

        byte[] combined = null;
        try {
            combined = this.sharingAlgorithm.reconstruct(shares);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            assert (false);
        }
        return combined;
    }

    @Override
    public Set<Fragment> encrypt(byte[] originalContent, Set<Fragment> fragments) {

        try {
            Share[] shares = this.sharingAlgorithm.share(originalContent);
            Fragment[] fs = fragments.toArray(new Fragment[0]);
            assertThat(fs.length == shares.length);
            assertThat(fragments.size() == 4);

            for (int i = 0; i < fs.length; i++) {

                Fragment f = fs[i];

                byte[] binData = ShareSerializer.serializeShare(shares[i]);
                assert (binData != null);
                assert (binData.length != 0);

                f.setData(binData);
                f.setEncryptionScheme(EncryptionScheme.SHAMIR);
            }
        } catch (WeakSecurityException | GeneralSecurityException e) {
            e.printStackTrace();
            assert (false);
        }
        return fragments;
    }
}
