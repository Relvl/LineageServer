package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;

public class DoorStat extends CharStat {
    public DoorStat(L2DoorInstance activeChar) {
        super(activeChar);
        setLevel((byte) 1);
    }

    @Override
    public L2DoorInstance getActiveChar() {
        return (L2DoorInstance) super.getActiveChar();
    }

    @Override
    public int getMDef(L2Character target, L2Skill skill) {
        double defense = getActiveChar().getTemplate().getBaseMDef();

        int sealOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
        if (sealOwner == SevenSigns.CABAL_DAWN) { defense *= 1.2; }
        else if (sealOwner == SevenSigns.CABAL_DUSK) { defense *= 0.3; }

        return (int) defense;
    }

    @Override
    public int getPDef(L2Character target) {
        double defense = getActiveChar().getTemplate().getBasePDef();

        int sealOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
        if (sealOwner == SevenSigns.CABAL_DAWN) { defense *= 1.2; }
        else if (sealOwner == SevenSigns.CABAL_DUSK) { defense *= 0.3; }

        return (int) defense;
    }

    @Override
    public final byte getLevel() {
        return 1;
    }
}