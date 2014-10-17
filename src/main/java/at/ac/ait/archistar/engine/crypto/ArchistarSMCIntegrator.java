/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.ait.archistar.engine.crypto;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.data.InvalidParametersException;
import at.archistar.crypto.data.SerializableShare;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.exceptions.ImpossibleException;
import at.archistar.crypto.exceptions.ReconstructionException;
import at.archistar.crypto.exceptions.WeakSecurityException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 *
 * @author andy
 */
public class ArchistarSMCIntegrator {
        /**
     * split and encrypt data into fragments
     *
     * @param data the original data
     * @param fragments collection of expected fragments, the cryptoengine
     * should fill the existing fragments with the encrypted data
     * @return set of fragments containing encrypted data
     */
    public static Set<Fragment> encrypt(CryptoEngine engine, byte[] data, Set<Fragment> fragments) {
        try {
            Share[] shares = engine.share(data);
            Fragment[] fs = fragments.toArray(new Fragment[fragments.size()]);
            assertThat(fs.length == shares.length);
            assertThat(fragments.size() == 4);

            for (int i = 0; i < fs.length; i++) {

                Fragment f = fs[i];

                byte[] binData = shares[i].serialize();
                assert (binData != null);
                assert (binData.length != 0);

                f.setData(binData);
                f.setEncryptionScheme(Fragment.EncryptionScheme.SHAMIR);
            }
        } catch (WeakSecurityException | ImpossibleException | IOException e) {
            assert (false);
        }
        return fragments;

    }

    /**
     * decrypt collection of fragments into an Object
     * @param input set of encrypted fragments
     * @return origin (decrypted) user data
     */
    public static byte[] decrypt(CryptoEngine engine, Set<Fragment> input) throws ReconstructionException {
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

        return engine.reconstruct(shares);
    }
}
