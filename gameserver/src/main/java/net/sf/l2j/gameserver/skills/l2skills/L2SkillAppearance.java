package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillAppearance extends L2Skill {
    private final int _faceId;
    private final int _hairColorId;
    private final int _hairStyleId;

    public L2SkillAppearance(StatsSet set) {
        super(set);

        _faceId = set.getInteger("faceId", -1);
        _hairColorId = set.getInteger("hairColorId", -1);
        _hairStyleId = set.getInteger("hairStyleId", -1);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        try {
            for (L2Object target : targets) {
                if (target instanceof L2PcInstance) {
                    L2PcInstance targetPlayer = (L2PcInstance) target;
                    if (_faceId >= 0) { targetPlayer.getAppearance().setFace(_faceId); }
                    if (_hairColorId >= 0) { targetPlayer.getAppearance().setHairColor(_hairColorId); }
                    if (_hairStyleId >= 0) { targetPlayer.getAppearance().setHairStyle(_hairStyleId); }

                    targetPlayer.broadcastUserInfo();
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}