package net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 22.07.2017
 */
public class PlayerRecipesLoadCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRecipesLoadCall.class);

    @OrmParamIn(1)
    private final Integer playerId;
    @OrmParamOut(value = 2, cursorClass = RecipeRow.class)
    private final List<RecipeRow> recipes = new ArrayList<>();
    @OrmParamOut(3)
    private Integer resultCode;

    protected PlayerRecipesLoadCall(Integer playerId) {
        super("game_server.player_recipes_load", 3, false);
        this.playerId = playerId;
    }

    public List<RecipeRow> getRecipes() { return recipes; }

    @Override
    public Integer getResultCode() { return resultCode; }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    protected boolean throwErrorOnRecultCode() { return true; }

    @Override
    public String toString() {
        return "PlayerRecipesLoadCall{" +
                "playerId=" + playerId +
                ", recipes=" + recipes +
                ", resultCode=" + resultCode +
                '}';
    }

    public static final class RecipeRow {
        @OrmParamCursor("PLAYER_ID")
        private Integer playerId;
        @OrmParamCursor("RECIPE_ID")
        private Integer recipeId;

        public Integer getPlayerId() { return playerId; }

        public Integer getRecipeId() { return recipeId; }

        @Override
        public String toString() {
            return "RecipeRow{" +
                    "playerId=" + playerId +
                    ", recipeId=" + recipeId +
                    '}';
        }
    }
}
