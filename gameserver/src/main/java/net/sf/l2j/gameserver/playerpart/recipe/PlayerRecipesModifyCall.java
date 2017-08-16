package net.sf.l2j.gameserver.playerpart.recipe;

import net.sf.l2j.commons.database.EModify;
import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 22.07.2017
 */
public class PlayerRecipesModifyCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRecipesModifyCall.class);

    @OrmParamIn(1)
    private final Integer playerId;
    @OrmParamIn(2)
    private Integer recipeId;
    @OrmParamIn(3)
    private Integer modify;
    @OrmParamOut(4)
    private Integer resultCode;

    protected PlayerRecipesModifyCall(Integer playerId) {
        super("player_recipes_modify", 4, false);
        this.playerId = playerId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public void setModify(EModify modify) {
        this.modify = modify.getMod();
    }

    @Override
    public Integer getResultCode() { return resultCode; }

    @Override
    protected boolean throwErrorOnRecultCode() { return true; }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    public String toString() {
        return "PlayerRecipesModifyCall{" +
                "playerId=" + playerId +
                ", recipeId=" + recipeId +
                ", modify=" + modify +
                ", resultCode=" + resultCode +
                '}';
    }
}
