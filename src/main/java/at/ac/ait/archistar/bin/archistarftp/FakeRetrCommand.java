package at.ac.ait.archistar.bin.archistarftp;

import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.InvocationRecord;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.stub.command.AbstractStubDataCommandHandler;

import at.ac.ait.archistar.Engine;
import at.ac.ait.archistar.data.user.FSObject;
import at.ac.ait.archistar.data.user.SimpleFile;

/**
 * command handler for RETR (get) ftp commands
 * 
 * @author andy
 */
public class FakeRetrCommand extends AbstractStubDataCommandHandler {

	private Engine engine;
	
	public FakeRetrCommand(Engine engine) {
		super();
		this.engine = engine;
	}
	
	@Override
	protected void processData(Command command, Session session, InvocationRecord ir)
			throws Exception {
		
		String path = command.getParameter(0);
		
		FSObject obj = engine.getObject(path);
		byte[] data = {};
		
		if (obj instanceof SimpleFile) {
			data = ((SimpleFile) obj).getData();
		}
		session.sendData(data, data.length);
	}
}
