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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class ScrollOfResurrection implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance activeChar = (L2PcInstance) playable;
        if (activeChar.isSitting()) {
            activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
            return;
        }

        if (activeChar.isMovementDisabled()) { return; }

        L2Character target = (L2Character) activeChar.getTarget();

        // Target must be a dead L2PetInstance or L2PcInstance.
        if ((!(target instanceof L2PetInstance) && !(target instanceof L2PcInstance)) || !target.isDead()) {
            activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        // Pet scrolls to ress a player.
        if (item.getItemId() == 6387 && target instanceof L2PcInstance) {
            activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        // Pickup player, or pet owner in case target is a pet.
        L2PcInstance targetPlayer = target.getActingPlayer();

        // Check if target isn't in a active siege zone.
        Castle castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
        if (castle != null && castle.getSiege().isInProgress()) {
            activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
            return;
        }

        // Check if the target is in a festival.
        if (targetPlayer.isFestivalParticipant()) { return; }

        if (targetPlayer.isReviveRequested()) {
            if (targetPlayer.isRevivingPet()) {
                activeChar.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
            }
            else {
                activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
            }

            return;
        }

        IntIntHolder[] skills = item.getEtcItem().getSkills();
        if (skills == null) {
            LOGGER.info("{} does not have registered any skill for handler.", item.getName());
            return;
        }

        for (IntIntHolder skillInfo : skills) {
            if (skillInfo == null) { continue; }

            L2Skill itemSkill = skillInfo.getSkill();
            if (itemSkill == null) { continue; }

            // Scroll consumption is made on skill call, not on item call.
            playable.useMagic(itemSkill, false, false);
        }
    }
}