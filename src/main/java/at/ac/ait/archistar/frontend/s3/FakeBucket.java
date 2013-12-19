package at.ac.ait.archistar.frontend.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.resteasy.util.Hex;

import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;

@Path("/fake_bucket")
public class FakeBucket {
	
	private Engine engine;
	
	private XmlDocumentBuilder builder;
	
	public FakeBucket(Engine engine) throws ParserConfigurationException {
		this.engine = engine;
		this.builder = new XmlDocumentBuilder();
	}
	
	/* list all elements within bucket */
	@GET
	@Produces("text/plain")
	public String getAll() throws DecryptionException, NoSuchAlgorithmException {
		
		HashSet<SimpleFile> results = new HashSet<SimpleFile>();
		for(String key : this.engine.listObjects(null)) {
			FSObject obj = engine.getObject(key);
			
			if (obj instanceof SimpleFile) {
				results.add((SimpleFile)obj);
			}
		}
		
		return builder.stringFromDoc(builder.listElements(null, 1000, results));
	}
	
	@GET
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getById(@PathParam("id") String id) throws DecryptionException, NoSuchAlgorithmException {
		
		System.err.println("get within bucket upon " + id);
		
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