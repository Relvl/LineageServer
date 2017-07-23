/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
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

    public PcStat(L2PcInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addExp(long value) {
        // Allowed to gain exp?
        if (!getActiveChar().getAccessLevel().canGainExp()) { return false; }

        if (!super.addExp(value)) { return false; }

        getActiveChar().sendPacket(new UserInfo(getActiveChar()));
        return true;
    }

    /**
     * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.
     * <ul>
     * <li>Remove Karma when the player kills L2MonsterInstance</li>
     * <li>Send StatusUpdate to the L2PcInstance</li>
     * <li>Send a Server->Client System Message to the L2PcInstance</li>
     * <li>If the L2PcInstance increases its level, send SocialAction (broadcast)</li>
     * <li>If the L2PcInstance increases its level, manage the increase level task (Max MP, Max MP, Recommandation, Expertise and beginner skills...)</li>
     * <li>If the L2PcInstance increases its level, send UserInfo to the L2PcInstance</li>
     * </ul>
     *
     * @param addToExp The Experience value to add
     * @param addToSp  The SP value to add
     */
    @Override
    public boolean addExpAndSp(long addToExp, int addToSp) {
        // GM check concerning canGainExp().
        if (!getActiveChar().getAccessLevel().canGainExp()) { return false; }

        // If this player has a pet, give the xp to the pet now (if any).
        if (getActiveChar().hasPet()) {
            L2PetInstance pet = (L2PetInstance) getActiveChar().getPet();
            if (pet.getStat().getExp() <= (PetDataTable.getInstance().getPetLevelData(pet.getNpcId(), 81).getPetMaxExp() + 10000)) {
                if (Util.checkIfInShortRadius(Config.ALT_PARTY_RANGE, pet, getActiveChar(), true)) {
                    float ratioTakenByPet = pet.getPetLevelData().getOwnerExpTaken();

                    if (ratioTakenByPet > 0 && !pet.isDead()) { pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet)); }

                    // now adjust the max ratio to avoid the owner earning negative exp/sp
                    if (ratioTakenByPet > 1) { ratioTakenByPet = 1; }

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
    public boolean removeExpAndSp(long removeExp, int removeSp) {
        return removeExpAndSp(removeExp, removeSp, true);
    }

    public boolean removeExpAndSp(long removeExp, int removeSp, boolean sendMessage) {
        int oldLevel = getLevel();

        if (!super.removeExpAndSp(removeExp, removeSp)) { return false; }

        // Send messages.
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

        // Update the overloaded status of the L2PcInstance
        getActiveChar().refreshOverloaded();
        // Update the expertise status of the L2PcInstance
        getActiveChar().refreshExpertisePenalty();
        // Send UserInfo to the L2PcInstance
        getActiveChar().sendPacket(new UserInfo(getActiveChar()));

        return levelIncreased;
    }

    @Override
    public final long getExpForLevel(int level) {
        return Experience.LEVEL[level];
    }

    @Override
    public final L2PcInstance getActiveChar() {
        return (L2PcInstance) super.getActiveChar();
    }

    @Override
    public final long getExp() {
        if (getActiveChar().isSubClassActive()) { return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp(); }

        return super.getExp();
    }

    @Override
    public final void setExp(long value) {
        if (getActiveChar().isSubClassActive()) { getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value); }
        else { super.setExp(value); }
    }

    @Override
    public final byte getLevel() {
        if (getActiveChar().isSubClassActive()) { return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel(); }

        return super.getLevel();
    }

    @Override
    public final void setLevel(byte value) {
        if (value > Experience.MAX_LEVEL - 1) { value = Experience.MAX_LEVEL - 1; }

        if (getActiveChar().isSubClassActive()) { getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value); }
        else { super.setLevel(value); }
    }

    @Override
    public final int getMaxCp() {
        // Get the Max CP (base+modifier) of the L2PcInstance
        int val = (int) calcStat(Stats.MAX_CP, getActiveChar().getTemplate().getBaseCpMax(getActiveChar().getLevel()), null, null);
        if (val != _oldMaxCp) {
            _oldMaxCp = val;

            // Launch a regen task if the new Max CP is higher than the old one
            if (getActiveChar().getStatus().getCurrentCp() != val) {
                getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp()); // trigger start of regeneration
            }
        }
        return val;
    }

    @Override
    public final int getMaxHp() {
        // Get the Max HP (base+modifier) of the L2PcInstance
        int val = super.getMaxHp();
        if (val != _oldMaxHp) {
            _oldMaxHp = val;

            // Launch a regen task if the new Max HP is higher than the old one
            if (getActiveChar().getStatus().getCurrentHp() != val) {
                getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
            }
        }

        return val;
    }

    @Override
    public final int getMaxMp() {
        // Get the Max MP (base+modifier) of the L2PcInstance
        int val = super.getMaxMp();

        if (val != _oldMaxMp) {
            _oldMaxMp = val;

            // Launch a regen task if the new Max MP is higher than the old one
            if (getActiveChar().getStatus().getCurrentMp() != val) {
                getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp()); // trigger start of regeneration
            }
        }

        return val;
    }

    @Override
    public final int getSp() {
        if (getActiveChar().isSubClassActive()) { return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp(); }

        return super.getSp();
    }

    @Override
    public final void setSp(int value) {
        if (getActiveChar().isSubClassActive()) { getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value); }
        else { super.setSp(value); }

        StatusUpdate su = new StatusUpdate(getActiveChar());
        su.addAttribute(StatusUpdate.SP, getSp());
        getActiveChar().sendPacket(su);
    }

    @Override
    public int getRunSpeed() {
        int val;

        if (getActiveChar().isMounted()) {
            int baseRunSpd = NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).getBaseRunSpd();
            val = (int) calcStat(Stats.RUN_SPEED, baseRunSpd, null, null);
        }
        else { val = super.getRunSpeed(); }

        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) { val *= Math.pow(0.84, penalty); }

        return val;
    }

    @Override
    public int getMAtkSpd() {
        int val = super.getMAtkSpd();

        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) { val *= Math.pow(0.84, penalty); }

        return val;
    }

    @Override
    public int getEvasionRate(L2Character target) {
        int val = super.getEvasionRate(target);

        int penalty = getActiveChar().getExpertiseArmorPenalty();
        if (penalty > 0) { val -= 2 * penalty; }

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
        if (getActiveChar().isMounted()) { return getRunSpeed() * 1f / NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).getBaseRunSpd(); }

        return super.getMovementSpeedMultiplier();
    }

    @Override
    public int getPhysicalAttackRange() {
        return (int) calcStat(Stats.POWER_ATTACK_RANGE, getActiveChar().getAttackType().getAttackRange(), null, null);
    }

    @Override
    public int getWalkSpeed() {
        return (getRunSpeed() * 70) / 100;
    }
}