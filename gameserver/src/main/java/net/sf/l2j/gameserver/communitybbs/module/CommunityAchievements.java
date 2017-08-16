package net.sf.l2j.gameserver.communitybbs.module;

import net.sf.l2j.commons.ArgumentTokenizer;
import net.sf.l2j.gameserver.cache.HtmlCacheNew;
import net.sf.l2j.gameserver.communitybbs.ACommunityModule;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.playerpart.achievements.EAchievementGroup;
import net.sf.l2j.gameserver.playerpart.achievements.IAchieveElement;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Johnson / 07.08.2017
 */
public class CommunityAchievements extends ACommunityModule {
    public static final String KEY = "_bbs_achievements";

    private static final int ACHIEVEMENTS_PER_PAGE = 6;

    @Override
    public boolean handleCommand(L2PcInstance player, ArgumentTokenizer tokenizer) {
        String html = HtmlCacheNew.getInstance().getHtml("_community/index.html");
        String command = tokenizer.getNextArgument();
        int page = 1;

        switch (command) {
            // Показать дешборд (группы) на указанной странице --> _bbs_achievements {P}
            case "":
                page = Integer.parseInt(tokenizer.getNextArgument().isEmpty() ? "1" : tokenizer.getLastArgument());
                if (page <= 0) { return false; }

                String achiGroupTemplate = HtmlCacheNew.getInstance().getHtml("_community/achievements/achievement_group.htm");
                html = CommunityBoard.PTN_CONTENT.matcher(html).replaceAll(
                        HtmlCacheNew.getInstance().getHtml("_community/achievements/achievement_groups.htm")
                );
                html = CommunityBoard.PTN_CONTENT.matcher(html).replaceAll(
                        Stream.of(EAchievementGroup.values())
                              .map(g -> String.format(achiGroupTemplate,
                                      g.getIcon(),
                                      g.getTitle(),
                                      g.name(),
                                      g.getDescription(),
                                      EAchievementGroup.getAllAchievements().filter(a -> a.getGroup() == g).count(),
                                      EAchievementGroup.getAllAchievements().filter(a -> a.getGroup() == g && player.getAchievementController().hasAchievement(a)).count()
                              ))
                              .collect(Collectors.joining(""))
                );

                CommunityBoard.showHtml(player, html, this);
                return true;
            // Показать группу -> _bbs_achievements group {XXX} {P}
            case "group":
                EAchievementGroup group = EAchievementGroup.getGroup(tokenizer.getNextArgument());
                if (group == null) { return false; }
                page = Integer.parseInt(tokenizer.getNextArgument().isEmpty() ? "1" : tokenizer.getLastArgument());
                if (page <= 0) { return false; }

                List<IAchieveElement> achievements = group.getAchievements()
                                                          .sorted((a1, a2) -> {
                                                              if (player.getAchievementController().hasAchievement(a1)) { return Integer.MIN_VALUE; }
                                                              if (player.getAchievementController().hasAchievement(a2)) { return Integer.MAX_VALUE; }
                                                              return player.getAchievementController().getAchievementPartialCount(a2)
                                                                           .compareTo(player.getAchievementController().getAchievementPartialCount(a1));
                                                          })
                                                          .collect(Collectors.toList());
                int idxFirst = (page - 1) * ACHIEVEMENTS_PER_PAGE;
                int idxLast = Math.min((page * ACHIEVEMENTS_PER_PAGE) - 1, achievements.size() - 1);
                if (idxFirst > achievements.size()) { return false; }

                String achiElementTemplate = HtmlCacheNew.getInstance().getHtml("_community/achievements/achievement_dashboard_element.htm");
                html = CommunityBoard.PTN_CONTENT.matcher(html).replaceAll(
                        HtmlCacheNew.getInstance().getHtml("_community/achievements/dashboard.htm")
                );
                html = CommunityBoard.PTN_CONTENT.matcher(html).replaceAll(
                        achievements.subList(idxFirst, idxLast + 1).stream()
                                    .map(achi -> {

                                        boolean completed = player.getAchievementController().hasAchievement(achi);
                                        int partial = player.getAchievementController().getAchievementPartialCount(achi);
                                        return String.format(
                                                achiElementTemplate,
                                                achi.getId(), // %1$s - ID для ссылки
                                                String.format("%s&nbsp;[%s%s]",
                                                        achi.title(),
                                                        completed ? "завершено" : "",
                                                        partial > 0 ? partial + "/" + achi.getCount() : ""
                                                ), // %2$s - Текст ссылки
                                                completed ? "ffff00" : partial > 0 ? "8fbc8f" : "888888",
                                                String.format(achi.description(), achi.getCount())
                                        );
                                    })
                                    .collect(Collectors.joining("")) //
                );

                CommunityBoard.showHtml(player, html, this);
                return true;

            // Показать подробности ачивки
            case "details":
                IAchieveElement achieve = EAchievementGroup.getAchievement(tokenizer.getNextArgument());
                if (achieve == null) { return false; }

                System.out.println(">>> ACHI: " + achieve.title());

                CommunityBoard.showHtml(player, html, this);
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getModuleTitle() { return "Достижения"; }

    @Override
    public String getModuleKey() { return KEY; }

    @Override
    public void getPages(BiConsumer<String, String> consumer) {
        IntStream.rangeClosed(1, (int) Math.ceil(EAchievementGroup.getAllAchievements().count() / (double) ACHIEVEMENTS_PER_PAGE))
                 .mapToObj(String::valueOf)
                 .forEach(s -> consumer.accept("_bbs_achievements page " + s, s));
    }
}
