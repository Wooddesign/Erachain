package network;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.mapdb.Fun.Tuple2;

import core.transaction.Transaction;
import network.message.Message;

public interface ConnectionCallback {

	void onConnect(Peer peer, boolean asNew);
	void tryDisconnect(Peer peer, int banForMinutes, String error);
	//void banOnError(Peer peer, String error);
	boolean isKnownAddress(InetAddress address, boolean andUsed);
	boolean isKnownPeer(Peer peer, boolean andUsed);
	List<Peer> getActivePeers(boolean onlyWhite);
	int getActivePeersCounter(boolean onlyWhite);
	Peer getKnownPeer(Peer peer);
	void onMessage(Message message);
	Peer startPeer(Socket socket);
	void broadcastUnconfirmedToPeer(List<Transaction> transactions, Peer peer);
	
}
