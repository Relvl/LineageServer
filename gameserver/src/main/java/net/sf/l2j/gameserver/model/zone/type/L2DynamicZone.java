package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

import java.util.concurrent.Future;

public class L2DynamicZone extends L2ZoneType {
    private final L2WorldRegion _region;
    private final L2Character _owner;
    private final L2Skill _skill;
    private Future<?> task;

    public L2DynamicZone(L2WorldRegion region, L2Character owner, L2Skill skill) {
        super(-1);
        _region = region;
        _owner = owner;
        _skill = skill;

        task = ThreadPoolManager.getInstance().scheduleGeneral(this::remove, skill.getBuffDuration());
    }

    protected void setTask(Future<?> task) {
        this.task = task;
    }

    @Override
    protected void onEnter(L2Character character) {
        try {
            if (character.isPlayer()) {
                character.sendMessage("You have entered a temporary zone!");
            }
            _skill.getEffects(_owner, character);
        }
        catch (NullPointerException e) {
            _log.warning(String.valueOf(e));
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            character.sendMessage("You have left a temporary zone!");
        }

        if (character == _owner) {
            remove();
            return;
        }
        character.stopSkillEffects(_skill.getId());
    }

    protected final void remove() {
        if (task == null) { return; }

        task.cancel(false);
        task = null;

        _region.removeZone(this);
        for (L2Character member : _characterList) {
            try {
                member.stopSkillEffects(_skill.getId());
            }
            catch (NullPointerException e) {
            }
        }
        _owner.stopSkillEffects(_skill.getId());

    }

    @Override
    public void onDieInside(L2Character character) {
        if (character == _owner) { remove(); }
        else { character.stopSkillEffects(_skill.getId()); }
    }

    @Override
    public void onReviveInside(L2Character character) {
        _skill.getEffects(_owner, character);
    }
}