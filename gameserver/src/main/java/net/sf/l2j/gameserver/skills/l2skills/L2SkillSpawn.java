/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillSpawn extends L2Skill {
    private final int _npcId;
    private final int _despawnDelay;
    private final boolean _summonSpawn;
    private final boolean _randomOffset;

    public L2SkillSpawn(StatsSet set) {
        super(set);
        _npcId = set.getInteger("npcId", 0);
        _despawnDelay = set.getInteger("despawnDelay", 0);
        _summonSpawn = set.getBool("isSummonSpawn", false);
        _randomOffset = set.getBool("randomOffset", true);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        if (caster.isAlikeDead()) { return; }

        if (_npcId == 0) {
            LOGGER.warn("NPC ID not defined for skill ID: {}", getId());
            return;
        }

        NpcTemplate template = NpcTable.getInstance().getTemplate(_npcId);
        if (template == null) {
            LOGGER.warn("Spawn of the nonexisting NPC ID: {}, skill ID: {}", _npcId, getId());
            return;
        }

        try {
            L2Spawn spawn = new L2Spawn(template);
            spawn.setHeading(-1);

            if (_randomOffset) {
                spawn.setLocx(caster.getX() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)));
                spawn.setLocy(caster.getY() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)));
            }
            else {
                spawn.setLocx(caster.getX());
                spawn.setLocy(caster.getY());
            }
            spawn.setLocz(caster.getZ() + 20);

            spawn.stopRespawn();
            L2Npc npc = spawn.doSpawn(_summonSpawn);

            if (_despawnDelay > 0) { npc.scheduleDespawn(_despawnDelay); }
        }
        catch (Exception e) {
            LOGGER.error("Exception while spawning NPC ID: {}, skill ID: {}, exception: {}", _npcId, getId(), e.getMessage(), e);
        }
    }
}