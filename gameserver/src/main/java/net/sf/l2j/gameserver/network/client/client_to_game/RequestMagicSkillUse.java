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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.NextAction;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public final class RequestMagicSkillUse extends L2GameClientPacket {
    protected boolean _ctrlPressed;
    protected boolean _shiftPressed;
    private int _magicId;

    @Override
    protected void readImpl() {
        _magicId = readD(); // Identifier of the used skill
        _ctrlPressed = readD() != 0; // True if it's a ForceAttack : Ctrl pressed
        _shiftPressed = readC() != 0; // True if Shift pressed
    }

    @Override
    protected void runImpl() {
        // Get the current L2PcInstance of the player
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        // Get the level of the used skill
        int level = activeChar.getSkillLevel(_magicId);
        if (level <= 0) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Get the L2Skill template corresponding to the skillID received from the client
        L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);
        if (skill == null) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            _log.warn("No skill found with id {} and level {}.", _magicId, level);
            return;
        }

        // If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
        if (skill.getSkillType() == L2SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // players mounted on pets cannot use any toggle skills
        if (skill.isToggle() && activeChar.isMounted()) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (activeChar.isOutOfControl()) {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (activeChar.isAttackingNow()) {
            activeChar.getAI().setNextAction(new NextAction(ECtrlEvent.EVT_READY_TO_ACT, EIntention.CAST, () -> activeChar.useMagic(skill, _ctrlPressed, _shiftPressed)));
        }
        else { activeChar.useMagic(skill, _ctrlPressed, _shiftPressed); }
    }
}