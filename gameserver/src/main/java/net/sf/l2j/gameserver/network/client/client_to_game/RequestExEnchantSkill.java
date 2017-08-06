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

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        if (player.getClassId().level() < 3 || player.getLevel() < 76) { return; }

        L2Npc trainer = player.getCurrentFolkNPC();
        if (trainer == null) { return; }

        if (!player.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false) && !player.isGM()) { return; }

        if (player.getSkillLevel(skillId) >= skillLevel) { return; }

        L2Skill skill = SkillTable.getInfo(skillId, skillLevel);
        if (skill == null) { return; }

        L2EnchantSkillData data = null;
        int baseLvl = 0;

        // Try to find enchant skill.
        for (L2EnchantSkillLearn esl : SkillTreeTable.getInstance().getAvailableEnchantSkills(player)) {
            if (esl == null) { continue; }

            if (esl.getId() == skillId && esl.getLevel() == skillLevel) {
                data = SkillTreeTable.getInstance().getEnchantSkillData(esl.getEnchant());
                baseLvl = esl.getBaseLevel();
                break;
            }
        }

        if (data == null) { return; }

        // Check exp and sp neccessary to enchant skill.
        if (player.getStat().getSp() < data.getCostSp()) {
            player.sendPacket(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
            return;
        }
        if (player.getStat().getExp() < data.getCostExp()) {
            player.sendPacket(SystemMessageId.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
            return;
        }

        // Check item restriction, and try to consume item.
        if (Config.ES_SP_BOOK_NEEDED) {
            if (data.getItemId() != 0 && data.getItemCount() != 0) {
                if (player.getInventory().destroyItemByItemId(EItemProcessPurpose.SKILL, data.getItemId(), data.getItemCount(), player, trainer, true) == null) {
                    player.sendPacket(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                    return;
                }
            }
        }

        // All conditions fulfilled, consume exp and sp.
        player.removeExpAndSp(data.getCostExp(), data.getCostSp());

        // Try to enchant skill.
        if (Rnd.get(100) <= data.getRate(player.getLevel())) {
            player.addSkill(skill, true);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1).addSkillName(skillId, skillLevel));
        }
        else {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1).addSkillName(skillId, skillLevel));
            if (skillLevel > 100) {
                skillLevel = baseLvl;
                player.addSkill(SkillTable.getInfo(skillId, skillLevel), true);
            }
        }
        player.sendSkillList();
        player.sendPacket(new UserInfo(player));

        // Update shortcuts.
        for (L2ShortCut sc : player.getAllShortCuts()) {
            if (sc.getId() == skillId && sc.getType() == L2ShortCut.TYPE_SKILL) {
                L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), L2ShortCut.TYPE_SKILL, skillId, skillLevel, 1);
                player.sendPacket(new ShortCutRegister(newsc));
                player.registerShortCut(newsc);
            }
        }

        // Show enchant skill list.
        L2NpcInstance.showEnchantSkillList(player, trainer, player.getClassId());
    }
}