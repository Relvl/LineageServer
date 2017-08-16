package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.TimeStamp;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.List;
import java.util.stream.Collectors;

public class SkillCoolTime extends L2GameServerPacket {
    private List<TimeStamp> timeStampList;

    public SkillCoolTime(L2PcInstance cha) {
        timeStampList = cha.getReuseTimeStamps().stream().filter(r -> !r.hasNotPassed()).collect(Collectors.toList());
    }

    @Override
    protected void writeImpl() {
        writeC(0xc1);
        writeD(timeStampList.size()); // list size
        for (TimeStamp ts : timeStampList) {
            writeD(ts.getSkillId());
            writeD(ts.getSkillLvl());
            writeD((int) ts.getReuse() / 1000);
            writeD((int) ts.getRemaining() / 1000);
        }
    }
}