package at.ac.ait.archistar.frontend.s3;

import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.resteasy.util.Hex;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;

@Path("/fake_bucket")
public class FakeBucket {
	
	private Engine engine;
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;
	
	public FakeBucket(Engine engine) throws ParserConfigurationException {
		this.engine = engine;
		this.docFactory = DocumentBuilderFactory.newInstance();
		this.docBuilder = docFactory.newDocumentBuilder();
	}
	
	private String stringFromDoc(Document doc) {
		DOMImplementation impl = doc.getImplementation();
		DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
		LSSerializer lsSerializer = implLS.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("format-pretty-print", true);
		 
		LSOutput lsOutput = implLS.createLSOutput();
		lsOutput.setEncoding("UTF-8");
		Writer stringWriter = new StringWriter();
		lsOutput.setCharacterStream(stringWriter);
		lsSerializer.write(doc, lsOutput);
		
		return stringWriter.toString();
	}

	/* list all elements within bucket?
	 * 
	 * <?xml version="1.0" encoding="UTF-8"?>
<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Name>bucket</Name>
    <Prefix/>
    <Marker/>
    <MaxKeys>1000</MaxKeys>
    <IsTruncated>false</IsTruncated>
    <Contents>
        <Key>my-image.jpg</Key>
        <LastModified>2009-10-12T17:50:30.000Z</LastModified>
        <ETag>&quot;fba9dede5f27731c9771645a39863328&quot;</ETag>
        <Size>434234</Size>
        <StorageClass>STANDARD</StorageClass>
        <Owner>
            <ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID>
            <DisplayName>mtd@amazon.com</DisplayName>
        </Owner>
    </Contents>
    <Contents>
       <Key>my-third-image.jpg</Key>
         <LastModified>2009-10-12T17:50:30.000Z</LastModified>
        <ETag>&quot;1b2cf535f27731c974343645a3985328&quot;</ETag>
        <Size>64994</Size>
        <StorageClass>STANDARD</StorageClass>
        <Owner>
            <ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID>
            <DisplayName>mtd@amazon.com</DisplayName>
        </Owner>
    </Contents>
</ListBucketResult>
	 */
	@GET
	@Produces("text/plain")
	public String getAll() {
				
		Document doc = this.docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		
		Element rootElement = doc.createElement("ListBucketResult");
		rootElement.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01");
		doc.appendChild(rootElement);
		
		Element name = doc.createElement("Name");
		name.setTextContent("fake_bucket");
		rootElement.appendChild(name);
		rootElement.appendChild(doc.createElement("Prefix"));
		rootElement.appendChild(doc.createElement("Marker"));
		
		Element maxKeys = doc.createElement("MaxKeys");
		maxKeys.setTextContent("1000");
		rootElement.appendChild(maxKeys);
		
		Element isTruncated = doc.createElement("isTruncated");
		isTruncated.setTextContent("false");
		rootElement.appendChild(isTruncated);		
		
		for(String key : this.engine.listObjects("/")) {
			Element contents = doc.createElement("Contents");
			
			contents.appendChild(createElement(doc, "Key", key));
			contents.appendChild(createElement(doc, "Size", "42"));
			contents.appendChild(createElement(doc, "LastModified", "2006-02-03T16:41:58.000Z"));
			contents.appendChild(createElement(doc, "ETag", "42"));
			
			rootElement.appendChild(contents);
		}
		return stringFromDoc(doc);
	}
	
	private Element createElement(Document doc, String name, String value) {
		Element xmlKey = doc.createElement(name);
		xmlKey.setTextContent(value);
		return xmlKey;
	}
	
	@GET
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getById(@PathParam("id") String id) throws DecryptionException, NoSuchAlgorithmException {
		FSObject obj = engine.getObject(id);
		byte[] result = null;
		
		if (obj instanceof SimpleFile) {
			result = ((SimpleFile) obj).getData();
		}
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(result);
		
		return Response.accepted().entity(result).header("ETag", new String(Hex.encodeHex(thedigest))).build();
	}
	
	@PUT
	@Path( "{id:.+}")
	@Produces ("text/xml")
	public Response writeById(@PathParam("id") String id,
						   @HeaderParam("x-amz-server-side-encryption") String serverSideEncryption,
						   byte[] input) throws NoSuchAlgorithmException, DecryptionException {
		
		System.err.println("id: " + id);
		
		SimpleFile obj = new SimpleFile(id, input, new HashMap<String, String>());		
		engine.putObject(obj);
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(input);
		
		engine.getObject(id);
		
		return Response.accepted().header("ETag", new String(Hex.encodeHex(thedigest))).build();
	}

	@DELETE
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response deleteById(@PathParam("id") String id) throws DecryptionException {
		
		System.err.println("id: " + id);
		
		FSObject obj = engine.getObject(id);
		engine.deleteObject(obj);
		
		return Response.accepted().status(204).build();
	}
}