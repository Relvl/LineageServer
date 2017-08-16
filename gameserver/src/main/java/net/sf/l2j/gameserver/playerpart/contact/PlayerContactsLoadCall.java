package net.sf.l2j.gameserver.playerpart.contact;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 12.08.2017
 */
public class PlayerContactsLoadCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerContactsLoadCall.class);

    @OrmParamIn(1)
    private final Integer playerId;
    @OrmParamOut(value = 2, cursorClass = ContactRow.class)
    private final List<ContactRow> contacts = new ArrayList<>();
    @OrmParamOut(3)
    private Integer resultCode;

    protected PlayerContactsLoadCall(Integer playerId) {
        super("player_contacts_load", 3, false);
        this.playerId = playerId;
    }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    public Integer getResultCode() { return resultCode; }

    public List<ContactRow> getContacts() { return contacts; }

    @Override
    protected boolean throwErrorOnRecultCode() { return true; }

    @Override
    public String toString() {
        return "PlayerContactsLoadCall{" +
                "playerId=" + playerId +
                ", contacts=" + contacts +
                ", resultCode=" + resultCode +
                '}';
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class ContactRow {
        @OrmParamCursor("PLAYER_ID")
        private Integer playerId;
        @OrmParamCursor("CONTACT_ID")
        private Integer contactId;
        @OrmParamCursor("CONTACT_TYPE")
        private Integer contactType;

        public Integer getPlayerId() { return playerId; }

        public Integer getContactId() { return contactId; }

        public Integer getContactType() { return contactType; }

        @Override
        public String toString() {
            return "ContactRow{" +
                    "playerId=" + playerId +
                    ", contactId=" + contactId +
                    ", contactType=" + contactType +
                    '}';
        }
    }
}
