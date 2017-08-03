package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.playerpart.PunishLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IllegalPlayerAction implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IllegalPlayerAction.class);

    public static final int PUNISH_BROADCAST = 1;
    public static final int PUNISH_KICK = 2;
    public static final int PUNISH_KICKBAN = 3;
    public static final int PUNISH_JAIL = 4;
    private final String _message;
    private final int _punishment;
    private final L2PcInstance _actor;

    public IllegalPlayerAction(L2PcInstance actor, String message, int punishment) {
        _message = message;
        _punishment = punishment;
        _actor = actor;

        switch (punishment) {
            case PUNISH_KICK:
                _actor.sendMessage("You will be kicked for illegal action, GM informed.");
                break;
            case PUNISH_KICKBAN:
                _actor.setAccessLevel(-100);
                // TODO send ban to login server
                _actor.sendMessage("You are banned for illegal action, GM informed.");
                break;
            case PUNISH_JAIL:
                _actor.sendMessage("Illegal action performed!");
                _actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
                break;
        }
    }

    @Override
    public void run() {
        LOGGER.warn("AUDIT: {} -> {}, {}", _message, _actor, _punishment);

        GmListTable.broadcastMessageToGMs(_message);

        switch (_punishment) {
            case PUNISH_BROADCAST:
                return;
            case PUNISH_KICK:
                _actor.logout(false);
                break;
            case PUNISH_KICKBAN:
                _actor.logout();
                break;
            case PUNISH_JAIL:
                _actor.setPunishLevel(PunishLevel.JAIL, Config.DEFAULT_PUNISH_PARAM);
                break;
        }
    }
}