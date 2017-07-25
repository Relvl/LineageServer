package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillData;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ShortCutRegister;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;

public final class RequestExEnchantSkill extends L2GameClientPacket {
    private int skillId;
    private int skillLevel;

    @Override
    protected void readImpl() {
        skillId = readD();
        skillLevel = readD();
    }

    @Override
    protected void runImpl() {
        if (skillId <= 0 || skillLevel <= 0) { return; }

        /* TODO Реализовать проверки:
        public boolean isAllowedToEnchantSkills() {
            if (isSubclassChangeLocked()) { return false; }
            if (AttackStanceTaskManager.getInstance().isInAttackStance(this)) { return false; }
            if (isCastingNow() || isCastingSimultaneouslyNow()) { return false; }
            return !isInBoat();
        } */

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }
        if (activeChar.getClassId().level() < 3 || activeChar.getLevel() < 76) { return; }

        L2Npc trainer = activeChar.getCurrentFolkNPC();
        if (trainer == null) { return; }

        if (!activeChar.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false) && !activeChar.isGM()) { return; }

        if (activeChar.getSkillLevel(skillId) >= skillLevel) { return; }

        L2Skill skill = SkillTable.getInfo(skillId, skillLevel);
        if (skill == null) { return; }

        L2EnchantSkillData data = null;
        int baseLvl = 0;

        // Try to find enchant skill.
        for (L2EnchantSkillLearn esl : SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar)) {
            if (esl == null) { continue; }

            if (esl.getId() == skillId && esl.getLevel() == skillLevel) {
                data = SkillTreeTable.getInstance().getEnchantSkillData(esl.getEnchant());
                baseLvl = esl.getBaseLevel();
                break;
            }
        }

        if (data == null) { return; }

        // Check exp and sp neccessary to enchant skill.
        if (activeChar.getSp() < data.getCostSp()) {
            activeChar.sendPacket(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
            return;
        }
        if (activeChar.getExp() < data.getCostExp()) {
            activeChar.sendPacket(SystemMessageId.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
            return;
        }

        // Check item restriction, and try to consume item.
        if (Config.ES_SP_BOOK_NEEDED) {
            if (data.getItemId() != 0 && data.getItemCount() != 0) {
                if (!activeChar.destroyItemByItemId(EItemProcessPurpose.SKILL, data.getItemId(), data.getItemCount(), trainer, true)) {
                    activeChar.sendPacket(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                    return;
                }
            }
        }

        // All conditions fulfilled, consume exp and sp.
        activeChar.removeExpAndSp(data.getCostExp(), data.getCostSp());

        // Try to enchant skill.
        if (Rnd.get(100) <= data.getRate(activeChar.getLevel())) {
            activeChar.addSkill(skill, true);
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1).addSkillName(skillId, skillLevel));
        }
        else {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1).addSkillName(skillId, skillLevel));
            if (skillLevel > 100) {
                skillLevel = baseLvl;
                activeChar.addSkill(SkillTable.getInfo(skillId, skillLevel), true);
            }
        }
        activeChar.sendSkillList();
        activeChar.sendPacket(new UserInfo(activeChar));

        // Update shortcuts.
        for (L2ShortCut sc : activeChar.getAllShortCuts()) {
            if (sc.getId() == skillId && sc.getType() == L2ShortCut.TYPE_SKILL) {
                L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), L2ShortCut.TYPE_SKILL, skillId, skillLevel, 1);
                activeChar.sendPacket(new ShortCutRegister(newsc));
                activeChar.registerShortCut(newsc);
            }
        }

        // Show enchant skill list.
        L2NpcInstance.showEnchantSkillList(activeChar, trainer, activeChar.getClassId());
    }
}