package at.ac.ait.archistar.storage;

import java.io.File;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;

import at.ac.ait.archistar.backendserver.storageinterface.FilesystemStorage;
import at.ac.ait.archistar.cryptoengine.MirrorTest;

public class FileSystemStorageTest extends AbstractStorageTest {

    @Before
    public void prepareData() {
        File tmp = new File("/tmp/storage/" + UUID.randomUUID());
        if (!tmp.exists()) {
            assert (tmp.mkdirs());
        }
        tmp.deleteOnExit();

        Log log = LogFactory.getLog(MirrorTest.class);
        log.info("storing file system fragments under " + tmp.getAbsolutePath());

        store = new FilesystemStorage(0, tmp);
    }
}
