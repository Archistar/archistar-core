package at.ac.ait.archistar.backendserver.storageinterface;

/**
 * this exception should be used if a connection to a peer server was
 * interrupted (or could not be established). Remote partners can include
 * replicas as well as remote storage servers (i.e. S3)
 *
 * @author andy
 */
public class DisconnectedException extends Exception {

    private static final long serialVersionUID = 3950283191217740878L;
}
