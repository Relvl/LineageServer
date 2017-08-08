package net.sf.l2j.gameserver.communitybbs.module;

import net.sf.l2j.commons.ArgumentTokenizer;
import net.sf.l2j.gameserver.cache.HtmlCacheNew;
import net.sf.l2j.gameserver.communitybbs.ACommunityModule;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Johnson / 06.08.2017
 */
public class CommunityUnknown extends ACommunityModule {
    @Override
    public boolean handleCommand(L2PcInstance player, ArgumentTokenizer tokenizer) {
        CommunityBoard.showHtml(player, HtmlCacheNew.getInstance().getHtml("_community/404.html"), this);
        return false;
    }

    @Override
    public String getModuleTitle() { return null; }

    @Override
    public String getModuleKey() { return null; }
}
