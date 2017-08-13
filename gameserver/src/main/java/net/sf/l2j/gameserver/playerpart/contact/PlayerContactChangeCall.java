package net.sf.l2j.gameserver.playerpart.contact;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 12.08.2017
 */
public class PlayerContactChangeCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerContactChangeCall.class);

    @OrmParamOut(1)
    private Integer resultCode;
    @OrmParamIn(2)
    private final Integer playerId;
    @OrmParamIn(3)
    private final Integer contactId;
    @OrmParamIn(4)
    private final Integer contactType;

    protected PlayerContactChangeCall(Integer playerId, Integer contactId, EContactType contactType) {
        super("player_contacts_change", 3, true);
        this.playerId = playerId;
        this.contactId = contactId;
        this.contactType = contactType.getType();
    }

    @Override
    public Integer getResultCode() { return resultCode; }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    protected boolean throwErrorOnRecultCode() { return true; }

    @Override
    public String toString() {
        return "PlayerContactChangeCall{" +
                "resultCode=" + resultCode +
                ", playerId=" + playerId +
                ", contactId=" + contactId +
                ", contactType=" + contactType +
                '}';
    }
}
