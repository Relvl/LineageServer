package net.sf.l2j.gameserver.model.zone;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.entity.Castle;

public abstract class ACastleZoneType extends L2ZoneType {
    private int castleId;
    private Castle castle;
    private boolean enabled;

    protected ACastleZoneType(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("castleId")) { castleId = Integer.parseInt(value); }
        else { super.setParameter(name, value); }
    }

    @Override
    public void onDieInside(L2Character character) {}

    @Override
    public void onReviveInside(L2Character character) { }

    public Castle getCastle() {
        if (castleId > 0 && castle == null) { castle = CastleManager.getInstance().getCastleById(castleId); }

        return castle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean val) {
        enabled = val;
    }
}