package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.commons.ArgumentTokenizer;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ShowBoard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class CommunityBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityBoard.class);
    /** Максимальное количество символов в чанке. TODO Разобраться, мне кажется тут нужно исходить из максимальной дины пакета... */
    private static final int HTML_CHUNK_SIZE = 4096; // 4090 ???
    /** Минимальное количество отправляемых чанков. */
    private static final int HTML_MIN_CHUNKS = 3;

    public static final Pattern PTN_CONTENT = Pattern.compile("%content%", Pattern.LITERAL);
    private static final Pattern PTN_BREADCRUMBS = Pattern.compile("%breadcrumbs%");
    private static final Pattern PTN_PAGES = Pattern.compile("%pages%");

    protected CommunityBoard() { }

    public static CommunityBoard getInstance() { return SingletonHolder.INSTANCE; }

    public static void handleCommand(L2PcInstance player, String commandFull) {
        LOGGER.info("CB#command: '{}'", commandFull);
        player.sendMessage("CB#command: " + commandFull);
        ArgumentTokenizer tokenizer = new ArgumentTokenizer(commandFull);
        ACommunityModule module = ECommunityModule.getModule(tokenizer.getNextArgument());
        if (module == null || !module.handleCommand(player, tokenizer)) {
            ECommunityModule.UNKNOWN.getModule().handleCommand(player, null);
        }
    }

    public void handleWriteCommand(L2PcInstance player, String url, String arg1, String arg2, String arg3, String arg4, String arg5) {
        LOGGER.info("CB#write: '{}' - '{}', '{}', '{}', '{}', '{}'", url, arg1, arg2, arg3, arg4, arg5);
    }

    /** Разбирает HTML на чанки и отправляет игроку. */
    public static void showHtml(L2PcInstance player, String html, ACommunityModule module) {
        if (html == null) {
            player.sendPacket(ShowBoard.CLOSEBOARD);
            return;
        }
        if (module != null) {
            html = PTN_BREADCRUMBS.matcher(html).replaceAll(module.makeBreadcrumbs());
            StringBuilder pagesSb = new StringBuilder();
            module.getPages((url, caption) -> {
                pagesSb.append(String.format("<a action=\"bypass %s\">&nbsp;%s&nbsp;</a>&nbsp;", url, caption));
            });
            if (pagesSb.length() > 0) {
                pagesSb.insert(0, "Страница:&nbsp;");
            }
            html = PTN_PAGES.matcher(html).replaceAll(pagesSb.toString());
        }
        else {
            html = PTN_BREADCRUMBS.matcher(html).replaceAll("");
            html = PTN_PAGES.matcher(html).replaceAll("");
        }

        int offset = 0;
        for (int i = 1; i <= Math.max(HTML_MIN_CHUNKS, Math.round(html.length() / HTML_CHUNK_SIZE)); i++) {
            player.sendPacket(new ShowBoard(
                    offset < html.length() ?
                    html.substring(offset, Math.min(html.length(), offset + HTML_CHUNK_SIZE)) :
                    null,
                    "10" + i
            ));
            offset += HTML_CHUNK_SIZE;
        }
    }

    private static final class SingletonHolder {
        private static final CommunityBoard INSTANCE = new CommunityBoard();
    }
}