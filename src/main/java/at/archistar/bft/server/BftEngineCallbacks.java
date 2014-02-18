package at.archistar.bft.server;

import at.archistar.bft.messages.AbstractCommand;
import at.archistar.bft.messages.CheckpointMessage;
import at.archistar.bft.messages.ClientCommand;
import at.archistar.bft.messages.IntraReplicaCommand;
import at.archistar.bft.messages.TransactionResult;

public interface BftEngineCallbacks {

	void invalidMessageReceived(AbstractCommand msg);

	void replicasMightBeMalicous();

	void sendToReplicas(IntraReplicaCommand cmd);
	
	byte[] executeClientCommand(ClientCommand cmd);

	void invalidCheckpointMessage(CheckpointMessage msg);

	void answerClient(TransactionResult transactionResult);
}
