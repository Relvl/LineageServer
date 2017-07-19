package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.network.client.game_to_client.SkillCoolTime;

/**
 * Клиент запрашивает время отката скиллов.
 *
 * @author Johnson / 19.07.2017
 */
public class RequestSkillCoolTime extends L2GameClientPacket {

    @Override
    protected void readImpl() { }

    @Override
    protected void runImpl() {
        getClient().sendPacket(new SkillCoolTime(getClient().getActiveChar()));
    }
}
