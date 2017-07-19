package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillDefault extends L2Skill {
    public L2SkillDefault(StatsSet set) {
        super(set);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        caster.sendPacket(ActionFailed.STATIC_PACKET);
        caster.sendMessage("Skill " + getId() + " [" + getSkillType() + "] isn't implemented.");
    }
}