package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.MoveToPawn;

public final class L2ArtefactInstance extends L2NpcInstance {
    public L2ArtefactInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        getCastle().registerArtefact(this);
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (player.getTarget() == this) {
            if (!canInteract(player)) {
                player.getAI().setIntention(EIntention.INTERACT, this);
            }
            else {
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
        else { player.setTarget(this); }
    }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public void onForcedAttack(L2PcInstance player) { player.sendPacket(ActionFailed.STATIC_PACKET); }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill) { }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill) { }

    @Override
    public boolean isHolyArtefact() { return true; }
}
