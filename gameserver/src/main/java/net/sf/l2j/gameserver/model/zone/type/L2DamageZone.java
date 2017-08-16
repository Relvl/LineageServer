package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ACastleZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.concurrent.Future;

public class L2DamageZone extends ACastleZoneType {
    private int hpDps = 100;
    private Future<?> task;

    private int startTask = 10;
    private int reuseTask = 5000;
    private String target = "L2Playable";

    public L2DamageZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("dmgSec")) { hpDps = Integer.parseInt(value); }
        else if (name.equalsIgnoreCase("initialDelay")) { startTask = Integer.parseInt(value); }
        else if (name.equalsIgnoreCase("reuse")) { reuseTask = Integer.parseInt(value); }
        else if (name.equals("targetClass")) { target = value; }
        else { super.setParameter(name, value); }
    }

    @Override
    protected boolean isAffected(L2Character character) {
        try {
            if (!(Class.forName("net.sf.l2j.gameserver.model.actor." + target).isInstance(character))) {
                return false;
            }
        }
        catch (ClassNotFoundException e) {
            LOGGER.error("", e);
        }

        return true;
    }

    @Override
    protected void onEnter(L2Character character) {
        if (task == null && hpDps != 0) {
            // Castle traps are active only during siege, or if they're activated.
            if (getCastle() != null && (!isEnabled() || !getCastle().getSiege().isInProgress())) { return; }

            synchronized (this) {
                if (task == null) {
                    task = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ApplyDamage(this), startTask, reuseTask);

                    // Message for castle traps.
                    if (getCastle() != null) { getCastle().getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.A_TRAP_DEVICE_HAS_BEEN_TRIPPED), false); }
                }
            }
        }

        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.DANGER_AREA, true);
            character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.DANGER_AREA, false);
            if (!character.isInsideZone(ZoneId.DANGER_AREA)) {
                character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
            }
        }
    }

    protected int getHpDps() {
        return hpDps;
    }

    protected void stopTask() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    private static class ApplyDamage implements Runnable {
        private final L2DamageZone damageZone;

        ApplyDamage(L2DamageZone zone) {
            damageZone = zone;
        }

        @Override
        public void run() {
            // Cancels the task if config has changed, if castle isn't in siege anymore, or if zone isn't enabled.
            if (damageZone.getHpDps() <= 0 || (damageZone.getCastle() != null && (!damageZone.isEnabled() || !damageZone.getCastle().getSiege().isInProgress()))) {
                damageZone.stopTask();
                return;
            }

            // Cancels the task if characters list is empty.
            if (damageZone.getCharactersInside().isEmpty()) {
                damageZone.stopTask();
                return;
            }

            // Effect all people inside the zone.
            for (L2Character temp : damageZone.getCharactersInside()) {
                if (temp != null && !temp.isDead()) { temp.reduceCurrentHp(damageZone.getHpDps() * (1 + (temp.calcStat(Stats.DAMAGE_ZONE_VULN, 0, null, null) / 100)), null, null); }
            }
        }
    }
}