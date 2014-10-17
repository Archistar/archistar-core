package at.ac.ait.archistar.engine.crypto;

import java.util.Arrays;
import java.util.Set;

import static org.fest.assertions.api.Assertions.*;
import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.Fragment.EncryptionScheme;
import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.data.InvalidParametersException;
import at.archistar.crypto.data.SerializableShare;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.exceptions.ImpossibleException;
import at.archistar.crypto.exceptions.ReconstructionException;
import at.archistar.crypto.exceptions.WeakSecurityException;
import java.io.IOException;

/**
 * wrapper for using an archistar-smc secret-sharing algorithm with the
 * archistar-core system
 *
 * @author andy
 */
public class SecretSharingCryptoEngine implements ArchistarCryptoEngine {

    private final CryptoEngine sharingAlgorithm;

    public SecretSharingCryptoEngine(CryptoEngine sharingAlgorithm) {
        this.sharingAlgorithm = sharingAlgorithm;
    }

    @Override
    public byte[] decrypt(Set<Fragment> input) throws DecryptionException {

        Share[] shares = new Share[input.size()];

        int i = 0;
        for (Fragment f : input) {
            if (f.getData() != null && f.getData().length != 0) {
                try {
                    shares[i++] = SerializableShare.deserialize(f.getData());
                } catch (IOException | WeakSecurityException | InvalidParametersException ex) {
                    assert(false);
                }
            }
        }

        if (i == 0) {
            return new byte[0];
        } else if (i != input.size()) {
            shares = Arrays.copyOf(shares, i);
        }

        try {
            return this.sharingAlgorithm.reconstruct(shares);
        } catch (ReconstructionException ex) {
            assert(false);
        }
        return new byte[0];
    }

    @Override
    public Set<Fragment> encrypt(byte[] originalContent, Set<Fragment> fragments) {

        try {
            Share[] shares = this.sharingAlgorithm.share(originalContent);
            Fragment[] fs = fragments.toArray(new Fragment[fragments.size()]);
            assertThat(fs.length == shares.length);
            assertThat(fragments.size() == 4);

            for (int i = 0; i < fs.length; i++) {

                Fragment f = fs[i];

                byte[] binData = shares[i].serialize();
                assert (binData != null);
                assert (binData.length != 0);

                f.setData(binData);
                f.setEncryptionScheme(EncryptionScheme.SHAMIR);
            }
        } catch (WeakSecurityException | ImpossibleException | IOException e) {
            assert (false);
        }
        return fragments;
    }
}
