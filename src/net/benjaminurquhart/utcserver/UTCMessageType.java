package net.benjaminurquhart.utcserver;

public enum UTCMessageType {

	CONFIGURATION,
	PLAYER_UPDATE,
	PLAYER_JOIN,
	PLAYER_LEAVE,
	BATTLE_UPDATE,
	FLAGS_UPDATE,
	HEARTBEAT; // Custom
}
