package at.ac.ait.archistar.trustmanager;

import io.netty.handler.ssl.SslHandler;
import java.io.IOException;
import java.security.KeyManagementException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

/**
 * Creates a bogus {@link SSLContext}. A client-side context created by this
 * factory accepts any certificate even if it is invalid. A server-side context
 * created by this factory sends a bogus certificate defined in
 * {@link SecureChatKeyStore}.
 * <p>
 * You will have to create your context differently in a real world application.
 *
 * <h3>Client Certificate Authentication</h3>
 *
 * To enable client certificate authentication:
 * <ul>
 * <li>Enable client authentication on the server side by calling
 * {@link SSLEngine#setNeedClientAuth(boolean)} before creating
 * {@link SslHandler}.</li>
 * <li>When initializing an {@link SSLContext} on the client side, specify the
 * {@link KeyManager} that contains the client certificate as the first argument
 * of {@link SSLContext#init(KeyManager[], TrustManager[], SecureRandom)}.</li>
 * <li>When initializing an {@link SSLContext} on the server side, specify the
 * proper {@link TrustManager} as the second argument of
 * {@link SSLContext#init(KeyManager[], TrustManager[], SecureRandom)} to
 * validate the client certificate.</li>
 * </ul>
 */
public final class SSLContextFactory {

    private static final String PROTOCOL = "TLS";
    private static final SSLContext SERVER_CONTEXT;
    private static final SSLContext CLIENT_CONTEXT;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext;
        SSLContext clientContext;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(SecureKeyStore.asInputStream(),
                    SecureKeyStore.getKeyStorePassword());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, SecureKeyStore.getCertificatePassword());

            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }

        try {
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(null, TrustManagerFactory.getTrustManagers(), null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new Error(
                    "Failed to initialize the client-side SSLContext", e);
        }

        SERVER_CONTEXT = serverContext;
        CLIENT_CONTEXT = clientContext;
    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    public static SSLContext getClientContext() {
        return CLIENT_CONTEXT;
    }

    private SSLContextFactory() {
        // Unused
    }
}
