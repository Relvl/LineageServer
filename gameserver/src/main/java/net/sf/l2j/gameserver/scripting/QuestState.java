package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.*;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class QuestState {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestState.class);

    public static final byte STATE_CREATED = 0;
    public static final byte STATE_STARTED = 1;
    public static final byte STATE_COMPLETED = 2;

    private static final String QUEST_SET_VAR = "REPLACE INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)";
    private static final String QUEST_DEL_VAR = "DELETE FROM character_quests WHERE charId=? AND name=? AND var=?";
    private static final String QUEST_DELETE = "DELETE FROM character_quests WHERE charId=? AND name=?";
    private static final String QUEST_COMPLETE = "DELETE FROM character_quests WHERE charId=? AND name=? AND var<>'<state>'";

    public static final byte DROP_DIVMOD = 0;
    public static final byte DROP_FIXED_RATE = 1;
    public static final byte DROP_FIXED_COUNT = 2;
    public static final byte DROP_FIXED_BOTH = 3;

    private final L2PcInstance player;
    private final Quest quest;
    private byte state;
    private final Map<String, String> vars = new HashMap<>();

    QuestState(L2PcInstance player, Quest quest, byte state) {
        this.player = player;
        this.quest = quest;
        this.state = state;
        this.player.setQuestState(this);
    }

    public L2PcInstance getPlayer() { return player; }

    public Quest getQuest() { return quest; }

    public byte getState() { return state; }

    public boolean isCreated() { return state == STATE_CREATED; }

    public boolean isCompleted() { return state == STATE_COMPLETED; }

    public boolean isStarted() { return state == STATE_STARTED; }

    public void setState(byte state) {
        if (this.state != state) {
            this.state = state;
            setQuestVarInDb("<state>", String.valueOf(this.state));
            player.sendPacket(new QuestList(player));
        }
    }

    public void exitQuest(boolean repeatable) {
        if (!isStarted()) { return; }
        player.removeNotifyQuestOfDeath(this);
        if (repeatable) {
            player.delQuestState(this);
            player.sendPacket(new QuestList(player));
        }
        else {
            setState(STATE_COMPLETED);
        }
        vars.clear();

        int[] itemIdList = quest.getItemsIds();
        if (itemIdList != null) {
            for (int itemId : itemIdList) {
                takeItems(itemId, -1);
            }
        }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement;
            if (repeatable) {
                statement = con.prepareStatement(QUEST_DELETE);
            }
            else {
                statement = con.prepareStatement(QUEST_COMPLETE);
            }
            statement.setInt(1, player.getObjectId());
            statement.setString(2, quest.getName());
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("could not delete char quest:", e);
        }
    }

    public void addNotifyOfDeath() {
        if (player != null) {
            player.addNotifyQuestOfDeath(this);
        }
    }

    public void set(String var, String value) {
        if (var == null || var.isEmpty() || value == null || value.isEmpty()) { return; }
        String old = vars.put(var, value);
        setQuestVarInDb(var, value);
        if ("cond".equals(var)) {
            try {
                int previousVal;
                try {
                    previousVal = Integer.parseInt(old);
                }
                catch (NumberFormatException ignored) {
                    previousVal = 0;
                }
                setCond(Integer.parseInt(value), previousVal);
            }
            catch (Exception e) {
                LOGGER.error("{}, {} cond [{}] is not an integer. Value stored, but no packet was sent: {}", player.getName(), quest.getName(), value, e.getMessage(), e);
            }
        }
    }

    public void setInternal(String var, String value) {
        if (var == null || var.isEmpty() || value == null || value.isEmpty()) { return; }
        vars.put(var, value);
    }

    private void setCond(int cond, int old) {
        if (cond == old) { return; }
        int completedStateFlags = 0;

        // cond 0 and 1 do not need completedStateFlags.
        // Also, if cond > 1, the 1st step must always exist (i.e. it can never be skipped).
        // So if cond is 2, we can still safely assume no steps have been skipped.
        // Finally, more than 31 steps CANNOT be supported in any way with skipping.
        if (cond < 3 || cond > 31) {
            unset("__compltdStateFlags");
        }
        else {
            completedStateFlags = getInt("__compltdStateFlags");
        }

        // case 1: No steps have been skipped so far...
        if (completedStateFlags == 0) {
            // check if this step also doesn't skip anything. If so, no further work is needed
            // also, in this case, no work is needed if the state is being reset to a smaller value
            // in those cases, skip forward to informing the client about the change...

            // ELSE, if we just now skipped for the first time...prepare the flags!!!
            if (cond > (old + 1)) {
                // set the most significant bit to 1 (indicates that there exist skipped states)
                // also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says)
                completedStateFlags = 0x80000001;
                // since no flag had been skipped until now, the least significant bits must all be set to 1, up until "old" number of bits.
                completedStateFlags |= (1 << old) - 1;
                // now, just set the bit corresponding to the passed cond to 1 (current step)
                completedStateFlags |= 1 << (cond - 1);
                set("__compltdStateFlags", String.valueOf(completedStateFlags));
            }
        }
        // case 2: There were exist previously skipped steps
        else {
            // if this is a push back to a previous step, clear all completion flags ahead
            if (cond < old) {
                // note, this also unsets the flag indicating that there exist skips
                completedStateFlags &= (1 << cond) - 1;
                // now, check if this resulted in no steps being skipped any more
                if (completedStateFlags == ((1 << cond) - 1)) {
                    unset("__compltdStateFlags");
                }
                else {
                    // set the most significant bit back to 1 again, to correctly indicate that this skips states.
                    // also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says)
                    completedStateFlags |= 0x80000001;
                    set("__compltdStateFlags", String.valueOf(completedStateFlags));
                }
            }
            // if this moves forward, it changes nothing on previously skipped steps...so just mark this state and we are done
            else {
                completedStateFlags |= 1 << (cond - 1);
                set("__compltdStateFlags", String.valueOf(completedStateFlags));
            }
        }
        player.sendPacket(new QuestList(player));
        if (quest.isRealQuest() && cond > 0) {
            player.sendPacket(new ExShowQuestMark(quest.getQuestId()));
        }
    }

    public void unset(String var) {
        if (vars.remove(var) != null) {
            removeQuestVarInDb(var);
        }
    }

    public String get(String var) { return vars.get(var); }

    public int getInt(String var) {
        String variable = vars.get(var);
        if (variable == null || variable.isEmpty()) { return 0; }
        int value = 0;
        try {
            value = Integer.parseInt(variable);
        }
        catch (NumberFormatException e) {
            LOGGER.error("{}: variable {} isn't an integer: {} ! {}", player.getName(), var, value, e.getMessage(), e);
        }
        return value;
    }

    private void setQuestVarInDb(String var, String value) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(QUEST_SET_VAR);
            statement.setInt(1, player.getObjectId());
            statement.setString(2, quest.getName());
            statement.setString(3, var);
            statement.setString(4, value);
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("could not insert char quest:", e);
        }
    }

    private void removeQuestVarInDb(String var) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(QUEST_DEL_VAR);
            statement.setInt(1, player.getObjectId());
            statement.setString(2, quest.getName());
            statement.setString(3, var);
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("could not delete char quest:", e);
        }
    }

    public boolean hasQuestItems(int itemId) { return player.getInventory().getItemByItemId(itemId) != null; }

    public boolean hasQuestItems(int... itemIds) {
        PcInventory inv = player.getInventory();
        for (int itemId : itemIds) {
            if (inv.getItemByItemId(itemId) == null) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAtLeastOneQuestItem(int... itemIds) { return player.getInventory().hasAtLeastOneItem(itemIds); }

    public int getQuestItemsCount(int itemId) {
        int count = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item != null && item.getItemId() == itemId) {
                count += item.getCount();
            }
        }
        return count;
    }

    public int getItemEquipped(EPaperdollSlot loc) { return player.getInventory().getPaperdollItemId(loc); }

    public int getEnchantLevel(int itemId) {
        L2ItemInstance enchanteditem = player.getInventory().getItemByItemId(itemId);
        if (enchanteditem == null) { return 0; }
        return enchanteditem.getEnchantLevel();
    }

    public void giveItems(int itemId, int itemCount) { giveItems(itemId, itemCount, 0); }

    public void giveItems(int itemId, int itemCount, int enchantLevel) {
        if (itemCount <= 0) { return; }
        L2ItemInstance item = player.getInventory().addItem(EItemProcessPurpose.QUEST, itemId, itemCount, player, player);
        if (item == null) { return; }
        if (enchantLevel > 0) { item.setEnchantLevel(enchantLevel); }
        // TODO Нахуй это дублировать?
        if (itemId == ItemConst.ADENA_ID) {
            SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
            smsg.addItemNumber(itemCount);
            player.sendPacket(smsg);
        }
        else {
            if (itemCount > 1) {
                SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
                smsg.addItemName(itemId);
                smsg.addItemNumber(itemCount);
                player.sendPacket(smsg);
            }
            else {
                SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
                smsg.addItemName(itemId);
                player.sendPacket(smsg);
            }
        }
        StatusUpdate su = new StatusUpdate(player);
        su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(su);
    }

    public void takeItems(int itemId, int itemCount) {
        L2ItemInstance item = player.getInventory().getItemByItemId(itemId);
        if (item == null) { return; }
        if (itemCount < 0 || itemCount > item.getCount()) { itemCount = item.getCount(); }
        if (item.isEquipped()) {
            L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (L2ItemInstance itm : unequiped) {
                iu.addModifiedItem(itm);
            }
            player.sendPacket(iu);
            player.broadcastUserInfo();
        }
        player.getInventory().destroyItemByItemId(EItemProcessPurpose.QUEST, itemId, itemCount, player, player, true);
    }

    public boolean dropItemsAlways(int itemId, int count, int neededCount) { return dropItems(itemId, count, neededCount, DropData.MAX_CHANCE, DROP_FIXED_RATE); }

    public boolean dropItems(int itemId, int count, int neededCount, int dropChance) { return dropItems(itemId, count, neededCount, dropChance, DROP_DIVMOD); }

    public boolean dropItems(int itemId, int count, int neededCount, int dropChance, byte type) {
        int currentCount = getQuestItemsCount(itemId);
        if (neededCount > 0 && currentCount >= neededCount) { return true; }
        int amount = 0;
        switch (type) {
            case DROP_DIVMOD:
                dropChance *= Config.RATE_QUEST_DROP;
                amount = count * (dropChance / DropData.MAX_CHANCE);
                if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE) { amount += count; }
                break;
            case DROP_FIXED_RATE:
                if (Rnd.get(DropData.MAX_CHANCE) < dropChance) { amount = (int) (count * Config.RATE_QUEST_DROP); }
                break;
            case DROP_FIXED_COUNT:
                if (Rnd.get(DropData.MAX_CHANCE) < dropChance * Config.RATE_QUEST_DROP) { amount = count; }
                break;
            case DROP_FIXED_BOTH:
                if (Rnd.get(DropData.MAX_CHANCE) < dropChance) { amount = count; }
                break;
        }

        boolean reached = false;
        if (amount > 0) {
            if (neededCount > 0) {
                reached = (currentCount + amount) >= neededCount;
                amount = (reached) ? neededCount - currentCount : amount;
            }
            if (!player.getInventory().validateCapacityByItemId(itemId)) { return false; }
            giveItems(itemId, amount, 0);
            playSound(reached ? ESound.ItemSound_quest_middle : ESound.ItemSound_quest_itemget);
        }
        return neededCount > 0 && reached;
    }

    public boolean dropMultipleItems(int[][] rewardsInfos) { return dropMultipleItems(rewardsInfos, DROP_DIVMOD); }

    public boolean dropMultipleItems(int[][] rewardsInfos, byte type) {
        boolean sendSound = false;
        boolean reached = true;
        for (int[] info : rewardsInfos) {
            int itemId = info[0];
            int currentCount = getQuestItemsCount(itemId);
            int neededCount = info[2];

            if (neededCount > 0 && currentCount >= neededCount) { continue; }
            int count = info[1];
            int dropChance = info[3];
            int amount = 0;

            switch (type) {
                case DROP_DIVMOD:
                    dropChance *= Config.RATE_QUEST_DROP;
                    amount = count * (dropChance / DropData.MAX_CHANCE);
                    if (Rnd.get(DropData.MAX_CHANCE) < dropChance % DropData.MAX_CHANCE) { amount += count; }
                    break;
                case DROP_FIXED_RATE:
                    if (Rnd.get(DropData.MAX_CHANCE) < dropChance) { amount = (int) (count * Config.RATE_QUEST_DROP); }
                    break;
                case DROP_FIXED_COUNT:
                    if (Rnd.get(DropData.MAX_CHANCE) < dropChance * Config.RATE_QUEST_DROP) { amount = count; }
                    break;
                case DROP_FIXED_BOTH:
                    if (Rnd.get(DropData.MAX_CHANCE) < dropChance) { amount = count; }
                    break;
            }

            if (amount > 0) {
                if (neededCount > 0) {
                    amount = ((currentCount + amount) >= neededCount) ? neededCount - currentCount : amount;
                }
                if (!player.getInventory().validateCapacityByItemId(itemId)) { continue; }
                giveItems(itemId, amount, 0);
                sendSound = true;
                if (neededCount <= 0 || ((currentCount + amount) < neededCount)) {
                    reached = false;
                }
            }
        }
        if (sendSound) {
            playSound((reached) ? ESound.ItemSound_quest_middle : ESound.ItemSound_quest_itemget);
        }
        return reached;
    }

    public void rewardItems(int itemId, int itemCount) {
        if (itemId == ItemConst.ADENA_ID) {
            giveItems(ItemConst.ADENA_ID, (int) (itemCount * Config.RATE_QUEST_REWARD_ADENA), 0);
        }
        else {
            giveItems(itemId, (int) (itemCount * Config.RATE_QUEST_REWARD), 0);
        }
    }

    public void rewardExpAndSp(long exp, int sp) { player.addExpAndSp((long) (exp * Config.RATE_QUEST_REWARD_XP), (int) (sp * Config.RATE_QUEST_REWARD_SP)); }

    // TODO: More radar functions need to be added when the radar class is complete.
    // region STUFF THAT WILL PROBABLY BE CHANGED
    public void addRadar(int x, int y, int z) { player.getRadar().addMarker(x, y, z); }

    public void removeRadar(int x, int y, int z) { player.getRadar().removeMarker(x, y, z); }

    public void clearRadar() { player.getRadar().removeAllMarkers(); }
    // endregion STUFF THAT WILL PROBABLY BE CHANGED

    public void playSound(ESound sound) { player.sendPacket(new PlaySound(sound)); }

    public void showQuestionMark(int number) { player.sendPacket(new TutorialShowQuestionMark(number)); }

    public void playTutorialVoice(String voice) { player.sendPacket(new PlaySound(2, voice, 0, 0, player.getX(), player.getY(), player.getZ())); }

    public void showTutorialHTML(String html) { player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getHtmForce("data/html/scripts/quests/Tutorial/" + html))); }

    public void closeTutorialHtml() { player.sendPacket(TutorialCloseHtml.STATIC_PACKET); }

    public void onTutorialClientEvent(int number) { player.sendPacket(new TutorialEnableClientEvent(number)); }
}
