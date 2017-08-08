package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.commons.ArgumentTokenizer;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.function.BiConsumer;

/**
 * @author Johnson / 06.08.2017
 */
public abstract class ACommunityModule {
    /**  */
    public abstract boolean handleCommand(L2PcInstance player, ArgumentTokenizer tokenizer);

    /**  */
    public abstract String getModuleTitle();

    public abstract String getModuleKey();

    public String makeBreadcrumbs() {
        StringBuilder sb = new StringBuilder("<a action=\"bypass _bbshome\">Главная</a>");
        if (getModuleKey() != null && !getModuleKey().equals("_bbshome")) {
            sb.append("&nbsp;»&nbsp;").append("<a action=\"bypass ").append(getModuleKey()).append("\">").append(getModuleTitle()).append("</a>");
        }
        return sb.toString();
    }

    public void getPages(BiConsumer<String, String> consumer) {}
}
