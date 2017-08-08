package net.sf.l2j.gameserver;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.handler.chathandlers.*;

/**
 * @author Johnson / 12.07.2017
 */
public enum EChatType {
    ALL(new ChatHandlerAll()), // msg
    SHOUT(new ChatHandlerShout()), // !msg
    TELL(new ChatHandlerTell()), // "target msg
    PARTY(new ChatHandlerParty()),  // #msg
    CLAN(new ChatHandlerClan()), // @msg
    GM(null),
    PETITION_PLAYER(new ChatHandlerPetition()), // &msg
    PETITION_GM(PETITION_PLAYER.handler), // *msg
    TRADE(new ChatHandlerTrade()), // +msg
    ALLIANCE(new ChatHandlerAlliance()), // $msg
    ANNOUNCEMENT(null),
    /** Судно. Не пытаться слать чат с этим типом, клиент вылетит с крашем. */
    BOAT(null),
    L2FRIEND(null),
    MSNCHAT(null),
    PARTYMATCH_ROOM(new ChatHandlerPartyMatchRoom()), //
    /** Бледно-красный, попутно вылазит уведомление справа (не умеет в русский). */
    PARTYROOM_COMMANDER(new ChatHandlerPartyRoomCommander()), //
    /** Бледно-желтый, слабо отличимый от белого. */
    PARTYROOM_ALL(new ChatHandlerPartyRoomAll()), // `msg
    HERO_VOICE(new ChatHandlerHeroVoice()), // %msg
    CRITICAL_ANNOUNCE(null),

    UNKNOWN(null);

    private final IChatHandler handler;

    EChatType(IChatHandler handler) {this.handler = handler;}

    public static EChatType getByCode(int code) {
        return code >= values().length ? UNKNOWN : values()[code];
    }

    public int getCode() {
        return ordinal();
    }

    public String getName() {
        return name();
    }

    public IChatHandler getHandler() {
        return handler;
    }
}
