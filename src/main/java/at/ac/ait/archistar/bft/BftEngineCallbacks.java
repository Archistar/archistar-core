package at.ac.ait.archistar.bft;

import at.ac.ait.archistar.bft.checkpointing.CheckpointMessage;
import at.ac.ait.archistar.bft.commands.AbstractCommand;
import at.ac.ait.archistar.bft.commands.ClientCommand;
import at.ac.ait.archistar.bft.commands.IntraReplicaCommand;
import at.ac.ait.archistar.middleware.commands.TransactionResult;

public interface BftEngineCallbacks {

	void invalidMessageReceived(AbstractCommand msg);

	void replicasMightBeMalicous();

	void sendToReplicas(IntraReplicaCommand cmd);
	
	byte[] executeClientCommand(ClientCommand cmd);

	void invalidCheckpointMessage(CheckpointMessage msg);

	void answerClient(TransactionResult transactionResult);
}
