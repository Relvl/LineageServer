package net.sf.l2j.gameserver.model.actor.instance.playerpart;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * —истема хранени€/загрузки переменных игрока.
 * ƒл€ хранени€ вс€кого редкоиспользуемого дерьма, на которое жалко тратить насто€щую переменную.
 *
 * @author Johnson / 20.07.2017
 */
public class PlayerVariables {
    private final L2PcInstance player;
    private Map<String, Variable> variables = new HashMap<>();

    public PlayerVariables(L2PcInstance player) {
        this.player = player;
        reload();
    }

    public L2PcInstance getPlayer() { return player; }

    public Boolean getBoolean(String name) { return getBoolean(name, null); }

    public Boolean getBoolean(String name, Boolean def) {
        Boolean value = variables.containsKey(name) ? variables.get(name).boolValue : def;
        return value == null ? def : value;
    }

    public String getString(String name) { return getString(name, null); }

    public String getString(String name, String def) {
        String value = variables.containsKey(name) ? variables.get(name).stringValue : def;
        return value == null ? def : value;
    }

    public Integer getInteger(String name) { return getInteger(name, null); }

    public Integer getInteger(String name, Integer def) {
        Integer value = variables.containsKey(name) ? variables.get(name).intValue : def;
        return value == null ? def : value;
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public void set(String name, String value) { setVariable(name, value, null, null); }

    public void set(String name, Boolean value) { setVariable(name, null, null, value); }

    public void set(String name, Integer value) { setVariable(name, null, value, null); }

    private void setVariable(String name, String stringValue, Integer intValue, Boolean boolValue) {

    }

    private void reload() {

    }

    private static final class Variable {
        private Integer intValue;
        private Boolean boolValue;
        private String stringValue;
    }
}
