package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.AnnouncementTable;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.listenerbus.ListenerBus;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class EnterWorld extends L2GameClientPacket {

    private static void engage(L2PcInstance cha) {
        int objectId = cha.getObjectId();
        for (Couple cl : CoupleManager.getInstance().getCouples()) {
            if (cl.getPlayer1Id() == objectId || cl.getPlayer2Id() == objectId) {
                if (cl.getMaried()) {
                    cha.setMarried(true);
                }
                cha.setCoupleId(cl.getId());
            }
        }
    }

    private static void notifyClanMembers(L2PcInstance activeChar) {
        L2Clan clan = activeChar.getClan();
        clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
        SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addPcName(activeChar);
        PledgeShowMemberListUpdate update = new PledgeShowMemberListUpdate(activeChar);
        for (L2PcInstance member : clan.getOnlineMembers()) {
            if (member == activeChar) { continue; }
            member.sendPacket(msg);
            member.sendPacket(update);
        }
    }

    private static void notifySponsorOrApprentice(L2PcInstance activeChar) {
        if (activeChar.getSponsor() != 0) {
            L2PcInstance sponsor = L2World.getInstance().getPlayer(activeChar.getSponsor());
            if (sponsor != null) {
                sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addPcName(activeChar));
            }
        }
        else if (activeChar.getApprentice() != 0) {
            L2PcInstance apprentice = L2World.getInstance().getPlayer(activeChar.getApprentice());
            if (apprentice != null) {
                apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addPcName(activeChar));
            }
        }
    }

    private static void loadTutorial(L2PcInstance player) {
        QuestState qs = player.getQuestState("Tutorial");
        if (qs != null) { qs.getQuest().notifyEvent("UC", null, player); }
    }

    @Override
    protected void readImpl() {}

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            _log.warn("EnterWorld failed! activeChar is null...");
            getClient().closeNow();
            return;
        }

        if (player.isGM()) {
            GmListTable.getInstance().addGm(player, false);
        }

        // Set dead status if applies
        if (player.getCurrentHp() < 0.5) { player.setIsDead(true); }

        L2Clan clan = player.getClan();
        if (clan != null) {
            player.sendPacket(new PledgeSkillList(clan));
            notifyClanMembers(player);
            notifySponsorOrApprentice(player);

            // Add message at connexion if clanHall not paid.
            ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
            if (clanHall != null) {
                if (!clanHall.getPaid()) {
                    player.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
                }
            }

            for (Siege siege : SiegeManager.getSieges()) {
                if (!siege.isInProgress()) { continue; }

                if (siege.checkIsAttacker(clan)) { player.setSiegeState((byte) 1); }
                else if (siege.checkIsDefender(clan)) { player.setSiegeState((byte) 2); }
            }

            player.sendPacket(new PledgeShowMemberListAll(clan, 0));

            for (SubPledge sp : clan.getAllSubPledges()) {
                player.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
            }

            player.sendPacket(new UserInfo(player));
            player.sendPacket(new PledgeStatusChanged(clan));
        }

        // Updating Seal of Strife Buff/Debuff
        if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL) {
            int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
            if (cabal != SevenSigns.CABAL_NULL) {
                if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE)) {
                    player.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
                }
                else { player.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill()); }
            }
        }
        else {
            player.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
            player.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
        }

        if (Config.PLAYER_SPAWN_PROTECTION > 0) { player.setProtection(true); }

        player.spawnMe(player.getX(), player.getY(), player.getZ());

        engage(player);

        // Announcements, welcome & Seven signs period messages
        player.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
        SevenSigns.getInstance().sendCurrentPeriodMsg(player);
        AnnouncementTable.getInstance().showAnnouncements(player, false);

        if (player.getRace() == PlayerRace.DarkElf && player.getSkillLevel(294) == 1) {
            player.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ?
                                                             SystemMessageId.NIGHT_S1_EFFECT_APPLIES :
                                                             SystemMessageId.DAY_S1_EFFECT_DISAPPEARS
            ).addSkillName(294));
        }

        player.getMacroses().sendUpdate();
        player.sendPacket(new UserInfo(player));
        player.sendPacket(new HennaInfo(player));
        player.sendPacket(new FriendList(player));
        player.sendPacket(new ItemList(player, false));
        player.sendPacket(new ShortCutInit(player));
        player.sendPacket(new ExStorageMaxCount(player));
        player.updateEffectIcons();
        player.sendPacket(new EtcStatusUpdate(player));
        player.sendSkillList();

        Quest.playerEnterWorld(player);
        loadTutorial(player);
        player.sendPacket(new QuestList(player));

        if (clan != null && clan.isNoticeEnabled()) {
            NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/clan_notice.htm");
            html.replace("%clan_name%", clan.getName());
            html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
            sendPacket(html);
        }

        PetitionManager.getInstance().checkPetitionMessages(player);

        // no broadcast needed since the player will already spawn dead to others
        if (player.isAlikeDead()) { sendPacket(new Die(player)); }

        if (!player.variables().isTimeInPast(EPlayerVariableKey.CLAN_JOIN_EXPIRY_TIME)) {
            player.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
        }

        player.onPlayerEnter();
        ListenerBus.onPlayerEnter(player);

        if (Olympiad.getInstance().playerInStadia(player)) {
            player.teleToLocation(TeleportWhereType.Town);
        }
        if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false)) {
            DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
        }
        if (!player.isGM() && (!player.isInSiege() || player.getSiegeState() < 2) && player.isInsideZone(ZoneId.SIEGE)) {
            player.teleToLocation(TeleportWhereType.Town);
        }
    }

    @Override
    protected boolean triggersOnActionRequest() {
        return false;
    }
}