package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDrain;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class L2CubicInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2CubicInstance.class);

    // Type of cubics
    public static final int STORM_CUBIC = 1;
    public static final int VAMPIRIC_CUBIC = 2;
    public static final int LIFE_CUBIC = 3;
    public static final int VIPER_CUBIC = 4;
    public static final int POLTERGEIST_CUBIC = 5;
    public static final int BINDING_CUBIC = 6;
    public static final int AQUA_CUBIC = 7;
    public static final int SPARK_CUBIC = 8;
    public static final int ATTRACT_CUBIC = 9;

    // Max range of cubic skills
    public static final int MAX_MAGIC_RANGE = 900;

    // Cubic skills
    public static final int SKILL_CUBIC_HEAL = 4051;
    public static final int SKILL_CUBIC_CURE = 5579;

    protected L2PcInstance _owner;
    protected L2Character _target;

    protected int _id;
    protected int _matk;
    protected int _activationtime;
    protected int _activationchance;
    protected boolean _active;
    private final boolean _givenByOther;

    protected List<L2Skill> _skills = new ArrayList<>();

    private Future<?> _disappearTask;
    private Future<?> _actionTask;

    public L2CubicInstance(L2PcInstance owner, int id, int level, int mAtk, int activationtime, int activationchance, int totallifetime, boolean givenByOther) {
        _owner = owner;
        _id = id;
        _matk = mAtk;
        _activationtime = activationtime * 1000;
        _activationchance = activationchance;
        _active = false;
        _givenByOther = givenByOther;

        switch (_id) {
            case STORM_CUBIC:
                _skills.add(SkillTable.getInfo(4049, level));
                break;

            case VAMPIRIC_CUBIC:
                _skills.add(SkillTable.getInfo(4050, level));
                break;

            case LIFE_CUBIC:
                _skills.add(SkillTable.getInfo(4051, level));
                doAction();
                break;

            case VIPER_CUBIC:
                _skills.add(SkillTable.getInfo(4052, level));
                break;

            case POLTERGEIST_CUBIC:
                _skills.add(SkillTable.getInfo(4053, level));
                _skills.add(SkillTable.getInfo(4054, level));
                _skills.add(SkillTable.getInfo(4055, level));
                break;

            case BINDING_CUBIC:
                _skills.add(SkillTable.getInfo(4164, level));
                break;

            case AQUA_CUBIC:
                _skills.add(SkillTable.getInfo(4165, level));
                break;

            case SPARK_CUBIC:
                _skills.add(SkillTable.getInfo(4166, level));
                break;

            case ATTRACT_CUBIC:
                _skills.add(SkillTable.getInfo(5115, level));
                _skills.add(SkillTable.getInfo(5116, level));
                break;
        }
        _disappearTask = ThreadPoolManager.getInstance().schedule(new Disappear(), totallifetime); // disappear
    }

    public synchronized void doAction() {
        if (_active) { return; }

        _active = true;

        switch (_id) {
            case AQUA_CUBIC:
            case BINDING_CUBIC:
            case SPARK_CUBIC:
            case STORM_CUBIC:
            case POLTERGEIST_CUBIC:
            case VAMPIRIC_CUBIC:
            case VIPER_CUBIC:
            case ATTRACT_CUBIC:
                _actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(_activationchance), 0, _activationtime);
                break;

            case LIFE_CUBIC:
                _actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(), 0, _activationtime);
                break;
        }
    }

    public int getId() {
        return _id;
    }

    public L2PcInstance getOwner() {
        return _owner;
    }

    public final int getMCriticalHit(L2Character target, L2Skill skill) {
        return _owner.getMCriticalHit(target, skill);
    }

    public int getMAtk() {
        return _matk;
    }

    public void stopAction() {
        _target = null;
        if (_actionTask != null) {
            _actionTask.cancel(true);
            _actionTask = null;
        }
        _active = false;
    }

    public void cancelDisappear() {
        if (_disappearTask != null) {
            _disappearTask.cancel(true);
            _disappearTask = null;
        }
    }

    /** this sets the enemy target for a cubic */
    public void getCubicTarget() {
        try {
            _target = null;
            L2Object ownerTarget = _owner.getTarget();
            if (ownerTarget == null) { return; }

            // Duel targeting
            if (_owner.isInDuel()) {
                L2PcInstance PlayerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
                L2PcInstance PlayerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();

                if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel()) {
                    L2Party partyA = PlayerA.getParty();
                    L2Party partyB = PlayerB.getParty();
                    L2Party partyEnemy = null;

                    if (partyA != null) {
                        if (partyA.getPartyMembers().contains(_owner)) {
                            if (partyB != null) { partyEnemy = partyB; }
                            else { _target = PlayerB; }
                        }
                        else { partyEnemy = partyA; }
                    }
                    else {
                        if (PlayerA == _owner) {
                            if (partyB != null) { partyEnemy = partyB; }
                            else { _target = PlayerB; }
                        }
                        else { _target = PlayerA; }
                    }
                    if (_target == PlayerA || _target == PlayerB) { if (_target == ownerTarget) { return; } }
                    if (partyEnemy != null) {
                        if (partyEnemy.getPartyMembers().contains(ownerTarget)) { _target = (L2Character) ownerTarget; }
                        return;
                    }
                }
                if (PlayerA != _owner && ownerTarget == PlayerA) {
                    _target = PlayerA;
                    return;
                }
                if (PlayerB != _owner && ownerTarget == PlayerB) {
                    _target = PlayerB;
                    return;
                }
                _target = null;
                return;
            }

            // Olympiad targeting
            if (_owner.isInOlympiadMode()) {
                if (_owner.isOlympiadStart()) {
                    if (ownerTarget instanceof L2Playable) {
                        final L2PcInstance targetPlayer = ownerTarget.getActingPlayer();
                        if (targetPlayer != null && targetPlayer.getOlympiadGameId() == _owner.getOlympiadGameId() && targetPlayer.getOlympiadSide() != _owner.getOlympiadSide()) { _target = (L2Character) ownerTarget; }
                    }
                }
                return;
            }

            // test owners target if it is valid then use it
            if (ownerTarget instanceof L2Character && ownerTarget != _owner.getPet() && ownerTarget != _owner) {
                // target mob which has aggro on you or your summon
                if (ownerTarget instanceof L2Attackable) {
                    if (((L2Attackable) ownerTarget).getAggroList().get(_owner) != null && !((L2Attackable) ownerTarget).isDead()) {
                        _target = (L2Character) ownerTarget;
                        return;
                    }

                    if (_owner.getPet() != null) {
                        if (((L2Attackable) ownerTarget).getAggroList().get(_owner.getPet()) != null && !((L2Attackable) ownerTarget).isDead()) {
                            _target = (L2Character) ownerTarget;
                            return;
                        }
                    }
                }

                // get target in pvp or in siege
                L2PcInstance enemy = null;

                if ((_owner.getPvpFlag() > 0 && !_owner.isInsideZone(ZoneId.PEACE)) || _owner.isInsideZone(ZoneId.PVP)) {
                    if (!((L2Character) ownerTarget).isDead()) { enemy = ownerTarget.getActingPlayer(); }

                    if (enemy != null) {
                        boolean targetIt = true;

                        if (_owner.getParty() != null) {
                            if (_owner.getParty().getPartyMembers().contains(enemy)) { targetIt = false; }
                            else if (_owner.getParty().getCommandChannel() != null) {
                                if (_owner.getParty().getCommandChannel().getMembers().contains(enemy)) { targetIt = false; }
                            }
                        }
                        if (_owner.getClan() != null && !_owner.isInsideZone(ZoneId.PVP)) {
                            if (_owner.getClan().isMember(enemy.getObjectId())) { targetIt = false; }
                            if (_owner.getAllyId() > 0 && enemy.getAllyId() > 0) {
                                if (_owner.getAllyId() == enemy.getAllyId()) { targetIt = false; }
                            }
                        }
                        if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(ZoneId.PVP)) { targetIt = false; }
                        if (enemy.isInsideZone(ZoneId.PEACE)) { targetIt = false; }
                        if (_owner.getSiegeState() > 0 && _owner.getSiegeState() == enemy.getSiegeState()) { targetIt = false; }
                        if (!enemy.isVisible()) { targetIt = false; }

                        if (targetIt) {
                            _target = enemy;
                            return;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private class Action implements Runnable {
        private final int _chance;

        Action(int chance) {
            _chance = chance;
        }

        @Override
        public void run() {
            try {
                if (_owner.isDead() || !_owner.isOnline()) {
                    stopAction();
                    _owner.delCubic(_id);
                    _owner.broadcastUserInfo();
                    cancelDisappear();
                    return;
                }

                if (!AttackStanceTaskManager.getInstance().isInAttackStance(_owner)) {
                    stopAction();
                    return;
                }

                if (Rnd.get(1, 100) < _chance) {
                    final L2Skill skill = Rnd.get(_skills);
                    if (skill != null) {
                        // friendly skill, so we look a target in owner's party
                        if (skill.getId() == SKILL_CUBIC_HEAL) { cubicTargetForHeal(); }
                        // offensive skill, we look for an enemy target
                        else {
                            getCubicTarget();
                            if (!isInCubicRange(_owner, _target)) { _target = null; }
                        }

                        final L2Character target = _target;
                        if (target != null && !target.isDead()) {
                            _owner.broadcastPacket(new MagicSkillUse(_owner, target, skill.getId(), skill.getLevel(), 0, 0));

                            final L2SkillType type = skill.getSkillType();
                            final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
                            final L2Character[] targets =
                                    {
                                            target
                                    };

                            if (type == L2SkillType.PARALYZE || type == L2SkillType.STUN || type == L2SkillType.ROOT || type == L2SkillType.AGGDAMAGE) { useCubicDisabler(type, L2CubicInstance.this, skill, targets); }
                            else if (type == L2SkillType.MDAM) { useCubicMdam(L2CubicInstance.this, skill, targets); }
                            else if (type == L2SkillType.POISON || type == L2SkillType.DEBUFF || type == L2SkillType.DOT) { useCubicContinuous(L2CubicInstance.this, skill, targets); }
                            else if (type == L2SkillType.DRAIN) { ((L2SkillDrain) skill).useCubicSkill(L2CubicInstance.this, targets); }
                            else { handler.useSkill(_owner, skill, targets); }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    public void useCubicContinuous(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets) {
        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            final L2Character target = ((L2Character) obj);
            if (target.isDead()) { continue; }

            if (skill.isOffensive()) {
                final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
                final boolean bss = activeCubic.getOwner().isChargedShot(ShotType.BLESSED_SPIRITSHOT);
                final boolean acted = Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld, bss);

                if (!acted) {
                    activeCubic.getOwner().sendPacket(SystemMessageId.ATTACK_FAILED);
                    continue;
                }
            }

            // If this is a debuff, let the duel manager know about it so the debuff can be removed after the duel (player & target must be in the same duel)
            if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId()) {
                for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target)) {
                    if (debuff != null) { DuelManager.getInstance().onBuff(((L2PcInstance) target), debuff); }
                }
            }
            else { skill.getEffects(activeCubic, target, null); }
        }
    }

    public void useCubicMdam(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets) {
        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            final L2Character target = ((L2Character) obj);
            if (target.isAlikeDead()) {
                if (target instanceof L2PcInstance) { target.stopFakeDeath(true); }
                else { continue; }
            }

            final boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, skill));
            final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
            final boolean bss = activeCubic.getOwner().isChargedShot(ShotType.BLESSED_SPIRITSHOT);

            int damage = (int) Formulas.calcMagicDam(activeCubic, target, skill, mcrit, shld);

            // If target is reflecting the skill then no damage is done Ignoring vengance-like reflections
            if ((Formulas.calcSkillReflect(target, skill) & Formulas.SKILL_REFLECT_SUCCEED) > 0) { damage = 0; }

            if (damage > 0) {
                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, damage);

                activeCubic.getOwner().sendDamageMessage(target, damage, mcrit, false, false);

                if (skill.hasEffects()) {
                    // activate attacked effects, if any
                    target.stopSkillEffects(skill.getId());

                    if (target.getFirstEffect(skill) != null) { target.removeEffect(target.getFirstEffect(skill)); }

                    if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld, bss)) { skill.getEffects(activeCubic, target, null); }
                }

                target.reduceCurrentHp(damage, activeCubic.getOwner(), skill);
            }
        }
    }

    public void useCubicDisabler(L2SkillType type, L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets) {
        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            final L2Character target = ((L2Character) obj);
            if (target.isDead()) { continue; }

            final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
            final boolean bss = activeCubic.getOwner().isChargedShot(ShotType.BLESSED_SPIRITSHOT);

            switch (type) {
                case STUN:
                case PARALYZE:
                case ROOT:
                    if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld, bss)) {
                        // If this is a debuff, let the duel manager know about it so the debuff can be removed after the duel (player & target must be in the same duel)
                        if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId()) {
                            for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target)) {
                                if (debuff != null) { DuelManager.getInstance().onBuff(((L2PcInstance) target), debuff); }
                            }
                        }
                        else { skill.getEffects(activeCubic, target, null); }
                    }
                    break;

                case CANCEL_DEBUFF:
                    final L2Effect[] effects = target.getAllEffects();
                    if (effects == null || effects.length == 0) { break; }

                    int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
                    for (L2Effect e : effects) {
                        if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects()) {
                            // Do not remove raid curse skills
                            if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082) {
                                e.exit();
                                if (count > -1) { count++; }
                            }
                        }
                    }
                    break;

                case AGGDAMAGE:
                    if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld, bss)) {
                        if (target instanceof L2Attackable) { target.getAI().notifyEvent(ECtrlEvent.EVT_AGGRESSION, activeCubic.getOwner(), (int) ((150 * skill.getPower()) / (target.getLevel() + 7))); }

                        skill.getEffects(activeCubic, target, null);
                    }
                    break;
            }
        }
    }

    /**
     * @param owner
     * @param target
     * @return true if the target is inside of the owner's max Cubic range
     */
    public boolean isInCubicRange(L2Character owner, L2Character target) {
        if (owner == null || target == null) { return false; }

        int x, y, z;
        int range = MAX_MAGIC_RANGE;

        x = (owner.getX() - target.getX());
        y = (owner.getY() - target.getY());
        z = (owner.getZ() - target.getZ());

        return ((x * x) + (y * y) + (z * z) <= (range * range));
    }

    /** this sets the friendly target for a cubic */
    public void cubicTargetForHeal() {
        L2Character target = null;
        double percentleft = 100.0;
        L2Party party = _owner.getParty();

        // if owner is in a duel but not in a party duel, then it is the same as he does not have a party
        if (_owner.isInDuel()) { if (!DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel()) { party = null; } }

        if (party != null && !_owner.isInOlympiadMode()) {
            // Get all Party Members in a spheric area near the L2Character
            for (L2Character partyMember : party.getPartyMembers()) {
                if (!partyMember.isDead()) {
                    // if party member not dead, check if he is in castrange of heal cubic
                    if (isInCubicRange(_owner, partyMember)) {
                        // member is in cubic casting range, check if he need heal and if he have the lowest HP
                        if (partyMember.getCurrentHp() < partyMember.getMaxHp()) {
                            if (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp())) {
                                percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
                                target = partyMember;
                            }
                        }
                    }
                }

                if (partyMember.getPet() != null) {
                    if (partyMember.getPet().isDead()) { continue; }

                    // if party member's pet not dead, check if it is in castrange of heal cubic
                    if (!isInCubicRange(_owner, partyMember.getPet())) { continue; }

                    // member's pet is in cubic casting range, check if he need heal and if he have
                    // the lowest HP
                    if (partyMember.getPet().getCurrentHp() < partyMember.getPet().getMaxHp()) {
                        if (percentleft > (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp())) {
                            percentleft = (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp());
                            target = partyMember.getPet();
                        }
                    }
                }
            }
        }
        else {
            if (_owner.getCurrentHp() < _owner.getMaxHp()) {
                percentleft = (_owner.getCurrentHp() / _owner.getMaxHp());
                target = _owner;
            }

            if (_owner.getPet() != null && !_owner.getPet().isDead() && _owner.getPet().getCurrentHp() < _owner.getPet().getMaxHp() && percentleft > (_owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp()) && isInCubicRange(_owner, _owner.getPet())) {
                target = _owner.getPet();
            }
        }

        _target = target;
    }

    public boolean givenByOther() {
        return _givenByOther;
    }

    private class Heal implements Runnable {
        Heal() {
        }

        @Override
        public void run() {
            if (_owner.isDead() || !_owner.isOnline()) {
                stopAction();
                _owner.delCubic(_id);
                _owner.broadcastUserInfo();
                cancelDisappear();
                return;
            }

            try {
                L2Skill skill = null;
                for (L2Skill sk : _skills) {
                    if (sk.getId() == SKILL_CUBIC_HEAL) {
                        skill = sk;
                        break;
                    }
                }

                if (skill != null) {
                    cubicTargetForHeal();
                    L2Character target = _target;
                    if (target != null && !target.isDead()) {
                        if (target.getMaxHp() - target.getCurrentHp() > skill.getPower()) {
                            final L2Character[] targets =
                                    {
                                            target
                                    };

                            final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
                            if (handler != null) { handler.useSkill(_owner, skill, targets); }
                            else { skill.useSkill(_owner, targets); }

                            MagicSkillUse msu = new MagicSkillUse(_owner, target, skill.getId(), skill.getLevel(), 0, 0);
                            _owner.broadcastPacket(msu);
                        }
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    private class Disappear implements Runnable {
        Disappear() {
        }

        @Override
        public void run() {
            stopAction();
            _owner.delCubic(_id);
            _owner.broadcastUserInfo();
        }
    }
}