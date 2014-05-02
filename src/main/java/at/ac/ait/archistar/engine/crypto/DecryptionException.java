package at.ac.ait.archistar.engine.crypto;

/**
 * used for exceptions during decryption -- mostly this should denote corrupted
 * shares
 *
 * TODO: I would like to improve this exception to include references to the
 * detected corrupted shares (if possible)
 *
 * @author andy
 */
public class DecryptionException extends Exception {

    private static final long serialVersionUID = 5753597356767327584L;
}
