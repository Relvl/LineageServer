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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Broadcast;

import java.util.StringTokenizer;

/**
 * @author evill33t & squeezed, rework Tryskell
 */
public class L2WeddingManagerInstance extends L2NpcInstance {
    public L2WeddingManagerInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    /**
     * Are both partners wearing formal wear ? If Formal Wear check is disabled, returns True in any case.<BR>
     *
     * @param p1 L2PcInstance
     * @param p2 L2PcInstance
     * @return boolean
     */
    private static boolean wearsFormalWear(L2PcInstance p1, L2PcInstance p2) {
        L2ItemInstance fw1 = p1.getChestArmorInstance();
        if (fw1 == null || fw1.getItemId() != 6408) { return false; }

        L2ItemInstance fw2 = p2.getChestArmorInstance();
        if (fw2 == null || fw2.getItemId() != 6408) { return false; }

        return true;
    }

    public static void justMarried(L2PcInstance player, L2PcInstance ptarget) {
        // Unlock the wedding manager for both users, and set them as married
        player.setUnderMarryRequest(false);
        ptarget.setUnderMarryRequest(false);

        player.setMarried(true);
        ptarget.setMarried(true);

        // reduce adenas amount according to configs
        player.getInventory().reduceAdena(EItemProcessPurpose.WEDDING, Config.WEDDING_PRICE, player.getCurrentFolkNPC(), true);
        ptarget.getInventory().reduceAdena(EItemProcessPurpose.WEDDING, Config.WEDDING_PRICE, player.getCurrentFolkNPC(), true);

        // Flag players as married
        Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
        couple.marry();

        // Messages to the couple
        player.sendMessage("Congratulations, you are now married with " + ptarget.getName() + " !");
        ptarget.sendMessage("Congratulations, you are now married with " + player.getName() + " !");

        // Wedding march
        player.broadcastPacket(new MagicSkillUse(player, player, 2230, 1, 1, 0));
        ptarget.broadcastPacket(new MagicSkillUse(ptarget, ptarget, 2230, 1, 1, 0));

        // Fireworks
        L2Skill skill = FrequentSkill.LARGE_FIREWORK.getSkill();
        player.doCast(skill);
        ptarget.doCast(skill);

        Broadcast.announceToOnlinePlayers("Congratulations to " + player.getName() + " and " + ptarget.getName() + "! They have been married.");
    }

    @Override
    public void onAction(L2PcInstance player) {
        // Set the target of the L2PcInstance player
        if (player.getTarget() != this) { player.setTarget(this); }
        else {
            // Calculate the distance between the L2PcInstance and the L2Npc
            if (!canInteract(player)) { player.getAI().setIntention(EIntention.INTERACT, this); }
            else {
                // Rotate the player to face the instance
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

                // Send ActionFailed to the player in order to avoid he stucks
                player.sendPacket(ActionFailed.STATIC_PACKET);

                // Married people got access to another menu
                if (player.isMarried()) { sendHtmlMessage(player, "data/html/mods/Wedding_start2.htm"); }
                // "Under marriage acceptance" people go to this one
                else if (player.isUnderMarryRequest()) { sendHtmlMessage(player, "data/html/mods/Wedding_waitforpartner.htm"); }
                // And normal players go here :)
                else { sendHtmlMessage(player, "data/html/mods/Wedding_start.htm"); }
            }
        }
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("AskWedding")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();

            if (st.hasMoreTokens()) {
                final L2PcInstance ptarget = L2World.getInstance().getPlayer(st.nextToken());
                if (ptarget == null) {
                    sendHtmlMessage(player, "data/html/mods/Wedding_notfound.htm");
                    return;
                }

                // check conditions
                if (!weddingConditions(player, ptarget)) { return; }

                // block the wedding manager until an answer is given.
                player.setUnderMarryRequest(true);
                ptarget.setUnderMarryRequest(true);

                // memorize the requesterId for future use, and send a popup to the target
                ptarget.setRequesterId(player.getObjectId());
                ptarget.sendPacket(new ConfirmDlg(1983).addString(player.getName() + " asked you to marry. Do you want to start a new relationship ?"));
            }
            else { sendHtmlMessage(player, "data/html/mods/Wedding_notfound.htm"); }
        }
        else if (command.startsWith("Divorce")) {
            player.sendMessage("You are now divorced.");

            // Find the partner using the couple information
            final L2PcInstance partner = L2World.getInstance().getPlayer(Couple.getPartnerId(player.getObjectId()));
            if (partner != null) { partner.sendMessage("Your beloved has decided to divorce."); }

            CoupleManager.getInstance().deleteCouple(player.getCoupleId());
        }
        else if (command.startsWith("GoToLove")) {
            // Find the partner using the couple information
            final L2PcInstance partner = L2World.getInstance().getPlayer(Couple.getPartnerId(player.getObjectId()));
            if (partner == null) {
                player.sendMessage("Your partner is not online.");
                return;
            }

            // Simple checks to avoid exploits
            if (partner.isInJail() || partner.isInOlympiadMode() || partner.isInDuel() || partner.isFestivalParticipant() || (partner.isInParty() && partner.getParty().isInDimensionalRift()) || partner.isInObserverMode()) {
                player.sendMessage("Due to the current partner's status, the teleportation failed.");
                return;
            }

            if (partner.getClan() != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().isInProgress()) {
                player.sendMessage("As your partner is in siege, you can't go to him/her.");
                return;
            }

            // If all checks are successfully passed, teleport the player to the partner
            player.teleToLocation(partner.getX(), partner.getY(), partner.getZ(), 20);
        }
    }

    private boolean weddingConditions(L2PcInstance player, L2PcInstance ptarget) {
        // Check if player target himself
        if (ptarget.getObjectId() == player.getObjectId()) {
            sendHtmlMessage(player, "data/html/mods/Wedding_error_wrongtarget.htm");
            return false;
        }

        // Sex check
        if (ptarget.getAppearance().isFemale() == player.getAppearance().isFemale() && !Config.WEDDING_SAMESEX) {
            sendHtmlMessage(player, "data/html/mods/Wedding_error_sex.htm");
            return false;
        }

        // Check if player has the target on friendlist
        if (!player.getContactController().isFriend(ptarget.getObjectId())) {
            sendHtmlMessage(player, "data/html/mods/Wedding_error_friendlist.htm");
            return false;
        }

        // Target mustn't be already married
        if (ptarget.isMarried()) {
            sendHtmlMessage(player, "data/html/mods/Wedding_error_alreadymarried.htm");
            return false;
        }

        // Check for Formal Wear
        if (Config.WEDDING_FORMALWEAR) {
            if (!wearsFormalWear(player, ptarget)) {
                sendHtmlMessage(player, "data/html/mods/Wedding_error_noformal.htm");
                return false;
            }
        }

        // Check and reduce wedding price
        if (player.getAdena() < Config.WEDDING_PRICE || ptarget.getAdena() < Config.WEDDING_PRICE) {
            sendHtmlMessage(player, "data/html/mods/Wedding_error_adena.htm");
            return false;
        }

        return true;
    }

    private void sendHtmlMessage(L2PcInstance player, String file) {
        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(file);
        html.replace("%objectId%", getObjectId());
        html.replace("%adenasCost%", StringUtil.formatNumber(Config.WEDDING_PRICE));
        html.replace("%needOrNot%", Config.WEDDING_FORMALWEAR ? "will" : "won't");
        player.sendPacket(html);
    }
}