package at.ac.ait.archistar.backendserver.storageinterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * provides a storage server through a newly created (empty) filesystem
 * directory. Each fragment will be written into a file.
 *
 * @author andy
 */
public class FilesystemStorage implements StorageServer {

    private boolean connected = false;

    private final int internalBFTId;

    private final File baseFp;

    public FilesystemStorage(int bftId, File baseDir) {
        this.baseFp = baseDir;
        this.internalBFTId = bftId;
    }

    /* memory storage does not need any connect operation */
    @Override
    public int connect() {
        if (!baseFp.isDirectory() || !baseFp.canWrite()) {
            assert (false);
            return -1;
        } else {
            this.connected = true;
            return 0;
        }
    }

    @Override
    public int disconnect() {
        this.connected = false;
        return 0;
    }

    @Override
    public byte[] putBlob(String id, byte[] blob) throws DisconnectedException {

        validateOnline();

        /* store data */
        File new_file = new File(baseFp, id);
        FileOutputStream out;
        try {
            out = new FileOutputStream(new_file);
            out.write(blob);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            assert (false);
        }
        return blob;
    }

    private void validateOnline() throws DisconnectedException {
        if (!this.connected) {
            throw new DisconnectedException();
        }
    }

    private boolean checkExistenceOfId(String id) {
        /* now do a fp.list().contains?(filename) */
        String[] files = baseFp.list();
        for (String f : files) {
            if (f.equalsIgnoreCase(id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public byte[] getBlob(String id) throws DisconnectedException {

        validateOnline();

        if (!checkExistenceOfId(id)) {
            return null;
        }

        /* receive data */
        File new_file = new File(baseFp, id);
        assert (new_file.exists());
        assert (new_file.canRead());

        try (FileInputStream in = new FileInputStream(new_file)) {
            return IOUtils.toByteArray(in);
        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }

        return null;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public int getFragmentCount() throws DisconnectedException {
        validateOnline();

        return baseFp.list().length;
    }

    @Override
    public String getId() {
        return "storage:///" + this.baseFp.getAbsolutePath();
    }

    @Override
    public int getBFTId() {
        return this.internalBFTId;
    }
}
