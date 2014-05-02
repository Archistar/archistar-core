package at.ac.ait.archistar.backendserver.storageinterface;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.CredentialsProvider;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;

/**
 * provide a storage provider utilizing a remote S3 storage provider
 *
 * @author andy
 */
public class JetS3tStorage implements StorageServer {

    private AWSCredentials awsCredentials;
    private S3Service s3service;
    private S3Bucket s3bucket;
    private String bucketId;
    private int internalBFTId;

    public JetS3tStorage(int bftId, String awsAccessKey, String awsSecretKey, String bucketId) {
        awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
        this.bucketId = bucketId;
        this.internalBFTId = bftId;
    }

    public byte[] putBlob(String id, byte[] blob) throws DisconnectedException {

        if (s3service == null || s3bucket == null) {
            throw new DisconnectedException();
        }

        try {
            StorageObject obj = new S3Object(id, blob);
            s3service.putObject(s3bucket.getName(), obj);
            return blob;
        } catch (S3ServiceException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        } catch (ServiceException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        } catch (NoSuchAlgorithmException e) {
            /* WTF? exception */
            e.printStackTrace();
            throw new DisconnectedException();
        }
    }

    @SuppressWarnings("deprecation")
    public byte[] getBlob(String id) throws DisconnectedException {

        try {
            if (!fragmentExists(id)) {
                return null;
            }
            S3Object obj = s3service.getObject(s3bucket, id);
            return IOUtils.toByteArray(obj.getDataInputStream());
        } catch (S3ServiceException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        } catch (ServiceException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        }
    }

    private boolean fragmentExists(String id) {
        try {
            return s3service.isObjectInBucket(s3bucket.getName(), id);
        } catch (ServiceException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * TODO: since when is this deprecated?
     * 
     * (non-Javadoc)
     * @see at.ac.ait.archistar.storage.StorageServer#getFragmentCount()
     */
    @SuppressWarnings("deprecation")
    public int getFragmentCount() throws DisconnectedException {
        try {
            return s3service.listObjects(s3bucket).length;
        } catch (S3ServiceException e) {
            e.printStackTrace();
            throw new DisconnectedException();
        }
    }

    public int connect() {
        try {
            s3service = new RestS3Service(awsCredentials);
            Jets3tProperties props = s3service.getJetS3tProperties();
            props.setProperty("s3service.s3-endpoint-http-port", "10453");
            props.setProperty("s3service.https-only", "false");
            props.setProperty("s3service.s3-endpoint", "fakes3.local");

            CredentialsProvider credentials = s3service.getCredentialsProvider();
            s3service = new RestS3Service(awsCredentials, "test-app", credentials, props);

            s3bucket = s3service.getBucket(bucketId);
            if (s3bucket == null) {
                s3bucket = s3service.createBucket(bucketId);
            }
        } catch (S3ServiceException e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    public int disconnect() {
        s3bucket = null;
        try {
            s3service.shutdown();
        } catch (ServiceException e) {
            e.printStackTrace();
            return -1;
        }

        s3service = null;
        return 0;
    }

    public boolean isConnected() {
        return s3service != null;
    }

    public String getId() {
        return "s3://amazon/" + bucketId + "/";
    }

    @Override
    public int getBFTId() {
        return this.internalBFTId;
    }
}
