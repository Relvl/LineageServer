package net.sf.l2j.gameserver.network;

/**
 * @author Johnson / 09.07.2017
 */
public enum GameClientState {
    CONNECTED, // client has just connected
    AUTHED, // client has authed but doesnt has character attached to it yet
    IN_GAME // client has selected a char and is in game
}
