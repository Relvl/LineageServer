package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ChanceSkillList extends ConcurrentHashMap<IChanceSkillTrigger, ChanceCondition> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChanceSkillList.class);

    private final L2Character character;

    public ChanceSkillList(L2Character owner) {
        character = owner;
    }

    public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit) {
        int event;
        if (ownerWasHit) {
            event = SkillTriggerType.ON_ATTACKED.getMask() | SkillTriggerType.ON_ATTACKED_HIT.getMask();
            if (wasCrit) {
                event |= SkillTriggerType.ON_ATTACKED_CRIT.getMask();
            }
        }
        else {
            event = SkillTriggerType.ON_HIT.getMask();
            if (wasCrit) {
                event |= SkillTriggerType.ON_CRIT.getMask();
            }
        }

        onChanceSkillEvent(event, target);
    }

    public void onSkillHit(L2Character target, boolean ownerWasHit, boolean wasMagic, boolean wasOffensive) {
        int event;
        if (ownerWasHit) {
            event = SkillTriggerType.ON_HIT_BY_SKILL.getMask();
            if (wasOffensive) {
                event |= SkillTriggerType.ON_HIT_BY_OFFENSIVE_SKILL.getMask();
                event |= SkillTriggerType.ON_ATTACKED.getMask();
            }
            else {
                event |= SkillTriggerType.ON_HIT_BY_GOOD_MAGIC.getMask();
            }
        }
        else {
            event = SkillTriggerType.ON_CAST.getMask();
            event |= wasMagic ? SkillTriggerType.ON_MAGIC.getMask() : SkillTriggerType.ON_PHYSICAL.getMask();
            event |= wasOffensive ? SkillTriggerType.ON_MAGIC_OFFENSIVE.getMask() : SkillTriggerType.ON_MAGIC_GOOD.getMask();
        }

        onChanceSkillEvent(event, target);
    }

    public void onStart() { onChanceSkillEvent(SkillTriggerType.ON_START.getMask(), character); }

    public void onActionTime() { onChanceSkillEvent(SkillTriggerType.ON_ACTION_TIME.getMask(), character); }

    public void onExit() { onChanceSkillEvent(SkillTriggerType.ON_EXIT.getMask(), character); }

    public void onEvadedHit(L2Character attacker) { onChanceSkillEvent(SkillTriggerType.ON_EVADED_HIT.getMask(), attacker); }

    public void onChanceSkillEvent(int event, L2Character target) {
        if (character.isDead()) { return; }

        for (Entry<IChanceSkillTrigger, ChanceCondition> entry : entrySet()) {
            IChanceSkillTrigger trigger = entry.getKey();
            ChanceCondition cond = entry.getValue();

            if (cond != null && cond.trigger(event)) {
                if (trigger.isSkill()) {
                    makeCast((L2Skill) trigger, target);
                }
                else if (trigger.isEffect()) {
                    makeCast((EffectChanceSkillTrigger) trigger, target);
                }
            }
        }
    }

    private void makeCast(L2Skill skill, L2Character target) {
        try {
            if (skill.getWeaponDependancy(character) && skill.checkCondition(character, target, false)) {
                if (skill.triggersChanceSkill()) // skill will trigger another skill, but only if its not chance skill
                {
                    skill = SkillTable.getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
                    if (skill == null || skill.getSkillType() == L2SkillType.NOTDONE) { return; }
                }

                if (character.isSkillDisabled(skill)) { return; }

                if (skill.getReuseDelay() > 0) { character.disableSkill(skill, skill.getReuseDelay()); }

                L2Object[] targets = skill.getTargetList(character, false, target);

                if (targets.length == 0) { return; }

                L2Character firstTarget = (L2Character) targets[0];

                ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());

                character.broadcastPacket(new MagicSkillLaunched(character, skill.getId(), skill.getLevel(), targets));
                character.broadcastPacket(new MagicSkillUse(character, firstTarget, skill.getId(), skill.getLevel(), 0, 0));

                // Launch the magic skill and calculate its effects
                // TODO: once core will support all possible effects, use effects (not handler)
                if (handler != null) { handler.useSkill(character, skill, targets); }
                else { skill.useSkill(character, targets); }
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
        }
    }

    private void makeCast(EffectChanceSkillTrigger effect, L2Character target) {
        try {
            if (effect == null || !effect.triggersChanceSkill()) { return; }

            L2Skill triggered = SkillTable.getInfo(effect.getTriggeredChanceId(), effect.getTriggeredChanceLevel());
            if (triggered == null) { return; }
            L2Character caster = triggered.getTargetType() == ESkillTargetType.TARGET_SELF ? character : effect.getEffector();

            if (caster == null || triggered.getSkillType() == L2SkillType.NOTDONE || caster.isSkillDisabled(triggered)) { return; }

            if (triggered.getReuseDelay() > 0) { caster.disableSkill(triggered, triggered.getReuseDelay()); }

            L2Object[] targets = triggered.getTargetList(caster, false, target);

            if (targets.length == 0) { return; }

            L2Character firstTarget = (L2Character) targets[0];

            ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(triggered.getSkillType());

            character.broadcastPacket(new MagicSkillLaunched(character, triggered.getId(), triggered.getLevel(), targets));
            character.broadcastPacket(new MagicSkillUse(character, firstTarget, triggered.getId(), triggered.getLevel(), 0, 0));

            // Launch the magic skill and calculate its effects
            // TODO: once core will support all possible effects, use effects (not handler)
            if (handler != null) { handler.useSkill(caster, triggered, targets); }
            else { triggered.useSkill(caster, targets); }
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
        }
    }
}