package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.PetDataTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStat extends PlayableStat {
    private int _oldMaxHp; // stats watch
    private int _oldMaxMp; // stats watch
    private int _oldMaxCp; // stats watch

    public PcStat(L2PcInstance activeChar) { super(activeChar); }

    @Override
    public boolean addExp(long value) {
        if (!getActiveChar().getAccessLevel().canGainExp()) { return false; }
        if (!super.addExp(value)) { return false; }
        getActiveChar().sendPacket(new UserInfo(getActiveChar()));
        return true;
    }

    @Override
    public boolean addExpAndSp(long addToExp, int addToSp) {
        if (!getActiveChar().getAccessLevel().canGainExp()) { return false; }
        if (getActiveChar().hasPet()) {
            L2PetInstance pet = (L2PetInstance) getActiveChar().getPet();
            if (pet.getStat().getExp() <= (PetDataTable.getInstance().getPetLevelData(pet.getNpcId(), 81).getPetMaxExp() + 10000)) {
                if (Util.checkIfInShortRadius(Config.ALT_PARTY_RANGE, pet, getActiveChar(), true)) {
                    float ratioTakenByPet = pet.getPetLevelData().getOwnerExpTaken();
                    if (ratioTakenByPet > 0 && !pet.isDead()) {
                        pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
                    }
                    if (ratioTakenByPet > 1) {
                        ratioTakenByPet = 1;
                    }
                    addToExp = (long) (addToExp * (1 - ratioTakenByPet));
                    addToSp = (int) (addToSp * (1 - ratioTakenByPet));
                }
            }
        }

        if (!super.addExpAndSp(addToExp, addToSp)) { return false; }

        SystemMessage sm;

        if (addToExp == 0 && addToSp > 0) { sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP).addNumber(addToSp); }
        else if (addToExp > 0 && addToSp == 0) { sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_EXPERIENCE).addNumber((int) addToExp); }
        else { sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP).addNumber((int) addToExp).addNumber(addToSp); }
        getActiveChar().sendPacket(sm);

        return true;
    }

    @Override
    public boolean removeExpAndSp(long removeExp, int removeSp) { return removeExpAndSp(removeExp, removeSp, true); }

    public boolean removeExpAndSp(long removeExp, int removeSp, boolean sendMessage) {
        int oldLevel = getLevel();
        if (!super.removeExpAndSp(removeExp, removeSp)) { return false; }
        if (sendMessage) {
            if (removeExp > 0) { getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EXP_DECREASED_BY_S1).addNumber((int) removeExp)); }
            if (removeSp > 0) { getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(removeSp)); }
            if (getLevel() < oldLevel) { getActiveChar().broadcastStatusUpdate(); }
        }
        return true;
    }

    @Override
    public final boolean addLevel(byte value) {
        if (getLevel() + value > Experience.MAX_LEVEL - 1) { return false; }
        boolean levelIncreased = super.addLevel(value);

        if (levelIncreased) {
            QuestState qs = getActiveChar().getQuestState("Tutorial");
            if (qs != null) { qs.getQuest().notifyEvent("CE40", null, getActiveChar()); }
            getActiveChar().setCurrentCp(getMaxCp());
            getActiveChar().broadcastPacket(new SocialAction(getActiveChar(), 15));
            getActiveChar().sendPacket(SystemMessageId.YOU_INCREASED_YOUR_LEVEL);
        }

        getActiveChar().rewardSkills(); // Give Expertise skill of this level
        if (getActiveChar().getClan() != null) {
            getActiveChar().getClan().updateClanMember(getActiveChar());
            getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
        }

        if (getActiveChar().isInParty()) {
            getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level
        }

        StatusUpdate su = new StatusUpdate(getActiveChar());
        su.addAttribute(StatusUpdate.LEVEL, getLevel());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
        su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
        getActiveChar().sendPacket(su);

        getActiveChar().refreshOverloaded();
        getActiveChar().refreshExpertisePenalty();
        getActiveChar().sendPacket(new UserInfo(getActiveChar()));

        return levelIncreased;
    }

    @Override
    public final long getExpForLevel(int level) { return Experience.LEVEL[level]; }

    @Override
    public final L2PcInstance getActiveChar() { return super.getActiveChar().getActingPlayer(); }

    @Override
    public final int getMaxCp() {
        // Get the Max CP (base+modifier) of the L2PcInstance
        int val = (int) calcStat(Stats.MAX_CP, getActiveChar().getTemplate().getBaseCpMax(getActiveChar().getLevel()), null, null);
        if (val != _oldMaxCp) {
            _oldMaxCp = val;
            if (getActiveChar().getStatus().getCurrentCp() != val) {
                getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp()); // trigger start of regeneration
            }
        }
        return val;
    }

    @Override
    public final int getMaxHp() {
        int val = super.getMaxHp();
        if (val != _oldMaxHp) {
            _oldMaxHp = val;
            if (getActiveChar().getStatus().getCurrentHp() != val) {
                getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
            }
        }
        return val;
    }

    @Override
    public final int getMaxMp() {
        int val = super.getMaxMp();
        if (val != _oldMaxMp) {
            _oldMaxMp = val;
            if (getActiveChar().getStatus().getCurrentMp() != val) {
                getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp()); // trigger start of regeneration
            }
        }
        return val;
    }

    @Override
    public int getRunSpeed() {
        int val;
        if (getActiveChar().isMounted()) {
            int baseRunSpd = NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).getBaseRunSpd();
            val = (int) calcStat(Stats.RUN_SPEED, baseRunSpd, null, null);
        }
        else {
            val = super.getRunSpeed();
        }
        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) {
            val *= Math.pow(0.84, penalty);
        }
        return val;
    }

    @Override
    public int getMAtkSpd() {
        int val = super.getMAtkSpd();
        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) {
            val *= Math.pow(0.84, penalty);
        }
        return val;
    }

    @Override
    public int getEvasionRate(L2Character target) {
        int val = super.getEvasionRate(target);
        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) {
            val -= 2 * penalty;
        }
        return val;
    }

    @Override
    public int getAccuracy() {
        int val = super.getAccuracy();
        if (getActiveChar().getExpertiseWeaponPenalty()) { val -= 20; }
        return val;
    }

    @Override
    public float getMovementSpeedMultiplier() {
        if (getActiveChar().isMounted()) {
            return getRunSpeed() * 1f / NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).getBaseRunSpd();
        }
        return super.getMovementSpeedMultiplier();
    }

    @Override
    public int getPhysicalAttackRange() { return (int) calcStat(Stats.POWER_ATTACK_RANGE, getActiveChar().getAttackType().getAttackRange(), null, null); }

    @Override
    public int getWalkSpeed() { return (getRunSpeed() * 70) / 100; }

    @Override
    public final long getExp() {
        if (getActiveChar().isSubClassActive()) {
            return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp();
        }
        return super.getExp();
    }

    @Override
    public final void setExp(long value) {
        if (getActiveChar().isSubClassActive()) {
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value);
        }
        else {
            super.setExp(value);
        }
    }

    @Override
    public final byte getLevel() {
        if (getActiveChar().isSubClassActive()) {
            return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel();
        }
        return super.getLevel();
    }

    @Override
    public final void setLevel(byte value) {
        if (value > Experience.MAX_LEVEL - 1) {
            value = Experience.MAX_LEVEL - 1;
        }
        if (getActiveChar().isSubClassActive()) {
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value);
        }
        else {
            super.setLevel(value);
        }
    }

    @Override
    public final int getSp() {
        if (getActiveChar().isSubClassActive()) {
            return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp();
        }
        return super.getSp();
    }

    @Override
    public final void setSp(int value) {
        if (getActiveChar().isSubClassActive()) {
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value);
        }
        else {
            super.setSp(value);
        }
        StatusUpdate su = new StatusUpdate(getActiveChar());
        su.addAttribute(StatusUpdate.SP, getSp());
        getActiveChar().sendPacket(su);
    }
}