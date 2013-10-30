package at.ac.ait.archistar.bft_core;

import org.junit.Test;

import static org.mockito.Mockito.*;
import at.ac.ait.archistar.bft.BftEngine;
import at.ac.ait.archistar.bft.BftEngineCallbacks;
import at.ac.ait.archistar.bft.commands.ClientCommand;
import at.ac.ait.archistar.bft.commands.IntraReplicaCommand;
import at.ac.ait.archistar.bft.commands.PrepareCommand;
import at.ac.ait.archistar.middleware.commands.WriteCommand;

public class InitialMessagesTest {
	
	@Test
	public void primarySendsPreprareAfterReceivingClientMessage() {
		
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		BftEngine primary = spy(new BftEngine(1, 1, callbacks));
	
		/* stub: make sure that BftEngine is primary */
		when(primary.isPrimary()).thenReturn(true);
		
		int clientId = 1;
		int clientSequence = 1;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		primary.processClientCommand(cmd);
		
		/* If message was called, expect that a new PreprepareCommand is sent to all other replicas */
		verify(callbacks).sendToReplicas(any(PrepareCommand.class));
	}
	
	@Test
	public void NonPrimaryDoesntSendPreprareAfterReceivingClientMessage() {
		
		/* this throws an exception if any callback is called */
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		
		BftEngine primary = spy(new BftEngine(1, 1, callbacks));
	
		/* stub: make sure that BftEngine is not primary */
		when(primary.isPrimary()).thenReturn(false);
		
		int clientId = 1;
		int clientSequence = 1;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		primary.processClientCommand(cmd);
		
		verify(callbacks, never()).sendToReplicas(any(IntraReplicaCommand.class));
	}
}