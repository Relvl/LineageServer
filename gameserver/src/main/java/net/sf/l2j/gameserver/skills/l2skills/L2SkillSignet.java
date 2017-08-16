package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2EffectPointInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class L2SkillSignet extends L2Skill {
    private final int effectNpcId;
    public int effectId;

    public L2SkillSignet(StatsSet set) {
        super(set);
        effectNpcId = set.getInteger("effectNpcId", -1);
        effectId = set.getInteger("effectId", -1);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        if (caster.isAlikeDead()) { return; }

        NpcTemplate template = NpcTable.getInstance().getTemplate(effectNpcId);
        L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, caster);
        effectPoint.setCurrentHp(effectPoint.getMaxHp());
        effectPoint.setCurrentMp(effectPoint.getMaxMp());

        int x = caster.getX();
        int y = caster.getY();
        int z = caster.getZ();

        if (caster instanceof L2PcInstance && getTargetType() == ESkillTargetType.TARGET_GROUND) {
            Location wordPosition = ((L2PcInstance) caster).getCurrentSkillWorldPosition();

            if (wordPosition != null) {
                x = wordPosition.getX();
                y = wordPosition.getY();
                z = wordPosition.getZ();
            }
        }
        getEffects(caster, effectPoint);

        effectPoint.setIsInvul(true);
        effectPoint.spawnMe(x, y, z);
    }
}