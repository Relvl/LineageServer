package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClanTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanTable.class);

    private final Map<Integer, L2Clan> _clans;

    public static ClanTable getInstance() {
        return SingletonHolder._instance;
    }

    public L2Clan[] getClans() {
        return _clans.values().toArray(new L2Clan[_clans.size()]);
    }

    protected ClanTable() {
        _clans = new HashMap<>();

        // Load all clans.
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM clan_data");
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                int clanId = result.getInt("clan_id");

                L2Clan clan = new L2Clan(clanId, result.getInt("leader_id"));
                _clans.put(clanId, clan);

                clan.setName(result.getString("clan_name"));
                clan.setLevel(result.getInt("clan_level"));
                clan.setCastle(result.getInt("hasCastle"));
                clan.setAllyId(result.getInt("ally_id"));
                clan.setAllyName(result.getString("ally_name"));

                // If ally expire time has been reached while server was off, keep it to 0.
                long allyExpireTime = result.getLong("ally_penalty_expiry_time");
                if (allyExpireTime > System.currentTimeMillis()) { clan.setAllyPenaltyExpiryTime(allyExpireTime, result.getInt("ally_penalty_type")); }

                // If character expire time has been reached while server was off, keep it to 0.
                long charExpireTime = result.getLong("char_penalty_expiry_time");
                if (charExpireTime + Config.ALT_CLAN_JOIN_DAYS * 86400000L > System.currentTimeMillis()) { clan.setCharPenaltyExpiryTime(charExpireTime); }

                clan.setDissolvingExpiryTime(result.getLong("dissolving_expiry_time"));

                clan.setCrestId(result.getInt("crest_id"));
                clan.setCrestLargeId(result.getInt("crest_large_id"));
                clan.setAllyCrestId(result.getInt("ally_crest_id"));

                clan.addReputationScore(result.getInt("reputation_score"));
                clan.setAuctionBiddedAt(result.getInt("auction_bid_at"));

                if (clan.getDissolvingExpiryTime() != 0) { scheduleRemoveClan(clan); }

                clan.setNoticeEnabled(result.getBoolean("enabled"));
                clan.setNotice(result.getString("notice"));

                clan.setIntroduction(result.getString("introduction"), false);
            }
            result.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("ClanTable: Error restoring ClanTable: ", e);
        }
        LOGGER.info("ClanTable: Restored {} clans.", _clans.size());

        // Check for non-existing alliances.
        allianceCheck();

        // Restore clan wars.
        restoreWars();

        // Refresh clans ladder.
        refreshClansLadder(false);
    }

    /**
     * @param clanId : The id of the clan to retrieve.
     * @return the clan object based on id.
     */
    public L2Clan getClan(int clanId) {
        return _clans.get(clanId);
    }

    public L2Clan getClanByName(String clanName) {
        for (L2Clan clan : _clans.values()) {
            if (clan.getName().equalsIgnoreCase(clanName)) { return clan; }
        }
        return null;
    }

    /**
     * Creates a new clan and store clan info to database
     *
     * @param player   The player who requested the clan creation.
     * @param clanName The name of the clan player wants.
     * @return null if checks fail, or L2Clan
     */
    public L2Clan createClan(L2PcInstance player, String clanName) {
        if (player == null) { return null; }

        if (player.getLevel() < 10) {
            player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN);
            return null;
        }

        if (player.getClanId() != 0) {
            player.sendPacket(SystemMessageId.FAILED_TO_CREATE_CLAN);
            return null;
        }

        if (!player.variables().isTimeInPast(EPlayerVariableKey.CLAN_CREATE_EXPIRY_TIME)) {
            player.sendPacket(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN);
            return null;
        }

        if (!StringUtil.isAlphaNumeric(clanName)) {
            player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
            return null;
        }

        if (clanName.length() < 2 || clanName.length() > 16) {
            player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
            return null;
        }

        if (getClanByName(clanName) != null) {
            // clan name is already taken
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
            return null;
        }

        L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
        L2ClanMember leader = new L2ClanMember(clan, player);
        clan.setLeader(leader);
        leader.setPlayerInstance(player);
        clan.store();
        player.setClan(clan);
        player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));
        player.setClanPrivileges(L2Clan.CP_ALL);

        _clans.put(clan.getClanId(), clan);

        player.sendPacket(new PledgeShowMemberListAll(clan, 0));
        player.sendPacket(new UserInfo(player));
        player.sendPacket(SystemMessageId.CLAN_CREATED);
        return clan;
    }

    public synchronized void destroyClan(int clanId) {
        L2Clan clan = _clans.get(clanId);
        if (clan == null) { return; }

        clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));

        int castleId = clan.getCastleId();
        if (castleId == 0) {
            for (Siege siege : SiegeManager.getSieges()) { siege.removeSiegeClan(clan); }
        }

        // Drop all items from clan warehouse.
        clan.getWarehouse().destroyAllItems(EItemProcessPurpose.CLAN_REMOVE, (clan.getLeader() == null) ? null : clan.getLeader().getPlayerInstance());

        for (L2ClanMember member : clan.getMembers()) {
            clan.removeClanMember(member.getObjectId(), 0);
        }

        _clans.remove(clanId);
        IdFactory.getInstance().releaseId(clanId);

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
            statement.setInt(1, clanId);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
            statement.setInt(1, clanId);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
            statement.setInt(1, clanId);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
            statement.setInt(1, clanId);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
            statement.setInt(1, clanId);
            statement.setInt(2, clanId);
            statement.execute();
            statement.close();

            if (castleId != 0) {
                statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
                statement.setInt(1, castleId);
                statement.execute();
                statement.close();
            }
        }
        catch (Exception e) {
            LOGGER.error("Error removing clan from DB.", e);
        }
    }

    public void scheduleRemoveClan(L2Clan clan) {
        if (clan == null) { return; }

        ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (clan.getDissolvingExpiryTime() != 0) { destroyClan(clan.getClanId()); }
            }
        }, Math.max(clan.getDissolvingExpiryTime() - System.currentTimeMillis(), 60000));
    }

    public boolean isAllyExists(String allyName) {
        for (L2Clan clan : _clans.values()) {
            if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName)) { return true; }
        }
        return false;
    }

    public void storeClansWars(int clanId1, int clanId2) {
        L2Clan clan1 = _clans.get(clanId1);
        L2Clan clan2 = _clans.get(clanId2);

        clan1.setEnemyClan(clanId2);
        clan1.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan1), SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP).addString(clan2.getName()));

        clan2.setAttackerClan(clanId1);
        clan2.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan2), SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR).addString(clan1.getName()));

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement;
            statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2) VALUES(?,?)");
            statement.setInt(1, clanId1);
            statement.setInt(2, clanId2);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error storing clan wars data.", e);
        }
    }

    public void deleteClansWars(int clanId1, int clanId2) {
        L2Clan clan1 = _clans.get(clanId1);
        L2Clan clan2 = _clans.get(clanId2);

        clan1.deleteEnemyClan(clanId2);
        clan1.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan1), SystemMessage.getSystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED).addString(clan2.getName()));

        clan2.deleteAttackerClan(clanId1);
        clan2.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan2), SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP).addString(clan1.getName()));

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement;

            if (Config.ALT_CLAN_WAR_PENALTY_WHEN_ENDED > 0) {
                long penaltyExpiryTime = System.currentTimeMillis() + Config.ALT_CLAN_WAR_PENALTY_WHEN_ENDED * 86400000L;

                clan1.addWarPenaltyTime(clanId2, penaltyExpiryTime);

                statement = con.prepareStatement("UPDATE clan_wars SET expiry_time=? WHERE clan1=? AND clan2=?");
                statement.setLong(1, penaltyExpiryTime);
                statement.setInt(2, clanId1);
                statement.setInt(3, clanId2);
            }
            else {
                statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
                statement.setInt(1, clanId1);
                statement.setInt(2, clanId2);
            }
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error removing clan wars data.", e);
        }
    }

    public void checkSurrender(L2Clan clan1, L2Clan clan2) {
        int count = 0;
        for (L2ClanMember player : clan1.getMembers()) {
            if (player != null && player.getPlayerInstance().variables().getBoolean(EPlayerVariableKey.WANTS_PEACE)) {
                count++;
            }
        }
        if (count == clan1.getMembersCount() - 1) {
            clan1.deleteEnemyClan(clan2.getClanId());
            clan2.deleteEnemyClan(clan1.getClanId());
            deleteClansWars(clan1.getClanId(), clan2.getClanId());
        }
    }

    private void restoreWars() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            // Delete deprecated wars (server was offline).
            PreparedStatement statement = con.prepareStatement("DELETE FROM clan_wars WHERE expiry_time > 0 AND expiry_time <= ?");
            statement.setLong(1, System.currentTimeMillis());
            statement.execute();
            statement.close();

            // Load all wars.
            statement = con.prepareStatement("SELECT * FROM clan_wars");
            ResultSet rset = statement.executeQuery();
            while (rset.next()) {
                int clan1 = rset.getInt("clan1");
                int clan2 = rset.getInt("clan2");
                long expiryTime = rset.getLong("expiry_time");

                // Expiry timer is found, add a penalty. Otherwise, add the regular war.
                if (expiryTime > 0) { _clans.get(clan1).addWarPenaltyTime(clan2, expiryTime); }
                else {
                    _clans.get(clan1).setEnemyClan(clan2);
                    _clans.get(clan2).setAttackerClan(clan1);
                }
            }
            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error restoring clan wars data.", e);
        }
    }

    /**
     * Check for nonexistent alliances
     */
    private void allianceCheck() {
        for (L2Clan clan : _clans.values()) {
            int allyId = clan.getAllyId();
            if (allyId != 0 && clan.getClanId() != allyId) {
                if (!_clans.containsKey(allyId)) {
                    clan.setAllyId(0);
                    clan.setAllyName(null);
                    clan.changeAllyCrest(0, true);
                    clan.updateClanInDB();
                    LOGGER.info("{}: Removed alliance from clan: {}", getClass().getSimpleName(), clan);
                }
            }
        }
    }

    public List<L2Clan> getClanAllies(int allianceId) {
        List<L2Clan> clanAllies = new ArrayList<>();
        if (allianceId != 0) {
            for (L2Clan clan : _clans.values()) {
                if (clan != null && clan.getAllyId() == allianceId) { clanAllies.add(clan); }
            }
        }
        return clanAllies;
    }

    /**
     * Refresh clans ladder, picking up the 99 first best clans, and allocating their ranks accordingly.
     *
     * @param cleanupRank if true, cleanup ranks. Used for the task, useless for startup.
     */
    public void refreshClansLadder(boolean cleanupRank) {
        // Cleanup ranks. Needed, as one clan can go off the list.
        if (cleanupRank) {
            for (L2Clan clan : _clans.values()) { if (clan != null && clan.getRank() != 0) { clan.setRank(0); } }
        }

        // Retrieve the 99 best clans, allocate their ranks.
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM clan_data ORDER BY reputation_score DESC LIMIT 99");
            ResultSet result = statement.executeQuery();

            int rank = 1;

            while (result.next()) {
                L2Clan clan = _clans.get(result.getInt("clan_id"));
                if (clan != null && clan.getReputationScore() > 0) { clan.setRank(rank++); }
            }
            result.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error updating clans ladder.", e);
        }
    }

    private static class SingletonHolder {
        protected static final ClanTable _instance = new ClanTable();
    }
}