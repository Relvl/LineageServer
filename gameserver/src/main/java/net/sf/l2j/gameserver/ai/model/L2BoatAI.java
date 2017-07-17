package net.sf.l2j.gameserver.ai.model;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleDeparture;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleStarted;

public class L2BoatAI extends L2CharacterAI {
    public L2BoatAI(L2BoatInstance boat) {
        super(boat);
    }

    @Override
    protected void moveTo(int x, int y, int z) {
        if (!actor.isMovementDisabled()) {
            if (!actorMoving) { actor.broadcastPacket(new VehicleStarted(getActor(), 1)); }

            actorMoving = true;
            actor.moveToLocation(x, y, z, 0);
            actor.broadcastPacket(new VehicleDeparture(getActor()));
        }
    }

    @Override
    protected void clientStopMoving(HeadedLocation pos) {
        if (actor.isMoving()) { actor.stopMove(pos); }

        if (actorMoving || pos != null) {
            actorMoving = false;
            actor.broadcastPacket(new VehicleStarted(getActor(), 0));
            actor.broadcastPacket(new VehicleInfo(getActor()));
        }
    }

    @Override
    public void describeStateToPlayer(L2PcInstance player) {
        if (actorMoving) {
            player.sendPacket(new VehicleDeparture(getActor()));
        }
    }

    @Override
    public L2BoatInstance getActor() {
        return (L2BoatInstance) actor;
    }

    @Override
    protected void onIntentionAttack(L2Character target) {}

    @Override
    protected void onIntentionCast(L2Skill skill, L2Object target) {}

    @Override
    protected void onIntentionFollow(L2Character target) {}

    @Override
    protected void onIntentionPickUp(L2Object object) {}

    @Override
    protected void onIntentionInteract(L2Object object) {}

    @Override
    protected void onEvtAttacked(L2Character attacker) {}

    @Override
    protected void onEvtAggression(L2Character target, int aggro) {}

    @Override
    protected void onEvtStunned(L2Character attacker) {}

    @Override
    protected void onEvtSleeping(L2Character attacker) {}

    @Override
    protected void onEvtRooted(L2Character attacker) {}

    @Override
    protected void onEvtForgetObject(L2Object object) {}

    @Override
    protected void onEvtCancel() {}

    @Override
    protected void onEvtDead() {}

    @Override
    protected void onEvtFakeDeath() {}

    @Override
    protected void onEvtFinishCasting() {}

    @Override
    protected void clientActionFailed() {}

    @Override
    protected void moveToPawn(L2Object pawn, int offset) {}
}