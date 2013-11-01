package at.ac.ait.archistar.frontend.archistarftp;

import java.util.HashMap;

import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.InvocationRecord;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.stub.command.AbstractStubDataCommandHandler;

import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;

/**
 * command handler for RETR (put) ftp commands
 * 
 * @author andy
 */
public class FakeStorCommand extends AbstractStubDataCommandHandler {

	private Engine engine;
	
	public FakeStorCommand(Engine engine) {
		super();
		this.engine = engine;
	}
	
	@Override
	protected void processData(Command command, Session session, InvocationRecord iv)
			throws Exception {
		
		String path = command.getParameter(0);
		byte[] data = session.readData();
		
		FSObject obj = new SimpleFile(path, data, new HashMap<String, String>());
		engine.putObject(obj);
	}
}
