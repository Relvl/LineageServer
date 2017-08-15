package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.location.Location;

public final class RequestRestartPoint extends L2GameClientPacket {
    protected int _requestedPointType;
    protected boolean _continuation;

    @Override
    protected void readImpl() {
        _requestedPointType = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (activeChar.isFakeDeath()) {
            activeChar.stopFakeDeath(true);
            return;
        }

        if (!activeChar.isDead()) {
            _log.warn("Living player [{}] called RequestRestartPoint packet.", activeChar.getName());
            return;
        }

        Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
        if (castle != null && castle.getSiege().isInProgress()) {
            if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan())) {
                // Schedule respawn delay for attacker
                ThreadPoolManager.getInstance().schedule(new DeathTask(activeChar), SiegeManager.ATTACKERS_RESPAWN_DELAY);

                if (SiegeManager.ATTACKERS_RESPAWN_DELAY > 0) {
                    activeChar.sendMessage("You will be teleported in " + SiegeManager.ATTACKERS_RESPAWN_DELAY / 1000 + " seconds.");
                }

                return;
            }
        }

        // run immediately (no need to schedule)
        new DeathTask(activeChar).run();
    }

    class DeathTask implements Runnable {
        final L2PcInstance activeChar;

        DeathTask(L2PcInstance _activeChar) {
            activeChar = _activeChar;
        }

        @Override
        public void run() {
            Location loc = null;
            Castle castle = null;

            // force
            if (activeChar.isInJail()) { _requestedPointType = 27; }
            else if (activeChar.isFestivalParticipant()) { _requestedPointType = 4; }

            switch (_requestedPointType) {
                case 1: // to clanhall
                    if (activeChar.getClan() == null || !activeChar.getClan().hasHideout()) {
                        _log.warn("{} called RestartPointPacket - To Clanhall while he doesn't have clan / Clanhall.", activeChar.getName());
                        return;
                    }

                    loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClanHall);

                    if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null) {
                        activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
                    }
                    break;

                case 2: // to castle
                    castle = CastleManager.getInstance().getCastle(activeChar);

                    if (castle != null && castle.getSiege().isInProgress()) {
                        // Siege in progress
                        if (castle.getSiege().checkIsDefender(activeChar.getClan())) {
                            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
                        }
                        // Just in case you lost castle while being dead.. Port to nearest Town.
                        else if (castle.getSiege().checkIsAttacker(activeChar.getClan())) {
                            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
                        }
                        else {
                            _log.warn("{} called RestartPointPacket - To Castle while he doesn't have Castle.", activeChar.getName());
                            return;
                        }
                    }
                    else {
                        if (activeChar.getClan() == null || !activeChar.getClan().hasCastle()) { return; }

                        loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
                    }
                    break;

                case 3: // to siege HQ
                    castle = CastleManager.getInstance().getCastle(activeChar);

                    L2SiegeClan siegeClan = null;
                    if (castle != null && castle.getSiege().isInProgress()) {
                        siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
                    }

                    // Not a normal scenario.
                    if (siegeClan == null) {
                        _log.warn("{} called RestartPointPacket - To Siege HQ while he doesn't have Siege HQ.", activeChar.getName());
                        return;
                    }

                    // If a player was waiting with flag option and then the flag dies before the
                    // player pushes the button, he is send back to closest/second closest town.
                    if (siegeClan.getFlags().isEmpty()) {
                        loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
                    }
                    else {
                        loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.SiegeFlag);
                    }
                    break;

                case 4: // Fixed or player is a festival participant
                    if (!activeChar.isGM() && !activeChar.isFestivalParticipant()) {
                        _log.warn("{} called RestartPointPacket - Fixed while he isn't festival participant!", activeChar.getName());
                        return;
                    }
                    loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
                    break;

                case 27: // to jail
                    if (!activeChar.isInJail()) { return; }
                    loc = new Location(-114356, -249645, -2984);
                    break;

                default:
                    loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
                    break;
            }

            // Teleport and revive
            activeChar.setIsIn7sDungeon(false);

            if (activeChar.isDead()) { activeChar.doRevive(); }

            activeChar.teleToLocation(loc, 20);
        }
    }
}