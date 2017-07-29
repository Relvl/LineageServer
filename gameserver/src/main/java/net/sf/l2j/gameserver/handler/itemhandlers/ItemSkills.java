package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ExUseSharedGroupItem;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class ItemSkills implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (playable instanceof L2SummonInstance) { return; }

        boolean isPet = playable instanceof L2PetInstance;
        L2PcInstance activeChar = playable.getActingPlayer();

        if (isPet && !item.isTradable()) {
            activeChar.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
            return;
        }

        IntIntHolder[] skills = item.getEtcItem().getSkills();
        if (skills == null) {
            _log.info(item.getName() + " does not have registered any skill for handler.");
            return;
        }

        for (IntIntHolder skillInfo : skills) {
            if (skillInfo == null) { continue; }
            L2Skill itemSkill = skillInfo.getSkill();
            if (itemSkill == null) { continue; }
            if (!itemSkill.checkCondition(playable, playable.getTarget(), false)) { return; }
            if (playable.isSkillDisabled(itemSkill)) { return; }
            if (!itemSkill.isPotion() && playable.isCastingNow()) { return; }
            if (itemSkill.isPotion() || itemSkill.isSimultaneousCast()) {
                if (!item.isHerb()) {
                    if (playable.getInventory().destroyItem(
                            EItemProcessPurpose.CONSUME,
                            item,
                            (itemSkill.getItemConsumeId() == 0 && itemSkill.getItemConsume() > 0) ? itemSkill.getItemConsume() : 1,
                            null, null, false) == null) {
                        return;
                    }
                }

                playable.doSimultaneousCast(itemSkill);
                if (!isPet && item.getItemType() == EtcItemType.HERB && activeChar.hasServitor()) {
                    activeChar.getPet().doSimultaneousCast(itemSkill);
                }
            }
            else {
                if (playable.getInventory().destroyItem(
                        EItemProcessPurpose.CONSUME,
                        item,
                        (itemSkill.getItemConsumeId() == 0 && itemSkill.getItemConsume() > 0) ? itemSkill.getItemConsume() : 1,
                        null, null, false) == null) {
                    return;
                }

                playable.getAI().setIntention(EIntention.IDLE);
                if (!playable.useMagic(itemSkill, forceUse, false)) { return; }
            }

            if (isPet) {
                activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addSkillName(itemSkill));
            }
            else {
                switch (skillInfo.getId()) {
                    case 2031: // Lesser healing potion
                    case 2032: // Healing potion
                    case 2037: // Greater Healing Potion
                        activeChar.shortBuffStatusUpdate(skillInfo.getId(), skillInfo.getValue(), itemSkill.getBuffDuration() / 1000);
                        break;
                }
            }

            int reuseDelay = itemSkill.getReuseDelay();
            if (item.isEtcItem()) {
                if (item.getEtcItem().getReuseDelay() > reuseDelay) {
                    reuseDelay = item.getEtcItem().getReuseDelay();
                }
                playable.addTimeStamp(itemSkill, reuseDelay);
                if (reuseDelay != 0) {
                    playable.disableSkill(itemSkill, reuseDelay);
                }
                if (!isPet) {
                    int group = item.getEtcItem().getSharedReuseGroup();
                    if (group >= 0) {
                        activeChar.sendPacket(new ExUseSharedGroupItem(item.getItemId(), group, reuseDelay, reuseDelay));
                    }
                }
            }
            else if (reuseDelay > 0) {
                playable.addTimeStamp(itemSkill, reuseDelay);
                playable.disableSkill(itemSkill, reuseDelay);
            }
        }
    }
}