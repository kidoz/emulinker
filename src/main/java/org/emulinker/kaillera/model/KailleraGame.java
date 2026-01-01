package org.emulinker.kaillera.model;

import java.util.Collection;

import org.emulinker.kaillera.model.exception.*;

public interface KailleraGame
{
	byte		STATUS_WAITING			= 0;
	byte		STATUS_PLAYING			= 1;
	byte		STATUS_SYNCHRONIZING	= 2;

	String[]	STATUS_NAMES			= { "Waiting", "Playing", "Synchronizing" };

	int getID();

	String getRomName();

	String getClientType();

	KailleraUser getOwner();

	int getPlayerNumber(KailleraUser user);

	int getNumPlayers();

	KailleraUser getPlayer(int playerNumber);

	Collection<? extends KailleraUser> getPlayers();

	int getStatus();

	KailleraServer getServer();

	void droppedPacket(KailleraUser user);

	int join(KailleraUser user) throws JoinGameException;

	void chat(KailleraUser user, String message) throws GameChatException;

	void kick(KailleraUser requester, int userID) throws GameKickException;

	void start(KailleraUser user) throws StartGameException;

	void ready(KailleraUser user, int playerNumber) throws UserReadyException;

	void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException;

	void drop(KailleraUser user, int playerNumber) throws DropGameException;

	void quit(KailleraUser user, int playerNumber) throws DropGameException, QuitGameException, CloseGameException;
}
