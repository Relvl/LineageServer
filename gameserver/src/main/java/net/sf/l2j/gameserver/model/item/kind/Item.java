package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public abstract class Item {
    private final int _itemId;
    private final String _name;
    protected EItemType1 itemType1;
    protected EItemType2 _type2; // different lists for armor, weapon, etc
    private final int _weight;
    private final boolean _stackable;
    private final MaterialType _materialType;
    private final CrystalType _crystalType;
    private final int _duration;
    private final EItemBodyPart bodyPart;
    private final int _referencePrice;
    private final int _crystalCount;

    private final boolean _sellable;
    private final boolean _dropable;
    private final boolean _destroyable;
    private final boolean _tradable;
    private final boolean _depositable;

    private final boolean _heroItem;
    private final boolean _isOlyRestricted;

    private final ActionType _defaultAction;

    protected List<FuncTemplate> _funcTemplates;

    protected List<Condition> _preConditions;
    private IntIntHolder[] _skillHolder;

    private final List<Quest> _questEvents = new ArrayList<>();

    protected static final Logger _log = Logger.getLogger(Item.class.getName());

    protected Item(StatsSet set) {
        _itemId = set.getInteger("item_id");
        _name = set.getString("name");
        _weight = set.getInteger("weight", 0);
        _materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
        _duration = set.getInteger("duration", -1);
        bodyPart = EItemBodyPart.getByCode(set.getString("bodypart", "none"));
        _referencePrice = set.getInteger("price", 0);
        _crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
        _crystalCount = set.getInteger("crystal_count", 0);

        _stackable = set.getBool("is_stackable", false);
        _sellable = set.getBool("is_sellable", true);
        _dropable = set.getBool("is_dropable", true);
        _destroyable = set.getBool("is_destroyable", true);
        _tradable = set.getBool("is_tradable", true);
        _depositable = set.getBool("is_depositable", true);

        _heroItem = (_itemId >= 6611 && _itemId <= 6621) || _itemId == 6842;
        _isOlyRestricted = set.getBool("is_oly_restricted", false);

        _defaultAction = set.getEnum("default_action", ActionType.class, ActionType.none);

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
                        _log.info("Ignoring item_skill(" + element + ") for item " + toString() + ". Skill id is 0.");
                        continue;
                    }

                    if (level == 0) {
                        _log.info("Ignoring item_skill(" + element + ") for item " + toString() + ". Skill level is 0.");
                        continue;
                    }

                    _skillHolder[used] = new IntIntHolder(id, level);
                    ++used;
                }
                catch (Exception e) {
                    _log.warning("Failed to parse item_skill(" + element + ") for item " + toString() + ". The used format is wrong.");
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

    /**
     * @return Enum the itemType.
     */
    public abstract EMaskedItemType getItemType();

    /**
     * @return int the duration of the item
     */
    public final int getDuration() {
        return _duration;
    }

    /**
     * @return int the ID of the item
     */
    public final int getItemId() {
        return _itemId;
    }

    public abstract int getItemMask();

    /**
     * @return int the type of material of the item
     */
    public final MaterialType getMaterialType() {
        return _materialType;
    }

    /**
     * @return int the type 2 of the item
     */
    public final EItemType2 getType2() {
        return _type2;
    }

    /**
     * @return int the weight of the item
     */
    public final int getWeight() {
        return _weight;
    }

    /**
     * @return boolean if the item is crystallizable
     */
    public final boolean isCrystallizable() {
        return _crystalType != CrystalType.NONE && _crystalCount > 0;
    }

    /**
     * @return CrystalType the type of crystal if item is crystallizable
     */
    public final CrystalType getCrystalType() {
        return _crystalType;
    }

    /**
     * @return int the type of crystal if item is crystallizable
     */
    public final int getCrystalItemId() {
        return _crystalType.getCrystalId();
    }

    /**
     * @return int the quantity of crystals for crystallization
     */
    public final int getCrystalCount() {
        return _crystalCount;
    }

    /**
     * @param enchantLevel
     * @return int the quantity of crystals for crystallization on specific enchant level
     */
    public final int getCrystalCount(int enchantLevel) {
        if (enchantLevel > 3) {
            switch (_type2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return _crystalCount + _crystalType.getCrystalEnchantBonusArmor() * (3 * enchantLevel - 6);

                case TYPE2_WEAPON:
                    return _crystalCount + _crystalType.getCrystalEnchantBonusWeapon() * (2 * enchantLevel - 3);

                default:
                    return _crystalCount;
            }
        }
        else if (enchantLevel > 0) {
            switch (_type2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return _crystalCount + _crystalType.getCrystalEnchantBonusArmor() * enchantLevel;
                case TYPE2_WEAPON:
                    return _crystalCount + _crystalType.getCrystalEnchantBonusWeapon() * enchantLevel;
                default:
                    return _crystalCount;
            }
        }
        else { return _crystalCount; }
    }

    /**
     * @return String the name of the item
     */
    public final String getName() {
        return _name;
    }

    /**
     * @return int the part of the body used with the item.
     */
    public final EItemBodyPart getBodyPart() {
        return bodyPart;
    }

    /**
     * @return int the type 1 of the item
     */
    public final EItemType1 getType1() {
        return itemType1;
    }

    /**
     * @return boolean if the item is stackable
     */
    public final boolean isStackable() {
        return _stackable;
    }

    /**
     * @return boolean if the item is consumable
     */
    public boolean isConsumable() {
        return false;
    }

    public boolean isEquipable() {
        return bodyPart != EItemBodyPart.SLOT_NONE && !(getItemType() instanceof EtcItemType);
    }

    /**
     * @return int the price of reference of the item
     */
    public final int getReferencePrice() {
        return isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice;
    }

    /**
     * Returns if the item can be sold
     *
     * @return boolean
     */
    public final boolean isSellable() {
        return _sellable;
    }

    /**
     * Returns if the item can dropped
     *
     * @return boolean
     */
    public final boolean isDropable() {
        return _dropable;
    }

    /**
     * Returns if the item can destroy
     *
     * @return boolean
     */
    public final boolean isDestroyable() {
        return _destroyable;
    }

    /**
     * Returns if the item can add to trade
     *
     * @return boolean
     */
    public final boolean isTradable() {
        return _tradable;
    }

    /**
     * Returns if the item can be put into warehouse
     *
     * @return boolean
     */
    public final boolean isDepositable() {
        return _depositable;
    }

    /**
     * Get the functions used by this item.
     *
     * @param item   : L2ItemInstance pointing out the item
     * @param player : L2Character pointing out the player
     * @return the list of functions
     */
    public final List<Func> getStatFuncs(L2ItemInstance item, L2Character player) {
        if (_funcTemplates == null || _funcTemplates.isEmpty()) { return Collections.emptyList(); }

        List<Func> funcs = new ArrayList<>(_funcTemplates.size());

        Env env = new Env();
        env.setCharacter(player);
        env.setTarget(player);
        env.setItem(item);

        for (FuncTemplate t : _funcTemplates) {
            Func f = t.getFunc(env, item);
            if (f != null) { funcs.add(f); }
        }
        return funcs;
    }

    /**
     * Add the FuncTemplate f to the list of functions used with the item
     *
     * @param f : FuncTemplate to add
     */
    public void attach(FuncTemplate f) {
        if (_funcTemplates == null) { _funcTemplates = new ArrayList<>(1); }

        _funcTemplates.add(f);
    }

    public final void attach(Condition c) {
        if (_preConditions == null) { _preConditions = new ArrayList<>(); }

        if (!_preConditions.contains(c)) { _preConditions.add(c); }
    }

    /**
     * Method to retrieve skills linked to this item
     *
     * @return Skills linked to this item as SkillHolder[]
     */
    public final IntIntHolder[] getSkills() {
        return _skillHolder;
    }

    public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage) {
        // Don't allow hero equipment and restricted items during Olympiad
        if ((_isOlyRestricted || _heroItem) && (activeChar instanceof L2PcInstance) && activeChar.getActingPlayer().isInOlympiadMode()) {
            if (isEquipable()) { activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT); }
            else { activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT); }

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
                        if (preCondition.isAddName()) { sm.addItemName(_itemId); }
                        activeChar.sendPacket(sm);
                    }
                }
                return false;
            }
        }
        return true;
    }

    public boolean isConditionAttached() {
        return _preConditions != null && !_preConditions.isEmpty();
    }

    public boolean isQuestItem() {
        return getItemType() == EtcItemType.QUEST;
    }

    public final boolean isHeroItem() {
        return _heroItem;
    }

    public boolean isOlyRestrictedItem() {
        return _isOlyRestricted;
    }

    public boolean isPetItem() {
        return getItemType() == ArmorType.PET || getItemType() == EWeaponType.PET;
    }

    public boolean isPotion() {
        return getItemType() == EtcItemType.POTION;
    }

    public boolean isElixir() {
        return getItemType() == EtcItemType.ELIXIR;
    }

    public ActionType getDefaultAction() {
        return _defaultAction;
    }

    /**
     * Returns the name of the item
     *
     * @return String
     */
    @Override
    public String toString() {
        return _name + " (" + _itemId + ")";
    }

    public void addQuestEvent(Quest q) {
        _questEvents.add(q);
    }

    public List<Quest> getQuestEvents() {
        return _questEvents;
    }
}