package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ArmorSetListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.BowRodListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.StatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.ShadowItemTaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PcInventory extends Inventory {

    private final L2PcInstance player;
    private L2ItemInstance adena;
    private L2ItemInstance ancientAdena;

    public PcInventory(L2PcInstance owner) {
        player = owner;

        addPaperdollListener(ArmorSetListener.getInstance());
        addPaperdollListener(BowRodListener.getInstance());
        addPaperdollListener(ItemPassiveSkillsListener.getInstance());
        addPaperdollListener(ShadowItemTaskManager.getInstance());
    }

    @Override
    public L2PcInstance getOwner() { return player; }

    @Override
    protected EItemLocation getBaseLocation() { return EItemLocation.INVENTORY; }

    @Override
    protected EItemLocation getEquipLocation() { return EItemLocation.PAPERDOLL; }

    public L2ItemInstance getAdenaInstance() { return adena; }

    @Override
    public int getAdena() { return adena != null ? adena.getCount() : 0; }

    public int getAncientAdena() { return (ancientAdena != null) ? ancientAdena.getCount() : 0; }

    /**
     * Получение уникальных предметов.
     *
     * @param strictEnchant Уникальность только для уникальных уровней зачарования.
     */
    public List<L2ItemInstance> getUniqueItems(boolean allowAncientAdena, boolean onlyAvailable, boolean strictEnchant) {
        List<L2ItemInstance> list = new ArrayList<>();
        for (L2ItemInstance item : items) {
            if (item == null) { continue; }
            if (item.getItemId() == ItemConst.ADENA_ID) { continue; }
            if (!allowAncientAdena && item.getItemId() == ItemConst.ANCIENT_ADENA_ID) { continue; }
            if (onlyAvailable && !item.isSellable()) { continue; }
            if (onlyAvailable && !item.isAvailable(player, false, false)) { continue; }
            if (list.stream().anyMatch(litem -> litem.getItemId() == item.getItemId() && (!strictEnchant || litem.getEnchantLevel() == item.getEnchantLevel()))) { continue; }
            list.add(item);
        }
        return list;
    }

    // TODO Нужен метод получения предметов по ID, с сортировкой по восхождению уровня заточки.

    public List<L2ItemInstance> getAllItemsByItemId(int itemId, boolean includeEquipped) {
        return items.stream()
                .filter(item -> item != null && item.getItemId() == itemId && (includeEquipped || !item.isEquipped()))
                .collect(Collectors.toList());
    }

    public List<L2ItemInstance> getAllItemsByItemId(int itemId, int enchantment) {
        return items.stream()
                .filter(item -> item != null && item.getItemId() == itemId && item.getEnchantLevel() == enchantment)
                .collect(Collectors.toList());
    }

    public List<L2ItemInstance> getAvailableItems(boolean allowAdena, boolean allowNonTradeable) {
        return items.stream()
                .filter(item -> item != null && item.isAvailable(player, allowAdena, allowNonTradeable))
                .collect(Collectors.toList());
    }

    public List<L2ItemInstance> getAugmentedItems() {
        return items.stream()
                .filter(item -> item != null && item.isAugmented())
                .collect(Collectors.toList());
    }

    public List<TradeItem> getAvailableItems(TradeList tradeList) {
        return items.stream()
                .filter(item -> item != null && item.isAvailable(player, false, false))
                .map(tradeList::adjustAvailableItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** @deprecated Почти точно такой же метод есть в TradeItem. Нужно решить и выбрать один. */
    @Deprecated
    public void adjustAvailableItem(TradeItem item) {
        boolean notAllEquipped = false;
        for (L2ItemInstance adjItem : getItemsByItemId(item.getItem().getItemId())) {
            if (adjItem.isEquipable()) {
                if (!adjItem.isEquipped()) { notAllEquipped |= true; }
            }
            else {
                notAllEquipped |= true;
                break;
            }
        }
        if (notAllEquipped) {
            L2ItemInstance adjItem = getItemByItemId(item.getItem().getItemId());
            item.setObjectId(adjItem.getObjectId());
            item.setEnchant(adjItem.getEnchantLevel());
            if (adjItem.getCount() < item.getCount()) {
                item.setCount(adjItem.getCount());
            }
            return;
        }
        item.setCount(0);
    }

    public void addAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (sendMessage) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
        }

        if (count > 0) {
            addItem(process, ItemConst.ADENA_ID, count, player, reference);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(adena);
            player.sendPacket(iu);
        }
    }

    public boolean reduceAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (count > getAdena()) {
            if (sendMessage) {
                player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            }
            return false;
        }
        if (count > 0) {
            if (destroyItemByItemId(process, ItemConst.ADENA_ID, count, player, reference, true) == null) { return false; }
        }
        return true;

    }

    public void addAncientAdena(EItemProcessPurpose process, int count, L2Object reference) {
        if (count > 0) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(ItemConst.ANCIENT_ADENA_ID).addNumber(count));
            addItem(process, ItemConst.ANCIENT_ADENA_ID, count, player, reference);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(ancientAdena);
            player.sendPacket(iu);
        }
    }

    public boolean reduceAncientAdena(EItemProcessPurpose process, int count, L2Object reference) {
        if (count > getAncientAdena()) {
            player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            return false;
        }
        return count <= 0 && destroyItemByItemId(process, ItemConst.ANCIENT_ADENA_ID, count, player, reference, true) != null;
    }

    @Override
    public L2ItemInstance addItem(EItemProcessPurpose process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        item = super.addItem(process, item, actor, reference);
        if (item == null) { return null; }
        if (item.getItemId() == ItemConst.ADENA_ID && !item.equals(adena)) { adena = item; }
        else if (item.getItemId() == ItemConst.ANCIENT_ADENA_ID && !item.equals(ancientAdena)) { ancientAdena = item; }
        return item;
    }

    @Override
    public L2ItemInstance addItem(EItemProcessPurpose process, int itemId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = super.addItem(process, itemId, count, actor, reference);
        if (item == null) { return null; }
        if (item.getItemId() == ItemConst.ADENA_ID && !item.equals(adena)) { adena = item; }
        else if (item.getItemId() == ItemConst.ANCIENT_ADENA_ID && !item.equals(ancientAdena)) { ancientAdena = item; }

        if (actor != null) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            actor.sendPacket(playerIU);

            StatusUpdate su = new StatusUpdate(actor);
            su.addAttribute(StatusUpdate.CUR_LOAD, actor.getCurrentLoad());
            actor.sendPacket(su);
        }
        return item;
    }

    @Override
    public L2ItemInstance transferItem(EItemProcessPurpose process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
        if (adena != null && (adena.getCount() <= 0 || adena.getOwnerId() != getOwnerId())) { adena = null; }
        if (ancientAdena != null && (ancientAdena.getCount() <= 0 || ancientAdena.getOwnerId() != getOwnerId())) { ancientAdena = null; }
        return item;
    }

    // -===========================================================================================================-

    public L2ItemInstance destroyItem(EItemProcessPurpose process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage) {
        return destroyItem(process, item, count, player, reference, sendMessage);
    }

    @Override
    protected L2ItemInstance destroyItem(EItemProcessPurpose process, L2ItemInstance item, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        item = super.destroyItem(process, item, count, actor, reference, sendMessage);
        if (adena != null && adena.getCount() <= 0) { adena = null; }
        if (ancientAdena != null && ancientAdena.getCount() <= 0) { ancientAdena = null; }

        if (item == null) {
            if (sendMessage) { player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }
            return null;
        }

        InventoryUpdate iu = new InventoryUpdate();
        iu.addItem(item);
        player.sendPacket(iu);

        StatusUpdate su = new StatusUpdate(player);
        su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(su);

        if (sendMessage) {
            if (item == adena) {
                player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA).addNumber(count));
            }
            else if (count > 1) {
                player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item).addItemNumber(count));
            }
            else {
                player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item));
            }
        }

        return item;
    }

    // -===========================================================================================================-

    @Override
    public L2ItemInstance dropItem(EItemProcessPurpose process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        item = super.dropItem(process, item, actor, reference);
        if (adena != null && (adena.getCount() <= 0 || adena.getOwnerId() != getOwnerId())) { adena = null; }
        if (ancientAdena != null && (ancientAdena.getCount() <= 0 || ancientAdena.getOwnerId() != getOwnerId())) { ancientAdena = null; }
        return item;
    }

    @Override
    public L2ItemInstance dropItem(EItemProcessPurpose process, int objectId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
        if (adena != null && (adena.getCount() <= 0 || adena.getOwnerId() != getOwnerId())) { adena = null; }
        if (ancientAdena != null && (ancientAdena.getCount() <= 0 || ancientAdena.getOwnerId() != getOwnerId())) { ancientAdena = null; }
        return item;
    }

    @Override
    protected boolean removeItem(L2ItemInstance item) {
        player.removeItemFromShortCut(item.getObjectId());
        if (item.equals(player.getActiveEnchantItem())) { player.setActiveEnchantItem(null); }
        if (item.getItemId() == ItemConst.ADENA_ID) { adena = null; }
        else if (item.getItemId() == ItemConst.ANCIENT_ADENA_ID) { ancientAdena = null; }
        return super.removeItem(item);
    }

    @Override
    public void refreshWeight() {
        super.refreshWeight();
        player.refreshOverloaded();
    }

    @Override
    public void restore() {
        super.restore();
        adena = getItemByItemId(ItemConst.ADENA_ID);
        ancientAdena = getItemByItemId(ItemConst.ANCIENT_ADENA_ID);
    }

    public boolean validateCapacity(L2ItemInstance item) {
        int slots = 0;
        if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB) {
            slots++;
        }
        return validateCapacity(slots);
    }

    public boolean validateCapacityByItemId(int itemId) {
        int slots = 0;
        L2ItemInstance invItem = getItemByItemId(itemId);
        if (!(invItem != null && invItem.isStackable())) { slots++; }
        return validateCapacity(slots);
    }

    @Override
    public boolean validateCapacity(int slots) {
        return items.size() + slots <= player.getInventoryLimit();
    }

    @Override
    public boolean validateWeight(int weight) {
        return totalWeight + weight <= player.getMaxLoad();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + player + "]";
    }
}