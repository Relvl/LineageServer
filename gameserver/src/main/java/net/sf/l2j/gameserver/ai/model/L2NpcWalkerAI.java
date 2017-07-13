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

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.model.L2Position;
import net.sf.l2j.gameserver.model.L2NpcWalkerNode;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcWalkerInstance;

import java.util.List;

public class L2NpcWalkerAI extends L2CharacterAI implements Runnable {
    // The route used by this NPC, consisting of multiple nodes
    private final List<L2NpcWalkerNode> _route;

    // Flag allowing NPC to go to next point or no (allow to delay).
    private boolean _walkingToNextPoint = false;
    private long _nextMoveTime;

    // The currents node and position where the NPC is situated.
    private L2NpcWalkerNode _currentNode;
    private int _currentPos;

    public L2NpcWalkerAI(L2Character character) {
        super(character);

        _route = NpcWalkerRoutesTable.getInstance().getRouteForNpc(getActor().getNpcId());

        if (_route != null) {
            ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
        }
        else {
            LOGGER.warn("{}: Missing route data for NpcID: {}", getClass().getSimpleName(), actor);
        }
    }

    @Override
    public void run() {
        onEvtThink();
    }

    @Override
    protected void onEvtThink() {
        if (_walkingToNextPoint) {
            checkArrived();
            return;
        }

        if (_nextMoveTime < System.currentTimeMillis()) { walkToLocation(); }
    }

    /**
     * If npc can't walk to it's target then just teleport to next point
     *
     * @param blockedAtPos ignoring it
     */
    @Override
    protected void onEvtArrivedBlocked(L2Position blockedAtPos) {
        LOGGER.warn("NpcWalker ID: {}: Blocked at coords: {}, {}, {}. Teleporting to next point.", getActor().getNpcId(), blockedAtPos.posX, blockedAtPos.posY, blockedAtPos.posZ);

        getActor().teleToLocation(_currentNode.getMoveX(), _currentNode.getMoveY(), _currentNode.getMoveZ(), 0);
        super.onEvtArrivedBlocked(blockedAtPos);
    }

    private void checkArrived() {
        if (getActor().isInsideRadius(_currentNode.getMoveX(), _currentNode.getMoveY(), _currentNode.getMoveZ(), 5, false, false)) {
            String chat = _currentNode.getChatText();
            if (chat != null && !chat.isEmpty()) { getActor().broadcastNpcSay(chat); }

            _nextMoveTime = System.currentTimeMillis() + Math.max(0, _currentNode.getDelay() * 1000);
            _walkingToNextPoint = false;
        }
    }

    private void walkToLocation() {
        if (_currentPos < (_route.size() - 1)) { _currentPos++; }
        else { _currentPos = 0; }

        _currentNode = _route.get(_currentPos);

        if (_currentNode.getRunning()) { getActor().setRunning(); }
        else { getActor().setWalking(); }

        _walkingToNextPoint = true;
        setIntention(EIntention.MOVE_TO, new L2Position(_currentNode.getMoveX(), _currentNode.getMoveY(), _currentNode.getMoveZ(), 0));
    }

    @Override
    public L2NpcWalkerInstance getActor() {
        return (L2NpcWalkerInstance) super.getActor();
    }
}