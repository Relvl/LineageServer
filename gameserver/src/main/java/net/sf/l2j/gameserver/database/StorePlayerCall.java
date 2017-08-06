package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import net.sf.l2j.gameserver.model.actor.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 04.08.2017
 */
public class StorePlayerCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorePlayerCall.class);
    @OrmParamOut(1)
    private Integer resultCode;

    @OrmParamIn(2)
    private final Integer playerId;
    @OrmParamIn(3)
    private final String login;
    @OrmParamIn(4)
    private final String name;
    @OrmParamIn(5)
    private final String title;
    @OrmParamIn(6)
    private final UDT_Level level;
    @OrmParamIn(7)
    private final HeadedLocation position;
    @OrmParamIn(8)
    private final PcAppearance appearance;

    public StorePlayerCall(L2PcInstance player, String login) {
        super("player__store", 7, true);
        this.login = login;
        playerId = player.getObjectId();
        name = player.getName();
        title = player.getTitle();
        position = player.getPosition();
        level = new UDT_Level(player.getLevel(), player.getStat().getExp(), player.getStat().getSp());
        appearance = player.getAppearance();
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "StorePlayerCall{" +
                "resultCode=" + resultCode +
                ", playerId=" + playerId +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", level=" + level +
                ", position=" + position +
                ", appearance=" + appearance +
                '}';
    }
}
