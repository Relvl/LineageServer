package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

import java.util.List;

/**
 * @author _drunk_
 */
public class Sweep implements ISkillHandler {
    private static final L2SkillType[] SKILL_IDS =
            {
                    L2SkillType.SWEEP
            };

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) {
        if (!(activeChar instanceof L2PcInstance)) { return; }

        L2PcInstance player = (L2PcInstance) activeChar;

        for (L2Object target : targets) {
            if (!(target instanceof L2Attackable)) { continue; }

            L2Attackable monster = (L2Attackable) target;
            if (!monster.isSpoiled()) { continue; }

            List<IntIntHolder> items = monster.getSweepItems();
            if (items.isEmpty()) { continue; }

            for (IntIntHolder item : items) {
                if (player.isInParty()) { player.getParty().distributeItem(player, item, true, monster); }
                else { player.addItem(EItemProcessPurpose.SWEEP, item.getId(), item.getValue(), player, true); }
            }
            items.clear();
        }
    }

    @Override
    public L2SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}