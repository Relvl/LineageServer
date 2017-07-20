package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.AggroInfo;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.client.game_to_client.SetSummonRemainTime;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

import java.util.concurrent.Future;

public class L2SummonInstance extends L2Summon {
    private float _expPenalty;
    private int _itemConsumeId;
    private int _itemConsumeCount;
    private int _itemConsumeSteps;
    private int _totalLifeTime = 1200000;
    private int _timeLostIdle = 1000;
    private int _timeLostActive = 1000;
    private int _timeRemaining;
    private int _nextItemConsumeTime;

    public int lastShowntimeRemaining;

    private Future<?> _summonLifeTask;

    public L2SummonInstance(int objectId, NpcTemplate template, L2PcInstance owner, L2Skill skill) {
        super(objectId, template, owner);
        setShowSummonAnimation(true);

        if (skill != null) {
            L2SkillSummon summonSkill = (L2SkillSummon) skill;
            _itemConsumeId = summonSkill.getItemConsumeIdOT();
            _itemConsumeCount = summonSkill.getItemConsumeOT();
            _itemConsumeSteps = summonSkill.getItemConsumeSteps();
            _totalLifeTime = summonSkill.getTotalLifeTime();
            _timeLostIdle = summonSkill.getTimeLostIdle();
            _timeLostActive = summonSkill.getTimeLostActive();
        }
        _timeRemaining = _totalLifeTime;
        lastShowntimeRemaining = _totalLifeTime;

        if (_itemConsumeId == 0 || _itemConsumeSteps == 0) {
            _nextItemConsumeTime = -1; // do not consume
        }
        else { _nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1); }

        _summonLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonLifetime(getOwner(), this), 1000, 1000);
    }

    @Override
    public final int getLevel() {
        return getTemplate() != null ? getTemplate().getLevel() : 0;
    }

    @Override
    public int getSummonType() {
        return 1;
    }

    public void setExpPenalty(float expPenalty) {
        _expPenalty = expPenalty;
    }

    public float getExpPenalty() {
        return _expPenalty;
    }

    public int getItemConsumeCount() {
        return _itemConsumeCount;
    }

    public int getItemConsumeId() {
        return _itemConsumeId;
    }

    public int getItemConsumeSteps() {
        return _itemConsumeSteps;
    }

    public int getNextItemConsumeTime() {
        return _nextItemConsumeTime;
    }

    public int getTotalLifeTime() {
        return _totalLifeTime;
    }

    public int getTimeLostIdle() {
        return _timeLostIdle;
    }

    public int getTimeLostActive() {
        return _timeLostActive;
    }

    public int getTimeRemaining() {
        return _timeRemaining;
    }

    public void setNextItemConsumeTime(int value) {
        _nextItemConsumeTime = value;
    }

    public void decNextItemConsumeTime(int value) {
        _nextItemConsumeTime -= value;
    }

    public void decTimeRemaining(int value) {
        _timeRemaining -= value;
    }

    public void addExpAndSp(int addToExp, int addToSp) {
        getOwner().addExpAndSp(addToExp, addToSp);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }

        // Send aggro of mobs to summoner.
        for (L2Attackable mob : getKnownList().getKnownType(L2Attackable.class)) {
            if (mob.isDead()) { continue; }

            AggroInfo info = mob.getAggroList().get(this);
            if (info != null) { mob.addDamageHate(getOwner(), info.getDamage(), info.getHate()); }
        }

        // Popup for summon if phoenix buff was on
        if (isPhoenixBlessed()) { getOwner().reviveRequest(getOwner(), null, true); }

        DecayTaskManager.getInstance().add(this, getTemplate().getCorpseTime());

        if (_summonLifeTask != null) {
            _summonLifeTask.cancel(false);
            _summonLifeTask = null;
        }
        return true;

    }

    static class SummonLifetime implements Runnable {
        private final L2PcInstance _activeChar;
        private final L2SummonInstance _summon;

        SummonLifetime(L2PcInstance activeChar, L2SummonInstance newpet) {
            _activeChar = activeChar;
            _summon = newpet;
        }

        @Override
        public void run() {
            try {
                double oldTimeRemaining = _summon.getTimeRemaining();
                int maxTime = _summon.getTotalLifeTime();

                // if pet is attacking
                if (_summon.isAttackingNow()) { _summon.decTimeRemaining(_summon.getTimeLostActive()); }
                else { _summon.decTimeRemaining(_summon.getTimeLostIdle()); }

                double newTimeRemaining = _summon.getTimeRemaining();

                // check if the summon's lifetime has ran out
                if (newTimeRemaining < 0) { _summon.unSummon(_activeChar); }
                else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime())) {
                    _summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));

                    // check if owner has enought itemConsume, if requested
                    if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId(EItemProcessPurpose.CONSUME, _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true)) {
                        _summon.unSummon(_activeChar);
                    }
                }

                // prevent useless packet-sending when the difference isn't visible.
                if ((_summon.lastShowntimeRemaining - newTimeRemaining) > maxTime / 352) {
                    _activeChar.sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
                    _summon.lastShowntimeRemaining = (int) newTimeRemaining;
                    _summon.updateEffectIcons();
                }
            }
            catch (Exception e) {
                LOGGER.error("Error on player [{}] summon item consume task.", _activeChar.getName(), e);
            }
        }
    }

    @Override
    public void unSummon(L2PcInstance owner) {
        if (_summonLifeTask != null) {
            _summonLifeTask.cancel(false);
            _summonLifeTask = null;
        }
        super.unSummon(owner);
    }

    @Override
    public boolean destroyItem(EItemProcessPurpose process, int objectId, int count, L2Object reference, boolean sendMessage) {
        return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
    }

    @Override
    public boolean destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2Object reference, boolean sendMessage) {
        return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
    }

    @Override
    public void doPickupItem(L2Object object) {
    }
}