package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;

public interface OnEquipListener {
    void onEquip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor);

    void onUnequip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor);
}