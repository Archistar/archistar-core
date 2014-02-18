package at.archistar.bft.messages;

import java.io.Serializable;

/**
 * This is the abstract base command from which all exchagned commands
 * will inherit. Every received command must be related to a client thus
 * the mandatory clientCmdId.
 * 
 * @author andy
 */
public abstract class AbstractCommand implements Serializable {

	private static final long serialVersionUID = -7606967793233370624L;
}
