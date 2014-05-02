package at.ac.ait.archistar.engine.crypto;

import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;

/**
 * this is the base interface for all encryption/decryption-related stuff. It
 * takes an end-user supplied FSObject (which can be serialized using
 * CustomSerializer) and a Set of fragments and fills the later with encrypted
 * data. The decrypt function does *tada* the reverse.
 *
 * @author andy
 *
 */
public interface CryptoEngine {

    /**
     * split and encrypt data into fragments
     *
     * @param data the original data
     * @param fragments collection of expected fragments, the cryptoengine
     * should fill the existing fragments with the encrypted data
     * @return set of fragments containing encrypted data
     */
    Set<Fragment> encrypt(byte[] data, Set<Fragment> fragments);

    /**
     * decrypt collection of fragments into an Object
     * @param input set of encrypted fragments
     * @return origin (decrypted) user data
     * @throws at.ac.ait.archistar.engine.crypto.DecryptionException
     */
    byte[] decrypt(Set<Fragment> input) throws DecryptionException;
}
