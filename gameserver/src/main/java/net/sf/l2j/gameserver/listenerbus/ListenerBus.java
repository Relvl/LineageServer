package net.sf.l2j.gameserver.listenerbus;

import net.sf.l2j.commons.WeakList;
import net.sf.l2j.gameserver.listenerbus.interfaces.*;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Шина событий.
 *
 * @author Johnson / 14.08.2017
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public final class ListenerBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerBus.class);

    private static final Map<Class<IListener>, WeakList<IListener>> MAPPING = new HashMap<>();

    private static final WeakList<OnPlayerEnterListener> ON_PLAYER_ENTER_LISTENERS = new WeakList<>(OnPlayerEnterListener.class);
    private static final WeakList<OnPlayerExitListener> ON_PLAYER_EXIT_LISTENERS = new WeakList<>(OnPlayerExitListener.class);
    private static final WeakList<OnPlayerDiedListener> ON_PLAYER_DIED_LISTENERS = new WeakList<>(OnPlayerDiedListener.class);
    private static final WeakList<OnPlayerRevivedListener> ON_PLAYER_REVIVED_LISTENERS = new WeakList<>(OnPlayerRevivedListener.class);

    static {
        for (Field field : ListenerBus.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (WeakList.class.isAssignableFrom(field.getType())) {
                try {
                    //noinspection unchecked,CastToConcreteClass
                    WeakList<IListener> list = (WeakList<IListener>) field.get(null);
                    MAPPING.put(list.getGenericType(), list);
                }
                catch (IllegalAccessException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    private ListenerBus() {}

    public static void addListener(IListener listener) {
        for (Entry<Class<IListener>, WeakList<IListener>> entry : MAPPING.entrySet()) {
            if (entry.getKey().isAssignableFrom(listener.getClass())) { entry.getValue().add(listener); }
        }
    }

    public static void onPlayerEnter(L2PcInstance player) {
        ON_PLAYER_ENTER_LISTENERS.clearReleased();
        for (OnPlayerEnterListener listener : ON_PLAYER_ENTER_LISTENERS) { listener.onPlayerEnter(player); }
    }

    public static void onPlayerExit(L2PcInstance player) {
        ON_PLAYER_EXIT_LISTENERS.clearReleased();
        for (OnPlayerExitListener listener : ON_PLAYER_EXIT_LISTENERS) { listener.onPlayerExit(player); }
    }

    public static void onPlayerDied(L2PcInstance player, L2Character killer) {
        ON_PLAYER_DIED_LISTENERS.clearReleased();
        for (OnPlayerDiedListener listener : ON_PLAYER_DIED_LISTENERS) { listener.onPlayerDied(player, killer); }
    }

    public static void onPlayerRevived(L2PcInstance player) {
        ON_PLAYER_REVIVED_LISTENERS.clearReleased();
        for (OnPlayerRevivedListener listener : ON_PLAYER_REVIVED_LISTENERS) { listener.onPlayerRevived(player); }
    }

}
