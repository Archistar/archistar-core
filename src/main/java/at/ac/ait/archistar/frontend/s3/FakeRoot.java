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
		
	@GET
	@Produces("text/plain")
	public String getAll(
			@QueryParam("delimiter") String delim,
            @QueryParam("prefix") String prefix,
            @QueryParam("max-keys") int maxKeysInt,
            @HeaderParam("X-Bucket") String bucket) throws DecryptionException {
		
		if (bucket.isEmpty()) {
			/* list all buckets */
			return builder.stringFromDoc(builder.listBuckets(this.buckets));
		} else if (!this.buckets.containsKey(bucket)) {
			return bucketNotFound(bucket);
		} else {
			/* return content of this bucket */
			return this.buckets.get(bucket).getAll(bucket, delim, prefix, maxKeysInt);		
		}
	}
	
	private String bucketNotFound(String bucket) {
		return builder.stringFromDoc(builder.bucketNotFound(bucket));
	}
	
	@GET
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getById(@PathParam("id") String id,
							@HeaderParam("X-Bucket") String bucket
			) throws DecryptionException, NoSuchAlgorithmException {
		
		if (!this.buckets.containsKey(bucket)) {
			return Response.accepted().status(404).entity(bucketNotFound(bucket)).build();
		} else {
			return this.buckets.get(bucket).getById(id);
		}
	}

	@HEAD
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getStatById(@PathParam("id") String id,
								@HeaderParam("X-Bucket") String bucket
							   ) throws DecryptionException, NoSuchAlgorithmException {
		
		if (!this.buckets.containsKey(bucket)) {
			return Response.accepted().status(404).entity(bucketNotFound(bucket)).build();
		} else {		
			return this.buckets.get(bucket).getStatById(id);
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
						   @HeaderParam("X-Bucket") String bucket,
						   byte[] input) throws NoSuchAlgorithmException, DecryptionException {
		
		if (!this.buckets.containsKey(bucket)) {
			return Response.accepted().status(404).entity(bucketNotFound(bucket)).build();
		} else {		
			return this.buckets.get(bucket).writeById(id, gid, uid, mode, serverSideEncryption, input);
		}
	}

	@DELETE
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response deleteById(@PathParam("id") String id,
			   				   @HeaderParam("X-Bucket") String bucket
							  ) throws DecryptionException {
		if (!this.buckets.containsKey(bucket)) {
			return Response.accepted().status(404).entity(bucketNotFound(bucket)).build();
		} else {		
			return this.buckets.get(bucket).deleteById(id);
		}
	}
}
