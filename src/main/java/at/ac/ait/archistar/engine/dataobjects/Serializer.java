package at.ac.ait.archistar.engine.dataobjects;

/**
 * convert incoming user file objects into byte arrays (and vice versa)
 *
 * @author andy
 */
public interface Serializer {

    public byte[] serialize(FSObject input);

    public FSObject deserialize(byte[] data);
}
