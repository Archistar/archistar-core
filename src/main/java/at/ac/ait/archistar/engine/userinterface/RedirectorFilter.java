package at.ac.ait.archistar.engine.userinterface;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this filter should normalize incoming requests. S3 allows two ways of
 * addressing incoming requests: path style and virtual style. With this filter
 * all requests should be normalized to using a virtual style request
 *
 * @author Andreas Happe <andreashappe@snikt.net>
 */
@Provider
@PreMatching
public class RedirectorFilter implements ContainerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(RedirectorFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {

        /* Host header: for path style this should be s3.amazonaws.com,
         *              for virtual-style this should be <bucket>.s3.amazonaws.com
         */
        String host = requestContext.getHeaderString("Host");

        if (host.endsWith(".s3.amazonaws.com")) {
            /* bucket name is already set, do nothing */
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
        String bucketString = "";
        String bucket = "";
        String query = uri.getQuery();

        logger.debug("Input: " + uri.toString());

        if (path.equals("/")) {
            bucketString = "s3.amazonaws.com";
            path = "/";
        } else {
            Pattern p = Pattern.compile("^/*([^/]+)(.*)$");
            Matcher m = p.matcher(path);

            if (m.find() && m.groupCount() == 2) {
                bucket = m.group(1);
                path = m.group(2);
                bucketString = bucket + ".s3.amazonaws.com";
            } else {
                throw new RuntimeException();
            }
        }

        /* nothing found, try query params */
        if (bucket != null && path != null && uri != null && uri.getQuery() != null && bucket.equalsIgnoreCase("") && path.equalsIgnoreCase("/") && !uri.getQuery().isEmpty()) {

            query = "";

            for (String part : uri.getQuery().split("&")) {

                if (!query.isEmpty()) {
                    query = query + "&";
                }

                String key = part.split("=")[0];
                String value = part.split("=")[1];

                if (key.equalsIgnoreCase("prefix")) {
                    Pattern p = Pattern.compile("^/*([^/]+)(.*)$");
                    Matcher m = p.matcher(value);

                    if (m.find()) {
                        System.err.println("MATCH!");
                        bucket = m.group(1);
                        bucketString = bucket + ".s3.amazonaws.com";
                        query = query + "prefix=" + m.group(2);
                    } else {
                        query = query + part;
                    }
                } else {
                    query = query + part;
                }
            }
        }

        logger.debug("redirected: " + uri.toString() + " -> " + bucket + "/" + path + "?" + query);

        /* set Bucket */
        ctx.getHeaders().add("X-Bucket", bucket);

        /* set Host */
        LinkedList<String> list = new LinkedList<String>();
        list.add(bucketString);
        ctx.getHeaders().put("Host", list);

        /* set URI */
        try {
            URI target = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, query, uri.getFragment());
            logger.info("redirected: " + uri.toString() + " -> " + target.toString());
            ctx.setRequestUri(target);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
