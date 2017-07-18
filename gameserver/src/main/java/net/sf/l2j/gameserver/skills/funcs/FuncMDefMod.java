package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncMDefMod extends Func {
    static final FuncMDefMod _fpa_instance = new FuncMDefMod();

    public static Func getInstance() {
        return _fpa_instance;
    }

    private FuncMDefMod() {
        super(Stats.MAGIC_DEFENCE, 0x20, null, null);
    }

    @Override
    public void calc(Env env) {
        if (env.getCharacter() instanceof L2PetInstance) { return; }

        if (env.getCharacter() instanceof L2PcInstance) {
            L2PcInstance player = env.getPlayer();
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LFINGER) != null) { env.subValue(5); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_RFINGER) != null) { env.subValue(5); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LEAR) != null) { env.subValue(9); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_REAR) != null) { env.subValue(9); }
            if (player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_NECK) != null) { env.subValue(13); }
        }

        env.mulValue(Formulas.MENbonus[env.getCharacter().getMEN()] * env.getCharacter().getLevelMod());
    }
}