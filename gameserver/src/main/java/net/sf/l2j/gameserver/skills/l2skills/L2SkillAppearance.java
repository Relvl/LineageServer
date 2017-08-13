package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillAppearance extends L2Skill {
    private final int faceId;
    private final int hairColorId;
    private final int hairStyleId;

    public L2SkillAppearance(StatsSet set) {
        super(set);
        faceId = set.getInteger("faceId", -1);
        hairColorId = set.getInteger("hairColorId", -1);
        hairStyleId = set.getInteger("hairStyleId", -1);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        try {
            for (L2Object target : targets) {
                if (target.isPlayer()) {
                    if (faceId >= 0) { target.getActingPlayer().getAppearance().setFace(faceId); }
                    if (hairColorId >= 0) { target.getActingPlayer().getAppearance().setHairColor(hairColorId); }
                    if (hairStyleId >= 0) { target.getActingPlayer().getAppearance().setHairStyle(hairStyleId); }
                    target.getActingPlayer().broadcastUserInfo();
                }
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
        }
    }
}