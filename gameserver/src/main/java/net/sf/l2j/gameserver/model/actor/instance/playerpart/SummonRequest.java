package net.sf.l2j.gameserver.model.actor.instance.playerpart;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;

/**
 * @author Johnson / 19.07.2017
 */
public class SummonRequest {
    private L2PcInstance destination;
    private L2Skill skill;

    public void setTarget(L2PcInstance destination, L2Skill skill) {
        this.destination = destination;
        this.skill = skill;
    }

    public L2PcInstance getTarget() {
        return destination;
    }

    public L2Skill getSkill() {
        return skill;
    }
}
