package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class L2SkillTeleport extends L2Skill {
    private final String _recallType;
    private final Location _loc;

    public L2SkillTeleport(StatsSet set) {
        super(set);

        _recallType = set.getString("recallType", "");
        String coords = set.getString("teleCoords", null);
        if (coords != null) {
            String[] valuesSplit = coords.split(",");
            _loc = new Location(Integer.parseInt(valuesSplit[0]), Integer.parseInt(valuesSplit[1]), Integer.parseInt(valuesSplit[2]));
        }
        else { _loc = null; }
    }

    @Override
    public void useSkill(L2Character activeChar, L2Object[] targets) {
        if (activeChar instanceof L2PcInstance) {
            // Check invalid states.
            if (activeChar.isAfraid() || ((L2PcInstance) activeChar).isInOlympiadMode() || GrandBossManager.getInstance().isInBossZone(activeChar)) { return; }
        }

        boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            final L2Character target = ((L2Character) obj);

            if (target instanceof L2PcInstance) {
                L2PcInstance targetChar = (L2PcInstance) target;

                // Check invalid states.
                if (targetChar.isFestivalParticipant() || targetChar.isInJail() || targetChar.isInDuel()) { continue; }

                if (targetChar != activeChar) {
                    if (targetChar.isInOlympiadMode()) { continue; }

                    if (GrandBossManager.getInstance().isInBossZone(targetChar)) { continue; }
                }
            }

            Location loc = null;
            if (getSkillType() == L2SkillType.TELEPORT) {
                if (_loc != null) {
                    if (!(target instanceof L2PcInstance) || !target.isFlying()) { loc = _loc; }
                }
            }
            else {
                if (_recallType.equalsIgnoreCase("Castle")) { loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.Castle); }
                else if (_recallType.equalsIgnoreCase("ClanHall")) { loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.ClanHall); }
                else { loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.Town); }
            }

            if (loc != null) {
                if (target instanceof L2PcInstance) { ((L2PcInstance) target).setIsIn7sDungeon(false); }

                target.teleToLocation(loc, 20);
            }
        }

        activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }
}