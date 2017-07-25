package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Sow implements ISkillHandler {
    private static final L2SkillType[] SKILL_IDS = {L2SkillType.SOW};

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) {
        if (!(activeChar instanceof L2PcInstance)) { return; }

        L2Object object = targets[0];
        if (!(object instanceof L2MonsterInstance)) { return; }

        L2PcInstance player = (L2PcInstance) activeChar;
        L2MonsterInstance target = (L2MonsterInstance) object;

        if (target.isDead() || target.isSeeded() || target.getSeederId() != activeChar.getObjectId()) { return; }

        int seedId = target.getSeedType();
        if (seedId == 0) { return; }

        if (activeChar.isPlayer() && !activeChar.getActingPlayer().destroyItemByItemId(EItemProcessPurpose.CONSUME, seedId, 1, target, false)) { return; }

        SystemMessage sm;
        if (calcSuccess(activeChar, target, seedId)) {
            player.sendPacket(new PlaySound(QuestState.SOUND_ITEMGET));
            target.setSeeded(activeChar.getObjectId());
            sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
        }
        else { sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN); }

        if (!player.isInParty()) { player.sendPacket(sm); }
        else { player.getParty().broadcastToPartyMembers(sm); }

        target.getAI().setIntention(EIntention.IDLE);
    }

    private static boolean calcSuccess(L2Character activeChar, L2Character target, int seedId) {
        int basicSuccess = L2Manor.getInstance().isAlternative(seedId) ? 20 : 90;
        int minlevelSeed = L2Manor.getInstance().getSeedMinLevel(seedId);
        int maxlevelSeed = L2Manor.getInstance().getSeedMaxLevel(seedId);
        int levelPlayer = activeChar.getLevel(); // Attacker Level
        int levelTarget = target.getLevel(); // target Level

        // Seed level
        if (levelTarget < minlevelSeed) { basicSuccess -= 5 * (minlevelSeed - levelTarget); }
        if (levelTarget > maxlevelSeed) { basicSuccess -= 5 * (levelTarget - maxlevelSeed); }

        // 5% decrease in chance if player level is more than +/- 5 levels to _target's_ level
        int diff = levelPlayer - levelTarget;
        if (diff < 0) { diff = -diff; }
        if (diff > 5) { basicSuccess -= 5 * (diff - 5); }

        // Chance can't be less than 1%
        if (basicSuccess < 1) { basicSuccess = 1; }

        return Rnd.get(99) < basicSuccess;
    }

    @Override
    public L2SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}