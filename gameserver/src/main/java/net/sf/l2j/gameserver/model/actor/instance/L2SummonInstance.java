package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.AggroInfo;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.concurrent.Future;

@SuppressWarnings("ClassHasNoToStringMethod")
public class L2SummonInstance extends L2Summon {
    private float expPenalty;
    private int itemConsumeId;
    private int itemConsumeCount;
    private int itemConsumeSteps;
    private int totalLifeTime = 1200000;
    private int timeLostIdle = 1000;
    private int timeLostActive = 1000;
    private int timeRemaining;
    private int nextItemConsumeTime;
    private int lastShowntimeRemaining;

    private Future<?> summonLifeTask;

    public L2SummonInstance(int objectId, NpcTemplate template, L2PcInstance owner, L2Skill skill) {
        super(objectId, template, owner);
        setShowSummonAnimation(true);

        if (skill != null) {
            L2SkillSummon summonSkill = (L2SkillSummon) skill;
            itemConsumeId = summonSkill.getItemConsumeIdOT();
            itemConsumeCount = summonSkill.getItemConsumeOT();
            itemConsumeSteps = summonSkill.getItemConsumeSteps();
            totalLifeTime = summonSkill.getTotalLifeTime();
            timeLostIdle = summonSkill.getTimeLostIdle();
            timeLostActive = summonSkill.getTimeLostActive();
        }
        timeRemaining = totalLifeTime;
        this.lastShowntimeRemaining = totalLifeTime;

        if (itemConsumeId == 0 || itemConsumeSteps == 0) {
            nextItemConsumeTime = -1; // do not consume
        }
        else {
            nextItemConsumeTime = totalLifeTime - totalLifeTime / (itemConsumeSteps + 1);
        }

        summonLifeTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new SummonLifetimeTask(getOwner(), this), 1000, 1000);
    }

    @Override
    public final int getLevel() { return getTemplate() != null ? getTemplate().getLevel() : 0; }

    @Override
    public int getSummonType() { return 1; }

    public void setExpPenalty(float expPenalty) { this.expPenalty = expPenalty; }

    public float getExpPenalty() { return expPenalty; }

    public int getItemConsumeCount() { return itemConsumeCount; }

    public int getItemConsumeId() { return itemConsumeId; }

    public int getItemConsumeSteps() { return itemConsumeSteps; }

    public int getNextItemConsumeTime() { return nextItemConsumeTime; }

    public int getTotalLifeTime() { return totalLifeTime; }

    public int getTimeLostIdle() { return timeLostIdle; }

    public int getTimeLostActive() { return timeLostActive; }

    public int getTimeRemaining() { return timeRemaining; }

    public void decNextItemConsumeTime(int value) { nextItemConsumeTime -= value; }

    public void decTimeRemaining(int value) { timeRemaining -= value; }

    public void addExpAndSp(int addToExp, int addToSp) { getOwner().addExpAndSp(addToExp, addToSp); }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }

        for (L2Attackable mob : getKnownList().getKnownType(L2Attackable.class)) {
            if (mob.isDead()) { continue; }
            AggroInfo info = mob.getAggroList().get(this);
            if (info != null) {
                mob.addDamageHate(getOwner(), info.getDamage(), info.getHate());
            }
        }
        if (isPhoenixBlessed()) {
            getOwner().reviveRequest(getOwner(), null, true);
        }
        DecayTaskManager.getInstance().add(this, getTemplate().getCorpseTime());
        if (summonLifeTask != null) {
            summonLifeTask.cancel(false);
            summonLifeTask = null;
        }
        return true;
    }

    @Override
    public void unSummon(L2PcInstance owner) {
        if (summonLifeTask != null) {
            summonLifeTask.cancel(false);
            summonLifeTask = null;
        }
        super.unSummon(owner);
    }

    @Override
    public void doPickupItem(L2Object object) {}

    public int getLastShowntimeRemaining() { return lastShowntimeRemaining; }

    public void setLastShowntimeRemaining(int time) { this.lastShowntimeRemaining = time; }
}