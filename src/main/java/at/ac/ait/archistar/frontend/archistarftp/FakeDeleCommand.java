package at.ac.ait.archistar.frontend.archistarftp;

import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.InvocationRecord;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.stub.command.AbstractStubCommandHandler;

import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.frontend.FSObject;

public class FakeDeleCommand extends AbstractStubCommandHandler {

	private Engine engine;
	
	public FakeDeleCommand(Engine engine) {
		this.engine = engine;
	}
	
	@Override
	protected void handleCommand(Command cmd, Session session, InvocationRecord ir) throws Exception {
		
		String path = cmd.getParameter(0);
		FSObject obj = engine.getObject(path);
		String reply = "file not found";

		if (obj != null) {
			if( engine.deleteObject(obj) == 0) {
				reply = "file deleted";
			} else {
				reply = "error during delete";
			}
		}
		
		setReplyText(reply);
		setReplyCode(202);
		sendReply(session);
	}
}
