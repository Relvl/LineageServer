package net.sf.l2j.gameserver.playerpart.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.Config;
import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.serialize.converter.IntegerNot100OnlyConverter;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("ClassHasNoToStringMethod")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Recipe {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    public Integer id;
    @JacksonXmlProperty(localName = "isDwarven", isAttribute = true)
    public Boolean isDwarven;
    @JacksonXmlProperty(localName = "level", isAttribute = true)
    public Integer level;
    @JacksonXmlProperty(localName = "mana", isAttribute = true)
    public Integer mana;
    @JacksonXmlProperty(localName = "chance", isAttribute = true)
    @JsonSerialize(converter = IntegerNot100OnlyConverter.class)
    public Integer chance = 100;
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    public String name;
    /** ID предмета рецепта (не продукта, а самого рецепта) */
    @JacksonXmlProperty(localName = "recipeItemId")
    public Integer recipeItemId;

    @JacksonXmlProperty(localName = "ingredient")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<RecipeIngredient> ingredients = new ArrayList<>();

    @JacksonXmlProperty(localName = "production")
    @JacksonXmlElementWrapper(useWrapping = false)
    public RecipeIngredient product;

    private Item productItem;

    @DefaultConstructor
    public Recipe() {}

    public int getLevel() { return level; }

    public int getRecipeId() { return id; }

    public Integer getRecipeItemId() { return recipeItemId; }

    public boolean isDwarvenRecipe() { return isDwarven; }

    public void makeInfo() { productItem = ItemTable.getInstance().getTemplate(product.id); }

    private boolean isConsumable() { return productItem.isConsumable(); }

    public List<RecipeIngredient> getIngredients() { return ingredients; }

    public void doManufacture(L2PcInstance manufacturer, boolean privateStore, L2PcInstance requester) {
        if (!privateStore && (AttackStanceTaskManager.getInstance().isInAttackStance(manufacturer) || manufacturer.isInDuel())) {
            manufacturer.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
            return;
        }
        try {
            manufacturer.isInCraftMode(true);
            if (manufacturer.isAlikeDead() || requester.isAlikeDead()) {
                requester.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            if (manufacturer.isProcessingTransaction() || requester.isProcessingTransaction()) {
                requester.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            if (level > manufacturer.getRecipeController().getCraftSkillLevel(isDwarven)) {
                requester.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            int price = 0;
            if (manufacturer != requester) {
                for (L2ManufactureItem item : manufacturer.getCreateList().getList()) {
                    if (item.getRecipeId() == id) {
                        price = item.getCost();
                        if (requester.getAdena() < price) {
                            requester.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
                            return;
                        }
                        break;
                    }
                }
            }

            // Резервируем ингредиенты
            Inventory inv = requester.getInventory();
            Map<L2ItemInstance, Integer> ingredientItems = new HashMap<>();
            boolean allMaterials = true;
            for (RecipeIngredient ingredient : ingredients) {
                int quantity = isConsumable() ? (int) (ingredient.count * Config.RATE_CONSUMABLE_COST) : ingredient.count;
                if (quantity > 0) {
                    L2ItemInstance item = inv.getItemByItemId(ingredient.id);
                    if (item == null || item.getCount() < quantity) {
                        requester.sendPacket(SystemMessage
                                        .getSystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE)
                                        .addItemName(ingredient.id)
                                        .addItemNumber((item == null) ? quantity : quantity - item.getCount())
                        );
                        allMaterials = false;
                    }
                    else {
                        ingredientItems.put(item, quantity);
                    }
                }
            }
            if (!allMaterials) { return; }

            // Проверяем, есть ли мана
            if (manufacturer.getCurrentMp() < mana) {
                requester.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
                return;
            }

            // Если клафт публичный - вычитаем оплату
            if (requester != manufacturer && price > 0) {
                L2ItemInstance adenatransfer = requester.transferItem(
                        EItemProcessPurpose.CRAFT,
                        requester.getInventory().getAdenaInstance().getObjectId(),
                        price,
                        manufacturer.getInventory(),
                        manufacturer
                );
                if (adenatransfer == null) {
                    requester.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
                    return;
                }
            }

            // Вычитаем ману
            manufacturer.reduceCurrentMp(mana);

            // Забираем ингредиенты
            for (Entry<L2ItemInstance, Integer> material : ingredientItems.entrySet()) {
                inv.destroyItem(EItemProcessPurpose.CRAFT, material.getKey(), material.getValue(), requester, manufacturer);
                if (material.getValue() > 1) {
                    requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(material.getKey().getItemId()).addItemNumber(material.getValue()));
                }
                else {
                    requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(material.getKey().getItemId()));
                }
            }

            // Если крафт прокнул - отдаем результат
            if (Rnd.get(100) < chance) {
                requester.getInventory().addItem(EItemProcessPurpose.CRAFT, productItem.getItemId(), product.count, requester, manufacturer);

                if (requester != manufacturer) {
                    if (product.count > 1) {
                        manufacturer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_S1_FOR_S4_ADENA)
                                .addString(requester.getName())
                                .addNumber(product.count)
                                .addItemName(productItem.getItemId())
                                .addItemNumber(price));
                        requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_S3_S_FOR_S4_ADENA)
                                .addString(manufacturer.getName())
                                .addNumber(product.count)
                                .addItemName(productItem.getItemId())
                                .addItemNumber(price));
                    }
                    else {
                        manufacturer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_CREATED_FOR_S1_FOR_S3_ADENA)
                                .addString(requester.getName())
                                .addItemName(productItem.getItemId())
                                .addItemNumber(price));
                        requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_FOR_S3_ADENA)
                                .addString(manufacturer.getName())
                                .addItemName(productItem.getItemId())
                                .addItemNumber(price));
                    }
                }

                if (product.count > 1) { requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(productItem.getItemId()).addNumber(product.count)); }
                else { /*             */ requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(productItem.getItemId())); }

                if (requester == manufacturer) { requester.sendPacket(new RecipeItemMakeInfo(id, requester, 1)); }
                else { /*                     */ requester.sendPacket(new RecipeShopItemInfo(manufacturer, id)); }
            }
            // Если крафт не прокнул - троллим
            else {
                if (requester != manufacturer) {
                    manufacturer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CREATION_OF_S2_FOR_S1_AT_S3_ADENA_FAILED)
                            .addPcName(requester)
                            .addItemName(productItem.getItemId())
                            .addItemNumber(price));
                    requester.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_FAILED_TO_CREATE_S2_FOR_S3_ADENA)
                            .addPcName(manufacturer)
                            .addItemName(productItem.getItemId())
                            .addItemNumber(price));
                }
                else { requester.sendPacket(SystemMessageId.ITEM_MIXING_FAILED); }

                if (requester == manufacturer) { requester.sendPacket(new RecipeItemMakeInfo(id, requester, 0)); }
                else { /*                     */ requester.sendPacket(new RecipeShopItemInfo(manufacturer, id)); }
            }

            requester.sendPacket(new StatusUpdate(requester)
                            .addAttribute(StatusUpdate.CUR_MP, (int) requester.getCurrentMp())
                            .addAttribute(StatusUpdate.CUR_LOAD, requester.getCurrentLoad())
            );
            requester.sendPacket(new ItemList(requester, false));
        }
        finally {
            manufacturer.isInCraftMode(false);
        }
    }
}