package at.ac.ait.archistar.engine.dataobjects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * convert incoming user file objects into byte arrays (and vice versa)
 *
 * @author andy
 */
public class CustomSerializer implements Serializer {

    @Override
    public byte[] serialize(FSObject input) {

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                out.writeObject(input);
            }

            return bos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public FSObject deserialize(byte[] data) {

        if (data == null) {
            return null;
        }

        try (ObjectInputStream reader = new ObjectInputStream(new ByteArrayInputStream(data))){
            return (FSObject) reader.readObject();
        } catch (IOException | ClassNotFoundException e) {
            assert(false);
        }
        return null;
    }
}
