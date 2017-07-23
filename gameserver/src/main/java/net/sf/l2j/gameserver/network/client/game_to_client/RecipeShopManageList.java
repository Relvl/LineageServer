package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.Recipe;

import java.util.Collection;
import java.util.Iterator;

public class RecipeShopManageList extends L2GameServerPacket {
    private final L2PcInstance seller;
    private final boolean isDwarven;
    private final Collection<Recipe> recipes;

    public RecipeShopManageList(L2PcInstance seller, boolean isDwarven) {
        this.seller = seller;
        this.isDwarven = isDwarven;

        recipes = this.isDwarven && seller.getRecipeController().hasDwarvenCraft() ? seller.getRecipeController().getDwarvenRecipes() : seller.getRecipeController().getCommonRecipes();

        // clean previous recipes
        if (seller.getCreateList() != null) {
            Iterator<L2ManufactureItem> it = seller.getCreateList().getList().iterator();
            while (it.hasNext()) {
                L2ManufactureItem item = it.next();
                if (item.isDwarven() != this.isDwarven || !seller.getRecipeController().hasRecipe(item.getRecipeId())) { it.remove(); }
            }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0xd8);
        writeD(seller.getObjectId());
        writeD(seller.getAdena());
        writeD(isDwarven ? 0x00 : 0x01);
        writeD(recipes.size());

        int i = 0;
        for (Recipe recipe : recipes) {
            writeD(recipe.getId());
            writeD(i + 1);
        }

        if (seller.getCreateList() == null) { writeD(0); }
        else {
            L2ManufactureList list = seller.getCreateList();
            writeD(list.size());

            for (L2ManufactureItem item : list.getList()) {
                writeD(item.getRecipeId());
                writeD(0x00);
                writeD(item.getCost());
            }
        }
    }
}