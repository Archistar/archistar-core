package at.ac.ait.archistar.middleware.crypto;

import java.util.Set;

import at.ac.ait.archistar.backendserver.fragments.Fragment;

/**
 * this is the base interface for all encryption/decryption-related
 * stuff. It takes an end-user supplied FSObject (which can be serialized
 * using CustomSerializer) and a Set of fragments and fills the later with
 * encrypted data. The decrypt function does *tada* the reverse.
 * 
 * @author andy
 *
 */
public interface CryptoEngine {

  /** encrypt data+metadata into the fragments */
  Set<Fragment> encrypt(byte[] data, Set<Fragment> fragments);

  /** decrypt collection of fragments into an Object
   */
  byte[] decrypt(Set<Fragment> input) throws DecryptionException;
}
