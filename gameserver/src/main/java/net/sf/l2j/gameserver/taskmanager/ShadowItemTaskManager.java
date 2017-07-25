package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updates the timer and removes the {@link L2ItemInstance} as a shadow item.
 *
 * @author Hasha
 */
public class ShadowItemTaskManager implements Runnable, OnEquipListener {
    private static final int DELAY = 1; // 1 second

    private final Map<L2ItemInstance, L2PcInstance> _shadowItems = new ConcurrentHashMap<>();

    public static ShadowItemTaskManager getInstance() {
        return SingletonHolder._instance;
    }

    protected ShadowItemTaskManager() {
        // Run task each second.
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 1000, 1000);
    }

    @Override
    public final void onEquip(EPaperdollSlot slot, L2ItemInstance item, L2Playable playable) {
        // Must be a shadow item.
        if (!item.isShadowItem()) { return; }

        // Must be a player.
        if (!(playable instanceof L2PcInstance)) { return; }

        _shadowItems.put(item, (L2PcInstance) playable);
    }

    @Override
    public final void onUnequip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
        // Must be a shadow item.
        if (!item.isShadowItem()) { return; }

        _shadowItems.remove(item);
    }

    public final void remove(L2PcInstance player) {
        // List is empty, skip.
        if (_shadowItems.isEmpty()) { return; }

        // Remove ALL associated items.
        _shadowItems.values().removeAll(Collections.singleton(player));
    }

    @Override
    public final void run() {
        // List is empty, skip.
        if (_shadowItems.isEmpty()) { return; }

        // For all items.
        for (Entry<L2ItemInstance, L2PcInstance> entry : _shadowItems.entrySet()) {
            // Get item and player.
            L2ItemInstance item = entry.getKey();
            L2PcInstance player = entry.getValue();

            // Decrease item mana.
            int mana = item.decreaseMana(DELAY);

            // If not enough mana, destroy the item and inform the player.
            if (mana == -1) {
                // Remove item first.
                player.getInventory().unEquipItemInSlotAndRecord(EPaperdollSlot.getByIndex(item.getLocationSlot()));
                InventoryUpdate iu = new InventoryUpdate();
                iu.addModifiedItem(item);
                player.sendPacket(iu);

                // Destroy shadow item, remove from list.
                player.getInventory().destroyItem(EItemProcessPurpose.SHADOW_ITEM, item, item.getCount(), player, false);
                player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0).addItemName(item.getItemId()));
                _shadowItems.remove(item);

                continue;
            }

            // Enough mana, show messages.
            if (mana == 60 - DELAY) { player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1).addItemName(item.getItemId())); }
            else if (mana == 300 - DELAY) { player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5).addItemName(item.getItemId())); }
            else if (mana == 600 - DELAY) { player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10).addItemName(item.getItemId())); }

            // Update inventory every minute.
            if (mana % 60 == 60 - DELAY) {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addModifiedItem(item);
                player.sendPacket(iu);
            }
        }
    }

    private static class SingletonHolder {
        protected static final ShadowItemTaskManager _instance = new ShadowItemTaskManager();
    }
}