package at.ac.ait.archistar.engine.crypto;

import java.util.Arrays;

import at.archistar.crypto.CryptoEngine;
import at.archistar.crypto.data.InvalidParametersException;
import at.archistar.crypto.data.ShamirShare;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.exceptions.ImpossibleException;
import at.archistar.crypto.exceptions.ReconstructionException;
import at.archistar.crypto.exceptions.WeakSecurityException;
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
        ShamirShare[] sshares = Arrays.copyOf(shares, shares.length, ShamirShare[].class);

        byte[] reference = null;
        boolean first = true;

        for (ShamirShare f : sshares) {
            if (first) {
                /* initialize on first access */
                reference = f.getY();
            } else {
                if (!Arrays.equals(reference, f.getY())) {
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
    public Share[] share(byte[] data) throws WeakSecurityException, ImpossibleException {
        
        ShamirShare[] sshares = new ShamirShare[n];
        
        for (int i = 0; i < n; i++) {
            try {
                sshares[i] = new ShamirShare((byte) (i+1), data);
            } catch (InvalidParametersException ex) {
                Logger.getLogger(PseudoMirrorCryptoEngine.class.getName()).log(Level.SEVERE, null, ex);
                assert(false);
            }
        }
        return sshares;
    }
}
