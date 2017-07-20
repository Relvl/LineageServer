package net.sf.l2j.gameserver.model.actor.instance.playerpart.variables;

import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * —истема хранени€/загрузки переменных игрока.
 * ƒл€ хранени€ вс€кого редкоиспользуемого дерьма, на которое жалко тратить насто€щую переменную.
 *
 * @author Johnson / 20.07.2017
 */
public class PlayerVariables {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVariables.class);

    private final L2PcInstance player;
    private final Map<String, PlayerVariable> variables = new ConcurrentHashMap<>();

    public PlayerVariables(L2PcInstance player) {
        this.player = player;
        reload();
    }

    public L2PcInstance getPlayer() { return player; }

    public Boolean getBoolean(String name) { return getBoolean(name, null); }

    public Boolean getBoolean(String name, Boolean def) {
        Boolean value = variables.containsKey(name) ? variables.get(name).getBoolValue() : def;
        return value == null ? def : value;
    }

    public String getString(String name) { return getString(name, null); }

    public String getString(String name, String def) {
        String value = variables.containsKey(name) ? variables.get(name).getStringValue() : def;
        return value == null ? def : value;
    }

    public Integer getInteger(String name) { return getInteger(name, null); }

    public Integer getInteger(String name, Integer def) {
        Integer value = variables.containsKey(name) ? variables.get(name).getIntValue() : def;
        return value == null ? def : value;
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public void set(String name, String value) { setVariable(name, value, null, null); }

    public void set(String name, Boolean value) { setVariable(name, null, null, value); }

    public void set(String name, Integer value) { setVariable(name, null, value, null); }

    public void remove(String name) { setVariable(name, null, null, null); }

    private void setVariable(String name, String stringValue, Integer intValue, Boolean boolValue) {
        try (StorePlayerVariableCall call = new StorePlayerVariableCall(player.getObjectId(), name, stringValue, intValue, boolValue)) {
            call.execute();
            if (call.getResultCode() == 0) {
                PlayerVariable variable = new PlayerVariable(name, intValue, boolValue, stringValue);
                variables.put(name, variable);
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot store player variable '{}' for player {}", name, player, e);
        }
    }

    private void reload() {
        try (GetPlayerVariablesCall call = new GetPlayerVariablesCall(player.getObjectId())) {
            call.execute();
            if (call.getResultCode() == 0) {
                variables.clear();
                for (PlayerVariable variable : call.getPlayerVariables()) {
                    variables.put(variable.getName(), variable);
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot load variables for player {}", player, e);
        }
    }

    @Override
    public String toString() {
        return "PlayerVariables{" +
                "player=" + player +
                ", variables=" + variables +
                '}';
    }
}
