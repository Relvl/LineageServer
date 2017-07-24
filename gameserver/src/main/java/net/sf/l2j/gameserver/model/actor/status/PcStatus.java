package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStatus extends PlayableStatus {
    private double currentCp = 0;

    public PcStatus(L2PcInstance activeChar) {
        super(activeChar);
    }

    @Override
    public final void reduceCp(int value) {
        if (getCurrentCp() > value) { setCurrentCp(getCurrentCp() - value); }
        else { setCurrentCp(0); }
    }

    @Override
    public final void reduceHp(double value, L2Character attacker) {
        reduceHp(value, attacker, true, false, false, false);
    }

    @Override
    public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption) {
        reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
    }

    public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP) {
        if (getActiveChar().isDead()) { return; }

        // invul handling
        if (getActiveChar().isInvul()) {
            // other chars can't damage
            if (attacker != getActiveChar()) { return; }

            // only DOT and HP consumption allowed for damage self
            if (!isDOT && !isHPConsumption) { return; }
        }

        if (!isHPConsumption) {
            getActiveChar().stopEffectsOnDamage(awake);
            getActiveChar().forceStandUp();

            if (!isDOT) {
                if (getActiveChar().isStunned() && Rnd.get(10) == 0) { getActiveChar().stopStunning(true); }
            }
        }

        int fullValue = (int) value;
        int tDmg = 0;

        if (attacker != null && attacker != getActiveChar()) {
            final L2PcInstance attackerPlayer = attacker.getActingPlayer();
            if (attackerPlayer != null) {
                if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage()) { return; }

                if (getActiveChar().isInDuel()) {
                    if (getActiveChar().getDuelState() == DuelState.DEAD) { return; }

                    if (getActiveChar().getDuelState() == DuelState.WINNER) { return; }

                    // cancel duel if player got hit by another player, that is not part of the duel
                    if (attackerPlayer.getDuelId() != getActiveChar().getDuelId()) { getActiveChar().setDuelState(DuelState.INTERRUPTED); }
                }
            }

            // Check and calculate transfered damage
            final L2Summon summon = getActiveChar().getPet();
            if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true)) {
                tDmg = (int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

                // Only transfer dmg up to current HP, it should not be killed
                tDmg = Math.min((int) summon.getCurrentHp() - 1, tDmg);
                if (tDmg > 0) {
                    summon.reduceCurrentHp(tDmg, attacker, null);
                    value -= tDmg;
                    fullValue = (int) value; // reduce the announced value here as player will get a message about summon damage
                }
            }

            if (!ignoreCP && attacker instanceof L2Playable) {
                if (getCurrentCp() >= value) {
                    setCurrentCp(getCurrentCp() - value); // Set Cp to diff of Cp vs value
                    value = 0; // No need to subtract anything from Hp
                }
                else {
                    value -= getCurrentCp(); // Get diff from value vs Cp; will apply diff to Hp
                    setCurrentCp(0, false); // Set Cp to 0
                }
            }

            if (fullValue > 0 && !isDOT) {
                SystemMessage smsg;
                // Send a System Message to the L2PcInstance
                smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG);
                smsg.addCharName(attacker);
                smsg.addNumber(fullValue);
                getActiveChar().sendPacket(smsg);

                if (tDmg > 0) {
                    smsg = SystemMessage.getSystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
                    smsg.addCharName(attacker);
                    smsg.addNumber(tDmg);
                    getActiveChar().sendPacket(smsg);

                    if (attackerPlayer != null) {
                        smsg = SystemMessage.getSystemMessage(SystemMessageId.GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_SERVITOR);
                        smsg.addNumber(fullValue);
                        smsg.addNumber(tDmg);
                        attackerPlayer.sendPacket(smsg);
                    }
                }
            }
        }

        if (value > 0) {
            value = getCurrentHp() - value;
            if (value <= 0) {
                if (getActiveChar().isInDuel()) {
                    getActiveChar().disableAllSkills();
                    stopHpMpRegeneration();

                    if (attacker != null) {
                        attacker.getAI().setIntention(EIntention.ACTIVE);
                        attacker.sendPacket(ActionFailed.STATIC_PACKET);
                    }

                    // let the DuelManager know of his defeat
                    DuelManager.getInstance().onPlayerDefeat(getActiveChar());
                    value = 1;
                }
                else { value = 0; }
            }
            setCurrentHp(value);
        }

        if (getActiveChar().getCurrentHp() < 0.5) {
            getActiveChar().abortAttack();
            getActiveChar().abortCast();

            if (getActiveChar().isInOlympiadMode()) {
                stopHpMpRegeneration();
                getActiveChar().setIsDead(true);

                if (getActiveChar().getPet() != null) { getActiveChar().getPet().getAI().setIntention(EIntention.IDLE, null); }

                return;
            }

            getActiveChar().doDie(attacker);

            QuestState qs = getActiveChar().getQuestState("Tutorial");
            if (qs != null) { qs.getQuest().notifyEvent("CE30", null, getActiveChar()); }
        }
    }

    @Override
    public final void setCurrentHp(double newHp, boolean broadcastPacket) {
        super.setCurrentHp(newHp, broadcastPacket);

        if (getCurrentHp() <= getActiveChar().getStat().getMaxHp() * .3) {
            QuestState qs = getActiveChar().getQuestState("Tutorial");
            if (qs != null) { qs.getQuest().notifyEvent("CE45", null, getActiveChar()); }
        }
    }

    @Override
    public final double getCurrentCp() {
        return currentCp;
    }

    @Override
    public final void setCurrentCp(double newCp) {
        setCurrentCp(newCp, true);
    }

    public final void setCurrentCp(double newCp, boolean broadcastPacket) {
        int maxCp = getActiveChar().getStat().getMaxCp();

        synchronized (this) {
            if (getActiveChar().isDead()) { return; }

            if (newCp < 0) { newCp = 0; }

            if (newCp >= maxCp) {
                // Set the RegenActive flag to false
                currentCp = maxCp;
                _flagsRegenActive &= ~REGEN_FLAG_CP;

                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0) { stopHpMpRegeneration(); }
            }
            else {
                // Set the RegenActive flag to true
                currentCp = newCp;
                _flagsRegenActive |= REGEN_FLAG_CP;

                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }

        if (broadcastPacket) { getActiveChar().broadcastStatusUpdate(); }
    }

    @Override
    protected void doRegeneration() {
        final PcStat pcStat = getActiveChar().getStat();

        // Modify the current CP of the L2Character.
        if (getCurrentCp() < pcStat.getMaxCp()) { setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false); }

        // Modify the current HP of the L2Character.
        if (getCurrentHp() < pcStat.getMaxHp()) { setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false); }

        // Modify the current MP of the L2Character.
        if (getCurrentMp() < pcStat.getMaxMp()) { setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false); }

        // Send the StatusUpdate packet.
        getActiveChar().broadcastStatusUpdate();
    }

    @Override
    public L2PcInstance getActiveChar() {
        return (L2PcInstance) super.getActiveChar();
    }
}