package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncPDefMod extends Func {
    public FuncPDefMod() { super(Stats.POWER_DEFENCE, 0x20, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getCharacter().isPlayer()) {
            L2PcInstance player = env.getPlayer();
            boolean hasMagePDef = player.getClassId().isMage() || player.getClassId().getId() == 0x31; // orc mystics are a special case
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_HEAD) != null) { env.subValue(12); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST) != null) { env.subValue((hasMagePDef) ? 15 : 31); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS) != null) { env.subValue((hasMagePDef) ? 8 : 18); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_GLOVES) != null) { env.subValue(8); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_FEET) != null) { env.subValue(7); }
        }
        env.mulValue(env.getCharacter().getLevelMod());
    }
}