package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.Manager.MailBBSManager;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.AnnouncementTable;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class EnterWorld extends L2GameClientPacket {
    private static void engage(L2PcInstance cha) {
        int objectId = cha.getObjectId();
        for (Couple cl : CoupleManager.getInstance().getCouples()) {
            if (cl.getPlayer1Id() == objectId || cl.getPlayer2Id() == objectId) {
                if (cl.getMaried()) { cha.setMarried(true); }

                cha.setCoupleId(cl.getId());
            }
        }
    }

    private static void notifyClanMembers(L2PcInstance activeChar) {
        L2Clan clan = activeChar.getClan();
        clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);

        SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addPcName(activeChar);
        PledgeShowMemberListUpdate update = new PledgeShowMemberListUpdate(activeChar);

        // Send packet to others members.
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
    protected void readImpl() {
        // this is just a trigger packet. it has no content
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) {
            _log.warn("EnterWorld failed! activeChar is null...");
            getClient().closeNow();
            return;
        }

        if (activeChar.isGM()) {
            if (Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel())) {
                activeChar.setIsInvul(true);
            }

            if (Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_hide", activeChar.getAccessLevel())) {
                activeChar.getAppearance().setInvisible();
            }

            if (Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel())) {
                activeChar.setInRefusalMode(true);
            }

            if (Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmlist", activeChar.getAccessLevel())) {
                GmListTable.getInstance().addGm(activeChar, false);
            }
            else { GmListTable.getInstance().addGm(activeChar, true); }
        }

        // Set dead status if applies
        if (activeChar.getCurrentHp() < 0.5) { activeChar.setIsDead(true); }

        L2Clan clan = activeChar.getClan();
        if (clan != null) {
            activeChar.sendPacket(new PledgeSkillList(clan));
            notifyClanMembers(activeChar);
            notifySponsorOrApprentice(activeChar);

            // Add message at connexion if clanHall not paid.
            ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
            if (clanHall != null) {
                if (!clanHall.getPaid()) {
                    activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
                }
            }

            for (Siege siege : SiegeManager.getSieges()) {
                if (!siege.isInProgress()) { continue; }

                if (siege.checkIsAttacker(clan)) { activeChar.setSiegeState((byte) 1); }
                else if (siege.checkIsDefender(clan)) { activeChar.setSiegeState((byte) 2); }
            }

            activeChar.sendPacket(new PledgeShowMemberListAll(clan, 0));

            for (SubPledge sp : clan.getAllSubPledges()) {
                activeChar.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
            }

            activeChar.sendPacket(new UserInfo(activeChar));
            activeChar.sendPacket(new PledgeStatusChanged(clan));
        }

        // Updating Seal of Strife Buff/Debuff
        if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL) {
            int cabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
            if (cabal != SevenSigns.CABAL_NULL) {
                if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE)) {
                    activeChar.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
                }
                else { activeChar.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill()); }
            }
        }
        else {
            activeChar.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
            activeChar.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
        }

        if (Config.PLAYER_SPAWN_PROTECTION > 0) { activeChar.setProtection(true); }

        activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

        engage(activeChar);

        // Announcements, welcome & Seven signs period messages
        activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
        SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
        AnnouncementTable.getInstance().showAnnouncements(activeChar, false);

        // if player is DE, check for shadow sense skill at night
        if (activeChar.getRace() == Race.DarkElf && activeChar.getSkillLevel(294) == 1) {
            activeChar.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(294));
        }

        activeChar.getMacroses().sendUpdate();
        activeChar.sendPacket(new UserInfo(activeChar));
        activeChar.sendPacket(new HennaInfo(activeChar));
        activeChar.sendPacket(new FriendList(activeChar));
        // activeChar.queryGameGuard();
        activeChar.sendPacket(new ItemList(activeChar, false));
        activeChar.sendPacket(new ShortCutInit(activeChar));
        activeChar.sendPacket(new ExStorageMaxCount(activeChar));
        activeChar.updateEffectIcons();
        activeChar.sendPacket(new EtcStatusUpdate(activeChar));
        activeChar.sendSkillList();

        Quest.playerEnter(activeChar);
        if (!Config.DISABLE_TUTORIAL) { loadTutorial(activeChar); }

        for (Quest quest : ScriptManager.getInstance().getQuests()) {
            if (quest != null && quest.getOnEnterWorld()) { quest.notifyEnterWorld(activeChar); }
        }
        activeChar.sendPacket(new QuestList(activeChar));

        // Unread mails make a popup appears.
        if (MailBBSManager.getInstance().checkUnreadMail(activeChar) > 0) {
            activeChar.sendPacket(SystemMessageId.NEW_MAIL);
            activeChar.sendPacket(new PlaySound("systemmsg_e.1233"));
            activeChar.sendPacket(ExMailArrived.STATIC_PACKET);
        }

        // Clan notice, if active.
        if (clan != null && clan.isNoticeEnabled()) {
            NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/clan_notice.htm");
            html.replace("%clan_name%", clan.getName());
            html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
            sendPacket(html);
        }
        else if (Config.SERVER_NEWS) {
            NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/servnews.htm");
            sendPacket(html);
        }

        PetitionManager.getInstance().checkPetitionMessages(activeChar);

        // no broadcast needed since the player will already spawn dead to others
        if (activeChar.isAlikeDead()) { sendPacket(new Die(activeChar)); }

        activeChar.onPlayerEnter();

        // If player logs back in a stadium, port him in nearest town.
        if (Olympiad.getInstance().playerInStadia(activeChar)) {
            activeChar.teleToLocation(TeleportWhereType.Town);
        }

        if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false)) {
            DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
        }

        if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis()) {
            activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
        }

        // Attacker or spectator logging into a siege zone will be ported at town.
        if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE)) {
            activeChar.teleToLocation(TeleportWhereType.Town);
        }
    }

    @Override
    protected boolean triggersOnActionRequest() {
        return false;
    }
}