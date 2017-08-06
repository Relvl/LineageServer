package net.sf.l2j.gameserver.playerpart.variables;

import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;

/**
 * Система хранения/загрузки переменных игрока.
 * Для хранения всякого редкоиспользуемого дерьма, на которое жалко тратить настоящую переменную.
 *
 * @author Johnson / 20.07.2017
 */
public class PlayerVariablesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVariablesController.class);

    private final L2PcInstance player;
    private final EnumMap<EPlayerVariableKey, PlayerVariable> variables = new EnumMap<>(EPlayerVariableKey.class);

    public PlayerVariablesController(L2PcInstance player) {
        this.player = player;
        reload();
    }

    public L2PcInstance getPlayer() { return player; }

    public Boolean getBoolean(EPlayerVariableKey key) { return getBoolean(key, null); }

    public Boolean getBoolean(EPlayerVariableKey key, Boolean def) {
        Boolean value = variables.containsKey(key) ? variables.get(key).getBoolValue() : def;
        return value == null ? def : value;
    }

    public String getString(EPlayerVariableKey key) { return getString(key, null); }

    public String getString(EPlayerVariableKey key, String def) {
        String value = variables.containsKey(key) ? variables.get(key).getStringValue() : def;
        return value == null ? def : value;
    }

    public Integer getInteger(EPlayerVariableKey key) { return getInteger(key, null); }

    public Integer getInteger(EPlayerVariableKey key, Integer def) {
        Integer value = variables.containsKey(key) ? variables.get(key).getIntValue() : def;
        return value == null ? def : value;
    }

    public Long getLong(EPlayerVariableKey key) { return getLong(key, null); }

    public Long getLong(EPlayerVariableKey key, Long def) {
        Long value = variables.containsKey(key) ? variables.get(key).getLongValue() : def;
        return value == null ? def : value;
    }

    public LocalDateTime getLocalDateTime(EPlayerVariableKey key) {
        return variables.containsKey(key) ? LocalDateTime.ofEpochSecond(variables.get(key).getLongValue(), 0, ZoneOffset.ofTotalSeconds(0)) : null;
    }

    public boolean isTimeInPast(EPlayerVariableKey key) {
        return variables.containsKey(key) ? getLocalDateTime(key).isBefore(LocalDateTime.now()) : true;
    }

    public boolean hasVariable(EPlayerVariableKey key) {
        return variables.containsKey(key) && variables.get(key) != null;
    }

    public void set(EPlayerVariableKey key, String value) { setVariable(key, value, null, null, null); }

    public void set(EPlayerVariableKey key, Boolean value) { setVariable(key, null, null, value, null); }

    public void set(EPlayerVariableKey key, Integer value) { setVariable(key, null, value, null, null); }

    public void set(EPlayerVariableKey key, Long value) { setVariable(key, null, null, null, value); }

    public void set(EPlayerVariableKey key, LocalDateTime value) { setVariable(key, null, null, null, value.toEpochSecond(ZoneOffset.ofTotalSeconds(0))); }

    public void remove(EPlayerVariableKey key) { setVariable(key, null, null, null, null); }

    private void setVariable(EPlayerVariableKey key, String stringValue, Integer intValue, Boolean boolValue, Long longValue) {
        try (StorePlayerVariableCall call = new StorePlayerVariableCall(player.getObjectId(), key.name(), stringValue, intValue, boolValue, longValue)) {
            call.execute();
            if (call.getResultCode() == 0) {
                if (intValue != null || stringValue != null || boolValue != null || longValue != null) {
                    PlayerVariable variable = new PlayerVariable(key.name(), intValue, boolValue, stringValue, longValue);
                    variables.put(key, variable);
                }
                else {
                    variables.remove(key);
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot store player variable '{}' for player {}", key, player, e);
        }
    }

    private void reload() {
        try (GetPlayerVariablesCall call = new GetPlayerVariablesCall(player.getObjectId())) {
            call.execute();
            if (call.getResultCode() == 0) {
                variables.clear();
                for (PlayerVariable variable : call.getPlayerVariables()) {
                    variables.put(EPlayerVariableKey.valueOf(variable.getName().toUpperCase()), variable);
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot load variables for player {}", player, e);
        }
    }

    @Override
    public String toString() {
        return "PlayerVariablesController{" +
                "player=" + player +
                ", variables=" + variables +
                '}';
    }
}
