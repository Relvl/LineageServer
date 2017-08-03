package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.client.game_to_client.EtcStatusUpdate;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class L2EffectZone extends L2ZoneType {
    protected Map<Integer, Integer> _skills = new ConcurrentHashMap<>();
    private int _chance;
    private int _initialDelay;
    private int _reuse;
    private boolean _enabled;
    private boolean _isShowDangerIcon;
    private Future<?> _task;
    private int _minLvl;
    private String _target = "L2Playable"; // default only playable

    public L2EffectZone(int id) {
        super(id);

        _chance = 100;
        _initialDelay = 0;
        _reuse = 30000;
        _enabled = true;
        _isShowDangerIcon = true;
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("chance")) { _chance = Integer.parseInt(value); }
        else if (name.equals("initialDelay")) { _initialDelay = Integer.parseInt(value); }
        else if (name.equals("defaultStatus")) { _enabled = Boolean.parseBoolean(value); }
        else if (name.equals("reuse")) { _reuse = Integer.parseInt(value); }
        else if (name.equals("skillIdLvl")) {
            String[] propertySplit = value.split(";");
            for (String skill : propertySplit) {
                String[] skillSplit = skill.split("-");
                if (skillSplit.length != 2) {
                    LOGGER.warn(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skill + "\"");
                }
                else {
                    try {
                        _skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
                    }
                    catch (NumberFormatException nfe) {
                        if (!skill.isEmpty()) {
                            LOGGER.warn(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skillSplit[0] + "\"" + skillSplit[1]);
                        }
                    }
                }
            }
        }
        else if (name.equals("showDangerIcon")) { _isShowDangerIcon = Boolean.parseBoolean(value); }
        else if (name.equals("affectedLvlMin")) { _minLvl = Integer.parseInt(value); }
        else if (name.equals("targetClass")) { _target = value; }
        else { super.setParameter(name, value); }
    }

    @Override
    protected boolean isAffected(L2Character character) {
        // Check lvl
        if (character.getLevel() < _minLvl) { return false; }

        // check obj class
        try {
            if (!(Class.forName("net.sf.l2j.gameserver.model.actor." + _target).isInstance(character))) { return false; }
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    protected void onEnter(L2Character character) {
        if (_task == null) {
            synchronized (this) {
                if (_task == null) { _task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(), _initialDelay, _reuse); }
            }
        }

        if (character instanceof L2PcInstance && _isShowDangerIcon) {
            character.setInsideZone(ZoneId.DANGER_AREA, true);
            character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character instanceof L2PcInstance && _isShowDangerIcon) {
            character.setInsideZone(ZoneId.DANGER_AREA, false);
            if (!character.isInsideZone(ZoneId.DANGER_AREA)) { character.sendPacket(new EtcStatusUpdate((L2PcInstance) character)); }
        }

        if (_characterList.isEmpty() && _task != null) {
            _task.cancel(true);
            _task = null;
        }
    }

    public int getChance() {
        return _chance;
    }

    public boolean isEnabled() {
        return _enabled;
    }

    public void setEnabled(boolean val) {
        _enabled = val;
    }

    public void addSkill(int skillId, int skillLvL) {
        _skills.put(skillId, (skillLvL < 1) ? 1 : skillLvL);
    }

    public void removeSkill(int skillId) {
        _skills.remove(skillId);
    }

    public void clearSkills() {
        _skills.clear();
    }

    public int getSkillLevel(int skillId) {
        if (!_skills.containsKey(skillId)) { return 0; }

        return _skills.get(skillId);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    class ApplySkill implements Runnable {
        ApplySkill() {
        }

        @Override
        public void run() {
            if (isEnabled()) {
                for (L2Character temp : getCharactersInside()) {
                    if (temp != null && !temp.isDead()) {
                        if (Rnd.get(100) < getChance()) {
                            for (Entry<Integer, Integer> e : _skills.entrySet()) {
                                L2Skill skill = SkillTable.getInfo(e.getKey(), e.getValue());
                                if (skill != null && skill.checkCondition(temp, temp, false)) { if (temp.getFirstEffect(e.getKey()) == null) { skill.getEffects(temp, temp); } }
                            }
                        }
                    }
                }
            }
        }
    }
}