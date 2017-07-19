package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;

public class BowRodListener implements OnEquipListener {
    private static final BowRodListener instance = new BowRodListener();

    public static BowRodListener getInstance() {
        return instance;
    }

    @Override
    public void onEquip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
        if (slot != EPaperdollSlot.PAPERDOLL_RHAND) { return; }

        if (item.getItemType() == EWeaponType.BOW) {
            L2ItemInstance arrow = actor.getInventory().findArrowForBow(item.getItem());
            if (arrow != null) { actor.getInventory().setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, arrow); }
        }
    }

    @Override
    public void onUnequip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
        if (slot != EPaperdollSlot.PAPERDOLL_RHAND) { return; }

        if (item.getItemType() == EWeaponType.BOW) {
            L2ItemInstance arrow = actor.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
            if (arrow != null) {
                actor.getInventory().setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, null);
            }
        }
        else if (item.getItemType() == EWeaponType.FISHINGROD) {
            L2ItemInstance lure = actor.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
            if (lure != null) {
                actor.getInventory().setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, null);
            }
        }
    }
}