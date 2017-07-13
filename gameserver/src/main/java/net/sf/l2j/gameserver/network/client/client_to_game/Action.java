package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;

public final class Action extends L2GameClientPacket {
    private int _objectId;
    private int _originX, _originY, _originZ;
    private int _actionId;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _originX = readD();
        _originY = readD();
        _originZ = readD();
        _actionId = readC();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (activeChar.inObserverMode()) {
            activeChar.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (activeChar.getActiveRequester() != null || activeChar.isOutOfControl()) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        L2Object obj = (activeChar.getTargetId() == _objectId) ? activeChar.getTarget() : L2World.getInstance().getObject(_objectId);
        if (obj == null) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        switch (_actionId) {
            case 0:
                obj.onAction(activeChar);
                break;

            case 1:
                obj.onActionShift(activeChar);
                break;

            default:
                // Invalid action detected (probably client cheating), log this
                _log.warn("{} requested invalid action: {}", activeChar.getName(), _actionId);
                activeChar.sendPacket(ActionFailed.STATIC_PACKET);
                break;
        }
    }

    @Override
    protected boolean triggersOnActionRequest() {
        return false;
    }
}