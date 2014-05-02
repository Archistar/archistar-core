package at.ac.ait.archistar.data;

import static org.fest.assertions.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import at.ac.ait.archistar.engine.dataobjects.CustomSerializer;
import at.ac.ait.archistar.engine.dataobjects.FSObject;
import at.ac.ait.archistar.engine.dataobjects.SimpleFile;

public class CustomSerializerTest {

    private static final String testData = "datadatadata";
    private static final String testFilename = "testfilename.dat";
    private CustomSerializer serializer;
    byte[] serializedData;

    @Before
    public void prepareTestdata() {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("key0", "value0");
        metadata.put("key1", "value1");

        byte[] testString = testData.getBytes();

        SimpleFile fs = new SimpleFile(testFilename, testString, metadata);
        serializer = new CustomSerializer();
        serializedData = serializer.serialize(fs);
    }

    @Test
    public void testDataWasCreated() {
        assertThat(serializedData).isNotNull();
    }

    @Test
    public void testDeserializationOfObject() {
        FSObject des = serializer.deserialize(serializedData);
        assertThat(des).isNotNull();
    }

    @Test
    public void testPathAfterDeserialization() {
        FSObject des = serializer.deserialize(serializedData);
        assertThat(des.getPath()).isEqualTo(testFilename);
    }

    @Test
    public void testMetadataAfterDeserialization() {
        FSObject des = serializer.deserialize(serializedData);
        assertThat(des.getMetadata()).contains(entry("key0", "value0"), entry("key1", "value1"));
    }

    @Test
    public void testDataAfterDeserialization() {
        FSObject des = serializer.deserialize(serializedData);
        assertThat(des).isInstanceOf(SimpleFile.class);
        String decoded = new String(((SimpleFile) des).getData());
        assertThat(decoded).isEqualTo(testData);
    }
}
