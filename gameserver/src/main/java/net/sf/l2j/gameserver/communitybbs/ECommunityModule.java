package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.gameserver.communitybbs.module.CommunityAchievements;
import net.sf.l2j.gameserver.communitybbs.module.CommunityMain;
import net.sf.l2j.gameserver.communitybbs.module.CommunityUnknown;

/**
 * @author Johnson / 06.08.2017
 */
public enum ECommunityModule {
    UNKNOWN(new CommunityUnknown()),
    MAIN(new CommunityMain(), "_bbshome"),

    ACHIEVEMENTS(new CommunityAchievements(), "_friendlist_0_", CommunityAchievements.KEY);

    private final transient ACommunityModule module;
    private final String[] keys;

    ECommunityModule(ACommunityModule module, String... keys) {
        this.module = module;
        this.keys = keys;
    }

    public ACommunityModule getModule() {
        return module;
    }

    public static ACommunityModule getModule(String key) {
        if (key != null && !key.isEmpty()) {
            for (ECommunityModule module : values()) {
                if (module.keys != null) {
                    for (String moduleKey : module.keys) {
                        if (moduleKey.equalsIgnoreCase(key)) {
                            return module.module;
                        }
                    }
                }
            }
        }
        return UNKNOWN.module;
    }
}
