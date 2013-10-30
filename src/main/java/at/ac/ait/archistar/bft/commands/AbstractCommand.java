package at.ac.ait.archistar.bft.commands;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

/**
 * This is the abstract base command from which all exchagned commands
 * will inherit. Every received command must be related to a client thus
 * the mandatory clientCmdId.
 * 
 * TODO: make sure that the clientcmdid is unique
 * TODO: make sure that only one client communicates on a client channel
 * 
 * @author andy
 */
public abstract class AbstractCommand implements Serializable {

	private static final long serialVersionUID = -7606967793233370624L;
	
	private ChannelHandlerContext ctx;

	public void setContext(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
	
	public ChannelHandlerContext getContext() {
		return this.ctx;
	}
}
