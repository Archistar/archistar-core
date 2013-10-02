package at.ac.ait.archistar.data;

import at.ac.ait.archistar.data.user.FSObject;

/**
 * convert incoming user file objects into byte arrays (and vice versa)
 * 
 * @author andy
 */
public interface Serializer {
	
	public byte[] serialize(FSObject input);
	
	public FSObject deserialize(byte[] data);
}
