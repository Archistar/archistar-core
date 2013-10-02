package at.ac.ait.archistar.cryptoengine;

import java.util.Set;

import at.ac.ait.archistar.data.fragments.Fragment;
import at.ac.ait.archistar.data.user.FSObject;

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
  Set<Fragment> encrypt(FSObject data, Set<Fragment> fragments);

  /** decrypt collection of fragments into an Object
   */
  FSObject decrypt(Set<Fragment> input) throws DecryptionException;
}
