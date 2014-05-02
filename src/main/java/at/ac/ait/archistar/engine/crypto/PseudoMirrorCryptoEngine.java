package at.ac.ait.archistar.engine.crypto;

import java.util.Arrays;
import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;

/**
 * This just takes a user-supplied FSObject, serializes it (using
 * CustomSerializer) and fills in duplicates of the serialized data into all
 * fragments.
 *
 * @author Andreas Happe <andreashappe@snikt.net>
 *
 */
public class PseudoMirrorCryptoEngine implements CryptoEngine {

    /**
     * checks if data within all fragments is the same and returns the
     * encapsulated data
     */
    @Override
    public byte[] decrypt(Set<Fragment> input) throws DecryptionException {

        byte[] reference = null;
        boolean first = true;

        for (Fragment f : input) {
            if (f.isSynchronized()) {
                if (first) {
                    /* initialize on first access */
                    reference = f.getData();
                } else {
                    if (!Arrays.equals(reference, f.getData())) {
                        throw new DecryptionException();
                    }
                }
            }
        }

        return reference;
    }

    /**
     * this sets data for each fragment -- no encryption whatsoever, pure
     * duplication
     */
    @Override
    public Set<Fragment> encrypt(byte[] data, Set<Fragment> fragments) {
        for (Fragment f : fragments) {
            f.setData(data);
        }
        return fragments;
    }
}
