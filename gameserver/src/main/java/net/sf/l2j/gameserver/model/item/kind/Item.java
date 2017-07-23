package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.EItemType1;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.*;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.basefuncs.FuncTemplate;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.templates.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Item {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Item.class);

    private final int itemId;
    private final String itemName;
    protected EItemType1 itemType1;
    protected EItemType2 itemType2; // different lists for armor, weapon, etc
    private final EItemBodyPart bodyPart;
    private final MaterialType materialType;
    private final int weight;

    private final CrystalType crystalType;
    private final int crystalCount;

    private final int referencePrice;
    private final int _duration;

    private final boolean sellable;
    private final boolean stackable;
    private final boolean dropable;
    private final boolean destroyable;
    private final boolean tradable;
    private final boolean depositable;

    private final boolean heroItem;
    private final boolean isOlyRestricted;

    protected final ActionType defaultAction;

    protected List<FuncTemplate> _funcTemplates;

    protected List<Condition> _preConditions;
    private IntIntHolder[] _skillHolder;

    private final List<Quest> _questEvents = new ArrayList<>();

    protected Item(StatsSet set) {
        itemId = set.getInteger("item_id");
        itemName = set.getString("name");
        weight = set.getInteger("weight", 0);
        materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
        _duration = set.getInteger("duration", -1);
        bodyPart = EItemBodyPart.getByCode(set.getString("bodypart", "none"));
        referencePrice = set.getInteger("price", 0);
        crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
        crystalCount = set.getInteger("crystal_count", 0);

        stackable = set.getBool("is_stackable", false);
        sellable = set.getBool("is_sellable", true);
        dropable = set.getBool("is_dropable", true);
        destroyable = set.getBool("is_destroyable", true);
        tradable = set.getBool("is_tradable", true);
        depositable = set.getBool("is_depositable", true);

        heroItem = (itemId >= 6611 && itemId <= 6621) || itemId == 6842;
        isOlyRestricted = set.getBool("is_oly_restricted", false);

        defaultAction = set.getEnum("default_action", ActionType.class, ActionType.none);

        String skills = set.getString("item_skill", null);
        if (skills != null) {
            String[] skillsSplit = skills.split(";");
            _skillHolder = new IntIntHolder[skillsSplit.length];
            int used = 0;

            for (String element : skillsSplit) {
                try {
                    String[] skillSplit = element.split("-");
                    int id = Integer.parseInt(skillSplit[0]);
                    int level = Integer.parseInt(skillSplit[1]);

                    if (id == 0) {
                        LOGGER.info("Ignoring item_skill({}) for item {}. Skill id is 0.", element, toString());
                        continue;
                    }
                    if (level == 0) {
                        LOGGER.info("Ignoring item_skill({}) for item {}. Skill level is 0.", element, toString());
                        continue;
                    }

                    _skillHolder[used] = new IntIntHolder(id, level);
                    ++used;
                }
                catch (Exception e) {
                    LOGGER.error("Failed to parse item_skill({}) for item {}. The used format is wrong.", element, toString());
                }
            }

            // this is only loading? just don't leave a null or use a collection?
            if (used != _skillHolder.length) {
                IntIntHolder[] skillHolder = new IntIntHolder[used];
                System.arraycopy(_skillHolder, 0, skillHolder, 0, used);
                _skillHolder = skillHolder;
            }
        }
    }

    public abstract EMaskedItemType getItemType();

    public final int getDuration() {
        return _duration;
    }

    public final int getItemId() {
        return itemId;
    }

    public abstract int getItemMask();

    public final EItemType2 getType2() {
        return itemType2;
    }

    public final int getWeight() {
        return weight;
    }

    public final boolean isCrystallizable() {
        return crystalType != CrystalType.NONE && crystalCount > 0;
    }

    public final CrystalType getCrystalType() {
        return crystalType;
    }

    public final int getCrystalItemId() {
        return crystalType.getCrystalId();
    }

    public final int getCrystalCount() {
        return crystalCount;
    }

    public final int getCrystalCount(int enchantLevel) {
        if (enchantLevel > 3) {
            switch (itemType2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return crystalCount + crystalType.getCrystalEnchantBonusArmor() * (3 * enchantLevel - 6);
                case TYPE2_WEAPON:
                    return crystalCount + crystalType.getCrystalEnchantBonusWeapon() * (2 * enchantLevel - 3);

                default:
                    return crystalCount;
            }
        }
        else if (enchantLevel > 0) {
            switch (itemType2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return crystalCount + crystalType.getCrystalEnchantBonusArmor() * enchantLevel;
                case TYPE2_WEAPON:
                    return crystalCount + crystalType.getCrystalEnchantBonusWeapon() * enchantLevel;
                default:
                    return crystalCount;
            }
        }
        else { return crystalCount; }
    }

    public final String getName() {
        return itemName;
    }

    public final EItemBodyPart getBodyPart() {
        return bodyPart;
    }

    public final EItemType1 getType1() {
        return itemType1;
    }

    public final boolean isStackable() {
        return stackable;
    }

    public boolean isConsumable() {
        return false;
    }

    public boolean isEquipable() {
        return bodyPart != EItemBodyPart.SLOT_NONE && !(getItemType() instanceof EtcItemType);
    }

    public final int getReferencePrice() {
        return isConsumable() ? (int) (referencePrice * Config.RATE_CONSUMABLE_COST) : referencePrice;
    }

    public final boolean isSellable() {
        return sellable;
    }

    public final boolean isDropable() {
        return dropable;
    }

    public final boolean isDestroyable() {
        return destroyable;
    }

    public final boolean isTradable() {
        return tradable;
    }

    public final boolean isDepositable() {
        return depositable;
    }

    public final List<Func> getStatFuncs(L2ItemInstance item, L2Character player) {
        if (_funcTemplates == null || _funcTemplates.isEmpty()) { return Collections.emptyList(); }

        List<Func> funcs = new ArrayList<>(_funcTemplates.size());

        Env env = new Env();
        env.setCharacter(player);
        env.setTarget(player);
        env.setItem(item);

        for (FuncTemplate t : _funcTemplates) {
            Func func = t.getFunc(env, item);
            if (func != null) { funcs.add(func); }
        }
        return funcs;
    }

    public void attach(FuncTemplate func) {
        if (_funcTemplates == null) { _funcTemplates = new ArrayList<>(1); }
        _funcTemplates.add(func);
    }

    public final void attach(Condition condition) {
        if (_preConditions == null) { _preConditions = new ArrayList<>(); }
        if (!_preConditions.contains(condition)) { _preConditions.add(condition); }
    }

    public final IntIntHolder[] getSkills() {
        return _skillHolder;
    }

    public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage) {
        // Don't allow hero equipment and restricted items during Olympiad
        if ((isOlyRestricted || heroItem) && activeChar.isPlayer() && activeChar.getActingPlayer().isInOlympiadMode()) {
            if (isEquipable()) {
                activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
            }
            else {
                activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            }
            return false;
        }

        if (_preConditions == null) { return true; }

        Env env = new Env();
        env.setCharacter(activeChar);
        if (target instanceof L2Character) { env.setTarget((L2Character) target); }

        for (Condition preCondition : _preConditions) {
            if (preCondition == null) { continue; }

            if (!preCondition.test(env)) {
                if (activeChar instanceof L2Summon) {
                    activeChar.getActingPlayer().sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
                    return false;
                }

                if (sendMessage) {
                    String msg = preCondition.getMessage();
                    int msgId = preCondition.getMessageId();
                    if (msg != null) {
                        activeChar.sendMessage(msg);
                    }
                    else if (msgId != 0) {
                        SystemMessage sm = SystemMessage.getSystemMessage(msgId);
                        if (preCondition.isAddName()) { sm.addItemName(itemId); }
                        activeChar.sendPacket(sm);
                    }
                }
                return false;
            }
        }
        return true;
    }

    public boolean isQuestItem() {
        return getItemType() == EtcItemType.QUEST;
    }

    public final boolean isHeroItem() { return heroItem; }

    public boolean isOlyRestrictedItem() { return isOlyRestricted; }

    public boolean isPetItem() { return getItemType() == ArmorType.PET || getItemType() == EWeaponType.PET; }

    public boolean isPotion() { return getItemType() == EtcItemType.POTION; }

    public boolean isElixir() { return getItemType() == EtcItemType.ELIXIR; }

    public boolean isWeapon() { return false; }

    public ActionType getDefaultAction() { return defaultAction; }

    public void addQuestEvent(Quest q) {
        _questEvents.add(q);
    }

    public List<Quest> getQuestEvents() {
        return _questEvents;
    }

    @Override
    public String toString() {
        return itemName + " (" + itemId + ")";
    }
}