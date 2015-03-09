package at.ac.ait.archistar.engine.crypto;

import java.util.Arrays;

import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.data.InvalidParametersException;
import at.archistar.crypto.data.Share;
import static at.archistar.crypto.data.Share.ShareType.SHAMIR_PSS;
import at.archistar.crypto.data.ShareFactory;
import at.archistar.crypto.secretsharing.ReconstructionException;
import at.archistar.crypto.secretsharing.WeakSecurityException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This just takes a user-supplied FSObject, serializes it (using
 * CustomSerializer) and fills in duplicates of the serialized data into all
 * fragments.
 *
 * @author Andreas Happe <andreashappe@snikt.net>
 *
 */
public class PseudoMirrorCryptoEngine implements CryptoEngine {

    private final int n;
    
    public PseudoMirrorCryptoEngine(int n) {
        this.n = n;
    }
    
    /**
     * checks if data within all fragments is the same and returns the
     * encapsulated data
     */
    @Override
    public byte[] reconstruct(Share[] shares) throws ReconstructionException {
        byte[] reference = null;
        boolean first = true;

        for (Share f : shares) {
            if (first) {
                /* initialize on first access */
                reference = f.getYValues();
            } else {
                if (!Arrays.equals(reference, f.getYValues())) {
                    throw new ReconstructionException();
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
    public Share[] share(byte[] data) {
        
        Share[] sshares = new Share[n];
        
        for (int i = 0; i < n; i++) {
            try {
                sshares[i] = ShareFactory.create(SHAMIR_PSS, (byte) (i+1), data, new HashMap<Byte, byte[]>());
            } catch (InvalidParametersException ex) {
                Logger.getLogger(PseudoMirrorCryptoEngine.class.getName()).log(Level.SEVERE, null, ex);
                assert(false);
            }
        }
        return sshares;
    }
}
