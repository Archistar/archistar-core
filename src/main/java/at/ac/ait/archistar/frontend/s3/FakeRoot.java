package at.ac.ait.archistar.frontend.s3;

import java.security.NoSuchAlgorithmException;
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
import javax.xml.parsers.ParserConfigurationException;

import at.ac.ait.archistar.middleware.crypto.DecryptionException;

@Path("/")
public class FakeRoot {
	
	private XmlDocumentBuilder builder;

	private Map<String, FakeBucket> buckets;
	
	public FakeRoot(Map<String, FakeBucket> buckets) throws ParserConfigurationException {
		this.builder = new XmlDocumentBuilder();
		this.buckets = buckets;
	}
		
	public String listBuckets() {
		return builder.stringFromDoc(builder.listBuckets());
	}
	
	/* need to do this in a cleaner way */
	@GET
	@Produces("text/plain")
	public String getAll(
			@QueryParam("delimiter") String delim,
            @QueryParam("prefix") String prefix,
            @QueryParam("max-keys") int maxKeysInt) throws DecryptionException {
		
		return this.buckets.get("fake_bucket").getAll(delim, prefix, maxKeysInt);
	}
	
	@GET
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getById(@PathParam("id") String id
			) throws DecryptionException, NoSuchAlgorithmException {
		
		return this.buckets.get("fake_bucket").getById(id);
	}

	@HEAD
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getStatById(@PathParam("id") String id) throws DecryptionException, NoSuchAlgorithmException {
		
		return this.buckets.get("fake_bucket").getStatById(id);
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
		
		return this.buckets.get("fake_bucket").writeById(id, gid, uid, mode, serverSideEncryption, input);
	}

	@DELETE
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response deleteById(@PathParam("id") String id) throws DecryptionException {
		return this.buckets.get("fake_bucket").deleteById(id);
	}	
}
