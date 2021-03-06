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
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Beast SoulShot Handler
 *
 * @author Tempy
 */
public class BeastSoulShot implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (playable == null) { return; }

        L2PcInstance activeOwner = playable.getActingPlayer();
        if (activeOwner == null) { return; }

        if (playable instanceof L2Summon) {
            activeOwner.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
            return;
        }

        L2Summon activePet = activeOwner.getPet();
        if (activePet == null) {
            activeOwner.sendPacket(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
            return;
        }

        if (activePet.isDead()) {
            activeOwner.sendPacket(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET);
            return;
        }

        // SoulShots are already active.
        if (activePet.isChargedShot(ShotType.SOULSHOT)) { return; }

        // If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
        if (activeOwner.getInventory().destroyItem(null, item, activePet.getSoulShotsPerHit(), null, false) == null) {
            if (!activeOwner.disableAutoShot(item.getItemId())) { activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET); }
            return;
        }

        activeOwner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addItemName(item.getItemId()));
        activePet.setChargedShot(ShotType.SOULSHOT, true);
        Broadcast.toSelfAndKnownPlayersInRadiusSq(activeOwner, new MagicSkillUse(activePet, activePet, 2033, 1, 0, 0), 360000);
    }
}