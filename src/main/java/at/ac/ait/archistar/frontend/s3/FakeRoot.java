package at.ac.ait.archistar.frontend.s3;

import java.io.StringWriter;
import java.io.Writer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import at.ac.ait.archistar.middleware.Engine;

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
	
	@GET
	@Produces ("text/xml")
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
}
