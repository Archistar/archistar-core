package at.ac.ait.archistar.frontend.s3;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

/**
 * this filter should normalize incoming requests. S3 allows two ways of
 * addressing incoming requests: path style and virtual style. With this
 * filter all requests should be normalized to using a virtual style
 * request
 *
 * @author Andreas Happe <andreashappe@snikt.net>
 */
@Provider
@PreMatching
public class RedirectorFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
    	
    	/* Host header: for path style this should be s3.amazonaws.com,
    	 *              for virtual-style this should be <bucket>.s3.amazonaws.com
    	 */
    	String host = requestContext.getHeaderString("Host");
    	
    	if (host.endsWith(".s3.amazonaws.com")) {
    		/* bucket name is already set, do nothing */
    	} else if (host.equalsIgnoreCase("s3.amazonaws.com")) {
    		/* extract bucket from path and set header */
    		adoptBucket(requestContext);
    	} else {
    		/* this is some invalid host -- normally this means this is a
    		 * local S3 installation -- try to extract the bucket from
    		 * the path and set it as header
    		 */
    		adoptBucket(requestContext);
    	}
    }
    
    private void adoptBucket(ContainerRequestContext ctx) {
    	/* extract bucket */
    	URI uri = ctx.getUriInfo().getRequestUri();
    	String path = uri.getPath();
     	String bucket = path.substring(1, path.indexOf("/", 1));
     	String resultPath = path.substring(path.indexOf("/", 1));
    	String bucketString = bucket + ".s3.amazonaws.com";
    	
    	/* set Host */
    	ctx.getHeaders().add("Host", bucketString);
       	
    	System.err.println("new Host: " + bucketString);
    	
    	/* set URI */
		try {
			URI target = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), resultPath, uri.getQuery(), uri.getFragment());
			System.err.println("New URL: " + target.toString());
	    	ctx.setRequestUri(target);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}