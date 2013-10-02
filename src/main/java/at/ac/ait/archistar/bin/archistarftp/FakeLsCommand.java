package at.ac.ait.archistar.bin.archistarftp;

import java.util.Set;

import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.InvocationRecord;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.stub.command.AbstractStubCommandHandler;

import at.ac.ait.archistar.Engine;

/**
 * command handler for LIST ftp commands
 * 
 * @author andy
 */
public class FakeLsCommand extends AbstractStubCommandHandler {
	
	private Engine engine;

	public FakeLsCommand(Engine engine) {
		super();
		this.engine = engine;
	}
	
	@Override
	protected void handleCommand(Command cmd, Session session,
			InvocationRecord record) throws Exception {
		
		Set<String> files = engine.listObjects("/");
		
		String result = "";
		for(String file : files) {
			result += (file + "\n");
		}
		
		setReplyText(result);
		setReplyCode(202);
		sendReply(session);
	}
}
