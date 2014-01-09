package at.ac.ait.archistar.frontend.s3;

import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import at.ac.ait.archistar.middleware.frontend.SimpleFile;

public class XmlDocumentBuilder {
	
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;
	
	public XmlDocumentBuilder() throws ParserConfigurationException {
		this.docFactory = DocumentBuilderFactory.newInstance();
		this.docBuilder = docFactory.newDocumentBuilder();
	}
	
	private Element createElement(Document doc, String name, String value) {
		Element xmlKey = doc.createElement(name);
		xmlKey.setTextContent(value);
		return xmlKey;
	}

	public Document listElements(String prefix, String bucketName, int maxKeys, Set<SimpleFile> results) {
		Document doc = this.docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		
		Element rootElement = doc.createElement("ListBucketResult");
		rootElement.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01");
		doc.appendChild(rootElement);
		
		Element name = doc.createElement("Name");
		name.setTextContent(bucketName);
		
		rootElement.appendChild(name);
		rootElement.appendChild(doc.createElement("Prefix"));
		rootElement.appendChild(doc.createElement("Marker"));
		
		rootElement.appendChild(createElement(doc, "MaxKeys", "1000"));
		rootElement.appendChild(createElement(doc, "isTruncated", "false"));

		for(SimpleFile entry : results) {
			Element contents = doc.createElement("Contents");

			String eTag = "could not compute";
			
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] thedigest = md.digest(entry.getData());
				eTag= new String(Hex.encodeHex(thedigest));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			
			contents.appendChild(createElement(doc, "Key", entry.getPath()));
			contents.appendChild(createElement(doc, "Size", "" + entry.getData().length));
			contents.appendChild(createElement(doc, "LastModified", "2006-02-03T16:41:58.000Z"));
			contents.appendChild(createElement(doc, "ETag", eTag));
			
			rootElement.appendChild(contents);
		}
		return doc;
	}
	
	public Document bucketNotFound(String bucket) {
		
		Document doc = this.docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		
		Element rootElement = doc.createElement("Error");
		rootElement.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01");
		doc.appendChild(rootElement);
		
		rootElement.appendChild(createElement(doc, "Code", "NoSuchBucket"));
		rootElement.appendChild(createElement(doc, "Resource", bucket));
		rootElement.appendChild(createElement(doc, "Resource", bucket));
		
		return doc;
	}
	
	public Document listBuckets(Map<String, FakeBucket> bucketList) {
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
		
		for(Entry<String, FakeBucket> e : bucketList.entrySet()) {
			
			Element bucket = doc.createElement("Bucket");
			
			bucket.appendChild(createElement(doc, "Name", e.getKey()));
			bucket.appendChild(createElement(doc, "CreationDate", "1982-07-07T16:41:58.000Z"));

			buckets.appendChild(bucket);
		}
		rootElement.appendChild(buckets);
		
		return doc;

	}
	
	public String stringFromDoc(Document doc) {
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
}
