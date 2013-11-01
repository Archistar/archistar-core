package at.ac.ait.archistar.bft_core;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.*;
import at.ac.ait.archistar.bft.BftEngine;
import at.ac.ait.archistar.bft.BftEngineCallbacks;
import at.ac.ait.archistar.bft.commands.ClientCommand;
import at.ac.ait.archistar.bft.commands.CommitCommand;
import at.ac.ait.archistar.bft.commands.PrepareCommand;
import at.ac.ait.archistar.bft.commands.PreprepareCommand;
import at.ac.ait.archistar.middleware.commands.TransactionResult;
import at.ac.ait.archistar.middleware.commands.WriteCommand;

public class PerfectRunTest {

	@Test
	public void testPrimaryWithThreeReplicas() {
		
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		BftEngine primary = spy(new BftEngine(0, 1, callbacks));
	
		/* stub: make sure that BftEngine is primary */
		when(primary.isPrimary()).thenReturn(true);
		assertThat(primary.isPrimary()).isEqualTo(true);
		
		int clientId = 1;
		int clientSequence = 1;
		int viewNr = 0;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		/* client sends command to primary */
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		primary.processClientCommand(cmd);
	
		/* capture outgoing preprepare command */
		ArgumentCaptor<PreprepareCommand> ppCommand = ArgumentCaptor.forClass(PreprepareCommand.class);
		verify(callbacks, times(1)).sendToReplicas(ppCommand.capture());
		String digest = ppCommand.getValue().getClientOperationId();
		int sequence = ppCommand.getValue().getSequence();
		
		/* primary receives prepare commands */
		primary.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 1, digest));
		primary.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 2, digest));
		primary.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 3, digest));
		verify(callbacks, times(1)).sendToReplicas(isA(CommitCommand.class));

		/* primary receives commit messages */
		primary.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 1));
		primary.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 2));
		primary.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 3));
		
		/* primary executes operation */
		verify(callbacks, times(1)).executeClientCommand(cmd);
		verify(callbacks, times(1)).answerClient(isA(TransactionResult.class));
	}
	
	@Test
	public void primaryWithTwoReplicas() {
		
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		BftEngine primary = spy(new BftEngine(0, 1, callbacks));
	
		/* stub: make sure that BftEngine is primary */
		when(primary.isPrimary()).thenReturn(true);
		assertThat(primary.isPrimary()).isEqualTo(true);
		
		int clientId = 1;
		int clientSequence = 1;
		int viewNr = 0;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		/* client sends command to primary */
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		primary.processClientCommand(cmd);
	
		/* capture outgoing preprepare command */
		ArgumentCaptor<PreprepareCommand> ppCommand = ArgumentCaptor.forClass(PreprepareCommand.class);
		verify(callbacks, times(1)).sendToReplicas(ppCommand.capture());
		String digest = ppCommand.getValue().getClientOperationId();
		int sequence = ppCommand.getValue().getSequence();
		
		/* primary receives prepare commands */
		primary.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 1, digest));
		primary.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 2, digest));
		verify(callbacks, times(1)).sendToReplicas(isA(CommitCommand.class));

		/* primary receives commit messages */
		primary.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 1));
		primary.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 3));
		
		/* primary executes operation */
		verify(callbacks, times(1)).executeClientCommand(cmd);
		verify(callbacks, times(1)).answerClient(isA(TransactionResult.class));
	}
	
	@Test
	public void testReplicaWithTwoReplicasAndPrimary() {
		
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		BftEngine replica = spy(new BftEngine(1, 1, callbacks));
	
		/* stub: make sure that BftEngine is primary */
		when(replica.isPrimary()).thenReturn(false);
		assertThat(replica.isPrimary()).isEqualTo(false);
		
		int clientId = 1;
		int clientSequence = 1;
		int viewNr = 0;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		
		/* client sends command to primary */
		replica.processClientCommand(cmd);
		
		int sequence = -1;
		int priorSequence = -2;
		String digest = cmd.getClientOperationId();
		
		replica.processIntraReplicaCommand(new PreprepareCommand(viewNr, sequence, 0, digest, priorSequence));
		verify(callbacks, times(1)).sendToReplicas(isA(PrepareCommand.class));
	
		/* primary receives prepare commands */
		replica.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 0, digest));
		replica.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 2, digest));
		replica.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 3, digest));
		verify(callbacks, times(1)).sendToReplicas(isA(CommitCommand.class));

		/* primary receives commit messages */
		replica.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 0));
		replica.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 2));
		replica.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 3));
		
		/* primary executes operation */
		verify(callbacks, times(1)).executeClientCommand(cmd);
		verify(callbacks, times(1)).answerClient(isA(TransactionResult.class));
	}
	
	@Test
	public void testReplicaWithOneReplicaAndPrimary() {
		
		BftEngineCallbacks callbacks = mock(BftEngineCallbacks.class);
		BftEngine replica = spy(new BftEngine(1, 1, callbacks));
	
		/* stub: make sure that BftEngine is primary */
		when(replica.isPrimary()).thenReturn(false);
		assertThat(replica.isPrimary()).isEqualTo(false);
		
		int clientId = 1;
		int clientSequence = 1;
		int viewNr = 0;
		String fragmentId = "fragment-id-1";
		byte[] data = {1, 2, 3, 4, 5, 6, 7, 8 , 9, 10};
		
		ClientCommand cmd = new WriteCommand(clientId, clientSequence, fragmentId, data);
		
		/* client sends command to primary */
		replica.processClientCommand(cmd);
		
		int sequence = -1;
		int priorSequence = -2;
		String digest = cmd.getClientOperationId();
		
		replica.processIntraReplicaCommand(new PreprepareCommand(viewNr, sequence, 0, digest, priorSequence));
		verify(callbacks, times(1)).sendToReplicas(isA(PrepareCommand.class));
	
		/* primary receives prepare commands */
		replica.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 0, digest));
		replica.processIntraReplicaCommand(new PrepareCommand(viewNr, sequence, 3, digest));
		verify(callbacks, times(1)).sendToReplicas(isA(CommitCommand.class));

		/* primary receives commit messages */
		replica.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 0));
		replica.processIntraReplicaCommand(new CommitCommand(viewNr, sequence, 3));
		
		/* primary executes operation */
		verify(callbacks, times(1)).executeClientCommand(cmd);
		verify(callbacks, times(1)).answerClient(isA(TransactionResult.class));
	}
}