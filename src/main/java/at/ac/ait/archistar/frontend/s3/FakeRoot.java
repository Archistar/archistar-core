package at.ac.ait.archistar.frontend.s3;

import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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

@Path("/")
public class FakeRoot {
	
	private Engine engine;
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;

	public FakeRoot(Engine engine) throws ParserConfigurationException {
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
	
	/*
	 * Example output:
	
	<?xml version="1.0" encoding="UTF-8"?>
	<ListAllMyBucketsResult xmlns="http://doc.s3.amazonaws.com/2006-03-01">
	  <Owner>
	    <ID>bcaf1ffd86f461ca5fb16fd081034f</ID>
	    <DisplayName>webfile</DisplayName>
	  </Owner>
	  <Buckets>
	    <Bucket>
	      <Name>quotes</Name>
	      <CreationDate>2006-02-03T16:45:09.000Z</CreationDate>
	    </Bucket>
	    <Bucket>
	      <Name>samples</Name>
	      <CreationDate>2006-02-03T16:41:58.000Z</CreationDate>
	    </Bucket>
	  </Buckets>
	</ListAllMyBucketsResult>
	*/
	
	public String listBuckets() {
		
		Document doc = this.docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		
		Element rootElement = doc.createElement("ListAllMyBucketsResult");
		rootElement.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01");
		doc.appendChild(rootElement);
		
		Element owner = doc.createElement("Owner");
		Element id = doc.createElement("ID");
		id.setTextContent("deadbeef");
		Element name = doc.createElement("DisplayName");
		name.setTextContent("Andreas Happe");

		owner.appendChild(id);
		owner.appendChild(name);
		rootElement.appendChild(owner);
		
		Element buckets = doc.createElement("Buckets");
		Element bucket = doc.createElement("Bucket");
		Element bucketName = doc.createElement("Name");
		bucketName.setTextContent("fake_bucket");
		
		Element creationDate = doc.createElement("CreationDate");
		creationDate.setTextContent("2006-02-03T16:41:58.000Z");
		
		bucket.appendChild(bucketName);
		bucket.appendChild(creationDate);
		buckets.appendChild(bucket);
		rootElement.appendChild(buckets);
		
		return stringFromDoc(doc);
	}
	
	/* need to do this in a cleaner way */
	@GET
	@Produces("text/plain")
	public String getAll(
			@QueryParam("delimiter") String delim,
            @QueryParam("prefix") String prefix,
            @QueryParam("max-keys") int maxKeysInt) {
				
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

		if (prefix != null && (prefix.equals("/") || prefix.equals(""))) {
			prefix = null;
		}
		
		if (prefix != null && prefix.startsWith("/fake_bucket")) {
			prefix = prefix.substring(12);
		}
		
		if (prefix != null && (prefix.equals("/") || prefix.equals(""))) {
			prefix = null;
		}
		
		System.err.println("prefix: " + prefix);
		
		for(String key : this.engine.listObjects(prefix)) {
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
	public Response getById(@PathParam("id") String id
			) throws DecryptionException, NoSuchAlgorithmException {
		
		System.err.println("get by id:" + id);
		
		FSObject obj = engine.getObject(id);
		byte[] result = null;
		
		if (obj instanceof SimpleFile) {
			result = ((SimpleFile) obj).getData();
		}
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(result);
		
		return Response.accepted().entity(result).header("ETag", new String(Hex.encodeHex(thedigest))).build();
	}

	@HEAD
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getStatById(@PathParam("id") String id) throws DecryptionException, NoSuchAlgorithmException {
		
		System.err.println("HEAD by id:" + id);
		Map<String, String> result = engine.statObject(id);
		
		if (result != null) {
			FSObject obj = engine.getObject(id);
			
			SimpleFile file = null;
			if (obj instanceof SimpleFile) {
				file = (SimpleFile) obj;
			}
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(file.getData());
			String etag = new String(Hex.encodeHex(thedigest));
			
			ResponseBuilder resp = Response.accepted().status(200);
			
			System.err.println("Content-Length is " + file.getData().length);
			
			resp.header("Content-Length", "" + file.getData().length);
			resp.header("x-foobar", "" + file.getData().length);
			resp.header("ETag", etag);
			
			Map<String, String> md1 = file.getMetadata();
			
			for(String i : md1.keySet()) {
				System.err.println("metadata: " + i + " -> " + md1.get(i));
			}
			
			if(md1.get("uid") != null) {
				resp.header("x-amz-meta-uid", md1.get("uid").replace("\r\n",""));
			}
			
			if(md1.get("gid") != null) {
				resp.header("x-amz-meta-gid", md1.get("gid").replace("\r\n",""));
			}
			
			
			if(md1.get("mode") != null) {
				resp.header("x-amz-meta-mode", md1.get("mode").replace("\r\n",""));
			}
			
			Response r = resp.build();
			
			System.err.println("r->length: " + r.getLength());
			System.err.println("returning 200");
			return r;
		} else {
			System.err.println("returning 404");
			return Response.accepted().status(404).build();
		}		
	}
	
	@PUT
	@Path( "{id:.+}")
	@Produces ("text/xml")
	public Response writeById(@PathParam("id") String id,
						   @HeaderParam("x-amz-server-side-encryption") String serverSideEncryption,
						   @HeaderParam("x-amz-meta-gid") String gid,
						   @HeaderParam("x-amz-meta-uid") String uid,
						   @HeaderParam("x-amz-meta-mode") String mode,
						   byte[] input) throws NoSuchAlgorithmException, DecryptionException {
		
		System.err.println("PUT by id: " + id);
		
		SimpleFile obj = new SimpleFile(id, input, new HashMap<String, String>());
		
		obj.setMetaData("gid", gid);
		obj.setMetaData("uid", uid);
		obj.setMetaData("mode", mode);
		
		engine.putObject(obj);
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(input);
		
		engine.getObject(id);
		
		return Response.accepted().status(200).header("ETag", new String(Hex.encodeHex(thedigest))).build();
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
