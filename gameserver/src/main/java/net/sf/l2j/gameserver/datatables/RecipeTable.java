package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecipeTable {
    protected static final Logger _log = Logger.getLogger(RecipeTable.class.getName());

    private final Map<Integer, Recipe> _lists = new HashMap<>();

    public static RecipeTable getInstance() {
        return SingletonHolder._instance;
    }

    protected RecipeTable() {
        try {
            File file = new File("./data/xml/recipes.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

            List<IntIntHolder> recipePartList = new ArrayList<>();

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if ("item".equalsIgnoreCase(d.getNodeName())) {
                    recipePartList.clear();
                    NamedNodeMap attrs = d.getAttributes();

                    Node att = attrs.getNamedItem("id");
                    if (att == null) {
                        _log.severe("Missing id for recipe item, skipping");
                        continue;
                    }
                    int id = Integer.parseInt(att.getNodeValue());

                    att = attrs.getNamedItem("name");
                    if (att == null) {
                        _log.severe("Missing name for recipe item id: " + id + ", skipping");
                        continue;
                    }
                    String recipeName = att.getNodeValue();

                    int recipeId = -1;
                    int level = -1;
                    boolean isDwarvenRecipe = true;
                    int mpCost = -1;
                    int successRate = -1;
                    int prodId = -1;
                    int count = -1;

                    for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if ("recipe".equalsIgnoreCase(c.getNodeName())) {
                            recipeId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                            level = Integer.parseInt(c.getAttributes().getNamedItem("level").getNodeValue());
                            isDwarvenRecipe = c.getAttributes().getNamedItem("type").getNodeValue().equalsIgnoreCase("dwarven");
                        }
                        else if ("mpCost".equalsIgnoreCase(c.getNodeName())) {
                            mpCost = Integer.parseInt(c.getTextContent());
                        }
                        else if ("successRate".equalsIgnoreCase(c.getNodeName())) {
                            successRate = Integer.parseInt(c.getTextContent());
                        }
                        else if ("ingredient".equalsIgnoreCase(c.getNodeName())) {
                            int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                            int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
                            recipePartList.add(new IntIntHolder(ingId, ingCount));
                        }
                        else if ("production".equalsIgnoreCase(c.getNodeName())) {
                            prodId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                            count = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
                        }
                    }

                    Recipe recipe = new Recipe(id, level, recipeId, recipeName, successRate, mpCost, prodId, count, isDwarvenRecipe);
                    for (IntIntHolder recipePart : recipePartList) { recipe.addNeededRecipePart(recipePart); }

                    _lists.put(id, recipe);
                }
            }

            _log.info("RecipeTable: Loaded " + _lists.size() + " recipes.");
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "RecipeTable: Failed loading recipe list", e);
        }
    }

    public Recipe getRecipeList(int listId) { return _lists.get(listId); }

    public Recipe getRecipeByItemId(int itemId) {
        for (Recipe find : _lists.values()) {
            if (find.getRecipeId() == itemId) { return find; }
        }
        return null;
    }

    public void requestManufactureItem(L2PcInstance manufacturer, int recipeListId, L2PcInstance player) {
        Recipe recipe = getValidRecipeList(player, recipeListId);
        if (recipe == null) { return; }
        if (!player.getRecipeController().hasRecipe(recipe.getId())) {
            Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
            return;
        }
        RecipeItemMaker maker = new RecipeItemMaker(manufacturer, recipe, player);
        if (maker._isValid) { maker.run(); }
    }

    public void requestMakeItem(L2PcInstance player, int recipeListId) {
        if (AttackStanceTaskManager.getInstance().isInAttackStance(player) || player.isInDuel()) {
            player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
            return;
        }

        Recipe recipe = getValidRecipeList(player, recipeListId);
        if (recipe == null) { return; }
        if (!player.getRecipeController().hasRecipe(recipe.getId())) {
            Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
            return;
        }

        RecipeItemMaker maker = new RecipeItemMaker(player, recipe, player);
        if (maker._isValid) { maker.run(); }
    }

    private class RecipeItemMaker implements Runnable {
        protected boolean _isValid;
        protected final Recipe _recipe;
        protected final L2PcInstance _player; // "crafter"
        protected final L2PcInstance _target; // "customer"
        protected final int _skillId;
        protected final int _skillLevel;
        protected double _manaRequired;
        protected int _price;

        public RecipeItemMaker(L2PcInstance pPlayer, Recipe pRecipe, L2PcInstance pTarget) {
            _player = pPlayer;
            _target = pTarget;
            _recipe = pRecipe;

            _isValid = false;
            _skillId = _recipe.isDwarvenRecipe() ? L2Skill.SKILL_CREATE_DWARVEN : L2Skill.SKILL_CREATE_COMMON;
            _skillLevel = _player.getSkillLevel(_skillId);

            _manaRequired = _recipe.getMana();

            _player.isInCraftMode(true);

            if (_player.isAlikeDead() || _target.isAlikeDead()) {
                _player.sendPacket(ActionFailed.STATIC_PACKET);
                abort();
                return;
            }

            if (_player.isProcessingTransaction() || _target.isProcessingTransaction()) {
                _target.sendPacket(ActionFailed.STATIC_PACKET);
                abort();
                return;
            }

            // validate recipe list
            if (_recipe.getNeededRecipeParts().isEmpty()) {
                _player.sendPacket(ActionFailed.STATIC_PACKET);
                abort();
                return;
            }

            // validate skill level
            if (_recipe.getLevel() > _skillLevel) {
                _player.sendPacket(ActionFailed.STATIC_PACKET);
                abort();
                return;
            }

            // check that customer can afford to pay for creation services
            if (_player != _target) {
                for (L2ManufactureItem temp : _player.getCreateList().getList()) {
                    if (temp.getRecipeId() == _recipe.getId()) // find recipe for item we want manufactured
                    {
                        _price = temp.getCost();
                        if (_target.getAdena() < _price) // check price
                        {
                            _target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
                            abort();
                            return;
                        }
                        break;
                    }
                }
            }

            // Check if inventory got all required materials.
            if (!listItems(false)) {
                abort();
                return;
            }

            // initial mana check requires MP as written on recipe
            if (_player.getCurrentMp() < _manaRequired) {
                _target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
                abort();
                return;
            }

            updateMakeInfo(true);
            updateStatus();

            _player.isInCraftMode(false);
            _isValid = true;
        }

        @Override
        public void run() {
            if (!Config.IS_CRAFTING_ENABLED) {
                _target.sendMessage("Item creation is currently disabled.");
                abort();
                return;
            }

            if (_player == null || _target == null) {
                _log.warning("Player or target == null (disconnected?), aborting" + _target + _player);
                abort();
                return;
            }

            if (!_player.isOnline() || !_target.isOnline()) {
                _log.warning("Player or target is not online, aborting " + _target + _player);
                abort();
                return;
            }

            _player.reduceCurrentMp(_manaRequired);

            // first take adena for manufacture
            if (_target != _player && _price > 0) // customer must pay for services
            {
                // attempt to pay for item
                L2ItemInstance adenatransfer = _target.transferItem(EItemProcessPurpose.CRAFT, _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player);
                if (adenatransfer == null) {
                    _target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
                    abort();
                    return;
                }
            }

            // Inventory check failed.
            if (!listItems(true)) {
                abort();
                return;
            }

            if (Rnd.get(100) < _recipe.getChance()) {
                rewardPlayer(); // and immediately puts created item in its place
                updateMakeInfo(true);
            }
            else {
                if (_target != _player) {
                    _player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CREATION_OF_S2_FOR_S1_AT_S3_ADENA_FAILED).addPcName(_target).addItemName(_recipe.getItemId()).addItemNumber(_price));
                    _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_FAILED_TO_CREATE_S2_FOR_S3_ADENA).addPcName(_player).addItemName(_recipe.getItemId()).addItemNumber(_price));
                }
                else { _target.sendPacket(SystemMessageId.ITEM_MIXING_FAILED); }

                updateMakeInfo(false);
            }

            updateStatus();

            _player.isInCraftMode(false);
            _target.sendPacket(new ItemList(_target, false));
        }

        private void updateMakeInfo(boolean success) {
            if (_target == _player) { _target.sendPacket(new RecipeItemMakeInfo(_recipe.getId(), _target, (success) ? 1 : 0)); }
            else { _target.sendPacket(new RecipeShopItemInfo(_player, _recipe.getId())); }
        }

        private void updateStatus() {
            StatusUpdate su = new StatusUpdate(_target);
            su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getCurrentMp());
            su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
            _target.sendPacket(su);
        }

        private boolean listItems(boolean remove) {
            Inventory inv = _target.getInventory();
            Collection<IntIntHolder> materials = new ArrayList<>();

            boolean gotAllMats = true;
            for (IntIntHolder neededPart : _recipe.getNeededRecipeParts()) {
                int quantity = _recipe.isConsumable() ? (int) (neededPart.getValue() * Config.RATE_CONSUMABLE_COST) : neededPart.getValue();
                if (quantity > 0) {
                    L2ItemInstance item = inv.getItemByItemId(neededPart.getId());
                    if (item == null || item.getCount() < quantity) {
                        _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE).addItemName(neededPart.getId()).addItemNumber((item == null) ? quantity : quantity - item.getCount()));
                        gotAllMats = false;
                    }
                    else if (remove) { materials.add(new IntIntHolder(item.getItemId(), quantity)); }
                }
            }

            if (!gotAllMats) { return false; }

            if (remove) {
                for (IntIntHolder material : materials) {
                    inv.destroyItemByItemId(EItemProcessPurpose.CRAFT, material.getId(), material.getValue(), _target, _player);

                    if (material.getValue() > 1) { _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(material.getId()).addItemNumber(material.getValue())); }
                    else { _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(material.getId())); }
                }
            }
            return true;
        }

        private void abort() {
            updateMakeInfo(false);
            _player.isInCraftMode(false);
        }

        private void rewardPlayer() {
            int itemId = _recipe.getItemId();
            int itemCount = _recipe.getCount();

            _target.getInventory().addItem(EItemProcessPurpose.CRAFT, itemId, itemCount, _target, _player);

            // inform customer of earned item
            if (_target != _player) {
                // inform manufacturer of earned profit
                if (itemCount == 1) {
                    _player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_CREATED_FOR_S1_FOR_S3_ADENA).addString(_target.getName()).addItemName(itemId).addItemNumber(_price));
                    _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_FOR_S3_ADENA).addString(_player.getName()).addItemName(itemId).addItemNumber(_price));
                }
                else {
                    _player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_S1_FOR_S4_ADENA).addString(_target.getName()).addNumber(itemCount).addItemName(itemId).addItemNumber(_price));
                    _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_S3_S_FOR_S4_ADENA).addString(_player.getName()).addNumber(itemCount).addItemName(itemId).addItemNumber(_price));
                }
            }

            if (itemCount > 1) { _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(itemCount)); }
            else { _target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId)); }

            updateMakeInfo(true); // success
        }
    }

    private Recipe getValidRecipeList(L2PcInstance player, int id) {
        Recipe recipe = _lists.get(id);
        if (recipe == null || recipe.getNeededRecipeParts().isEmpty()) {
            player.sendMessage("No recipe for: " + id);
            player.isInCraftMode(false);
            return null;
        }
        return recipe;
    }

    private static class SingletonHolder {
        protected static final RecipeTable _instance = new RecipeTable();
    }
}