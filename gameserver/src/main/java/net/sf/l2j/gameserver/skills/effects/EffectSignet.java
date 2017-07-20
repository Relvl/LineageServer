package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2EffectPointInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignet;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import java.util.ArrayList;
import java.util.List;

public class EffectSignet extends L2Effect {
    private L2Skill _skill;
    private L2EffectPointInstance _actor;
    private boolean _srcInArena;

    public EffectSignet(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public L2EffectType getEffectType() {
        return L2EffectType.SIGNET_EFFECT;
    }

    @Override
    public boolean onStart() {
        if (getSkill() instanceof L2SkillSignet) {
            _skill = SkillTable.getInfo(((L2SkillSignet) getSkill()).effectId, getLevel());
        }
        else if (getSkill() instanceof L2SkillSignetCasttime) {
            _skill = SkillTable.getInfo(((L2SkillSignetCasttime) getSkill()).effectId, getLevel());
        }

        _actor = (L2EffectPointInstance) getEffected();
        _srcInArena = getEffector().isInArena();
        return true;
    }

    @Override
    public boolean onActionTime() {
        if (_skill == null) { return true; }

        int mpConsume = _skill.getMpConsume();
        if (mpConsume > getEffector().getCurrentMp()) {
            getEffector().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
            return false;
        }
        getEffector().reduceCurrentMp(mpConsume);

        List<L2Character> targets = new ArrayList<>();
        for (L2Character cha : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, getSkill().getSkillRadius())) {
            if (_skill.isOffensive() && !L2Skill.checkForAreaOffensiveSkills(getEffector(), cha, _skill, _srcInArena)) { continue; }

            // there doesn't seem to be a visible effect with MagicSkillLaunched packet...
            _actor.broadcastPacket(new MagicSkillUse(_actor, cha, _skill.getId(), _skill.getLevel(), 0, 0));
            targets.add(cha);
        }

        if (!targets.isEmpty()) { getEffector().callSkill(_skill, targets.toArray(new L2Character[targets.size()])); }

        return true;
    }

    @Override
    public void onExit() {
        if (_actor != null) { _actor.deleteMe(); }
    }
}