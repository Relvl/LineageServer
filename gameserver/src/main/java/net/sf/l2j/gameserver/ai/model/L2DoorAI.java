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
package net.sf.l2j.gameserver.ai.model;

import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;

public class L2DoorAI extends L2CharacterAI {
    public L2DoorAI(L2DoorInstance door) {
        super(door);
    }

    @Override
    protected void onIntentionIdle() {
    }

    @Override
    protected void onIntentionActive() {
    }

    @Override
    protected void onIntentionRest() {
    }

    @Override
    protected void onIntentionAttack(L2Character target) {
    }

    @Override
    protected void onIntentionCast(L2Skill skill, L2Object target) {
    }

    @Override
    protected void onIntentionMoveTo(HeadedLocation destination) {
    }

    @Override
    protected void onIntentionFollow(L2Character target) {
    }

    @Override
    protected void onIntentionPickUp(L2Object item) {
    }

    @Override
    protected void onIntentionInteract(L2Object object) {
    }

    @Override
    protected void onEvtThink() {
    }

    @Override
    protected void onEvtAttacked(L2Character attacker) {
        ThreadPoolManager.getInstance().executeTask(new onEventAttackedDoorTask((L2DoorInstance) actor, attacker));
    }

    @Override
    protected void onEvtAggression(L2Character target, int aggro) {
    }

    @Override
    protected void onEvtStunned(L2Character attacker) {
    }

    @Override
    protected void onEvtSleeping(L2Character attacker) {
    }

    @Override
    protected void onEvtRooted(L2Character attacker) {
    }

    @Override
    protected void onEvtReadyToAct() {
    }

    @Override
    protected void onEvtUserCmd(Object arg0, Object arg1) {
    }

    @Override
    protected void onEvtArrived() {
    }

    @Override
    protected void onEvtArrivedBlocked(HeadedLocation blockedAtPos) {
    }

    @Override
    protected void onEvtForgetObject(L2Object object) {
    }

    @Override
    protected void onEvtCancel() {
    }

    @Override
    protected void onEvtDead() {
    }

    private class onEventAttackedDoorTask implements Runnable {
        private final L2DoorInstance _door;
        private final L2Character _attacker;

        public onEventAttackedDoorTask(L2DoorInstance door, L2Character attacker) {
            _door = door;
            _attacker = attacker;
        }

        @Override
        public void run() {
            for (L2SiegeGuardInstance guard : _door.getKnownList().getKnownType(L2SiegeGuardInstance.class)) {
                if (actor.isInsideRadius(guard, guard.getClanRange(), false, true) && Math.abs(_attacker.getZ() - guard.getZ()) < 200) { guard.getAI().notifyEvent(ECtrlEvent.EVT_AGGRESSION, _attacker, 15); }
            }
        }
    }
}