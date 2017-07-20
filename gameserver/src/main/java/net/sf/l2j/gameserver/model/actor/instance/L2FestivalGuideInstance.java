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
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Party.MessageType;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

import java.util.Calendar;
import java.util.List;

/**
 * Festival of Darkness Guide (Seven Signs)
 *
 * @author Tempy
 */
public final class L2FestivalGuideInstance extends L2NpcInstance {
    protected int _festivalType;
    protected int _festivalOracle;
    protected int _blueStonesNeeded;
    protected int _greenStonesNeeded;
    protected int _redStonesNeeded;

    public L2FestivalGuideInstance(int objectId, NpcTemplate template) {
        super(objectId, template);

        switch (getNpcId()) {
            case 31127:
            case 31132:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
                _festivalOracle = SevenSigns.CABAL_DAWN;
                _blueStonesNeeded = 900;
                _greenStonesNeeded = 540;
                _redStonesNeeded = 270;
                break;
            case 31128:
            case 31133:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
                _festivalOracle = SevenSigns.CABAL_DAWN;
                _blueStonesNeeded = 1500;
                _greenStonesNeeded = 900;
                _redStonesNeeded = 450;
                break;
            case 31129:
            case 31134:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
                _festivalOracle = SevenSigns.CABAL_DAWN;
                _blueStonesNeeded = 3000;
                _greenStonesNeeded = 1800;
                _redStonesNeeded = 900;
                break;
            case 31130:
            case 31135:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
                _festivalOracle = SevenSigns.CABAL_DAWN;
                _blueStonesNeeded = 4500;
                _greenStonesNeeded = 2700;
                _redStonesNeeded = 1350;
                break;
            case 31131:
            case 31136:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
                _festivalOracle = SevenSigns.CABAL_DAWN;
                _blueStonesNeeded = 6000;
                _greenStonesNeeded = 3600;
                _redStonesNeeded = 1800;
                break;

            case 31137:
            case 31142:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
                _festivalOracle = SevenSigns.CABAL_DUSK;
                _blueStonesNeeded = 900;
                _greenStonesNeeded = 540;
                _redStonesNeeded = 270;
                break;
            case 31138:
            case 31143:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
                _festivalOracle = SevenSigns.CABAL_DUSK;
                _blueStonesNeeded = 1500;
                _greenStonesNeeded = 900;
                _redStonesNeeded = 450;
                break;
            case 31139:
            case 31144:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
                _festivalOracle = SevenSigns.CABAL_DUSK;
                _blueStonesNeeded = 3000;
                _greenStonesNeeded = 1800;
                _redStonesNeeded = 900;
                break;
            case 31140:
            case 31145:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
                _festivalOracle = SevenSigns.CABAL_DUSK;
                _blueStonesNeeded = 4500;
                _greenStonesNeeded = 2700;
                _redStonesNeeded = 1350;
                break;
            case 31141:
            case 31146:
                _festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
                _festivalOracle = SevenSigns.CABAL_DUSK;
                _blueStonesNeeded = 6000;
                _greenStonesNeeded = 3600;
                _redStonesNeeded = 1800;
                break;
        }
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("FestivalDesc")) {
            int val = Integer.parseInt(command.substring(13));

            showChatWindow(player, val, null, true);
        }
        else if (command.startsWith("Festival")) {
            L2Party playerParty = player.getParty();
            int val = Integer.parseInt(command.substring(9, 10));

            switch (val) {
                case 1: // Become a Participant
                    // Check if the festival period is active, if not then don't allow registration.
                    if (SevenSigns.getInstance().isSealValidationPeriod()) {
                        showChatWindow(player, 2, "a", false);
                        return;
                    }

                    // Check if a festival is in progress, then don't allow registration yet.
                    if (SevenSignsFestival.getInstance().isFestivalInitialized()) {
                        player.sendMessage("You cannot sign up while a festival is in progress.");
                        return;
                    }

                    // Check if the player is in a formed party already.
                    if (playerParty == null) {
                        showChatWindow(player, 2, "b", false);
                        return;
                    }

                    // Check if the player is the party leader.
                    if (!playerParty.isLeader(player)) {
                        showChatWindow(player, 2, "c", false);
                        return;
                    }

                    // Check to see if the party has at least 5 members.
                    if (playerParty.getMemberCount() < Config.ALT_FESTIVAL_MIN_PLAYER) {
                        showChatWindow(player, 2, "b", false);
                        return;
                    }

                    // Check if all the party members are in the required level range.
                    if (playerParty.getLevel() > SevenSignsFestival.getMaxLevelForFestival(_festivalType)) {
                        showChatWindow(player, 2, "d", false);
                        return;
                    }

                    // Check to see if the player has already signed up, if they are then update the participant list providing all the required criteria has been met.
                    if (player.isFestivalParticipant()) {
                        SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
                        showChatWindow(player, 2, "f", false);
                        return;
                    }

                    showChatWindow(player, 1, null, false);
                    break;
                case 2: // Festival 2 xxxx
                    int stoneType = Integer.parseInt(command.substring(11));
                    int stonesNeeded = 0;

                    switch (stoneType) {
                        case SevenSigns.SEAL_STONE_BLUE_ID:
                            stonesNeeded = _blueStonesNeeded;
                            break;
                        case SevenSigns.SEAL_STONE_GREEN_ID:
                            stonesNeeded = _greenStonesNeeded;
                            break;
                        case SevenSigns.SEAL_STONE_RED_ID:
                            stonesNeeded = _redStonesNeeded;
                            break;
                    }

                    if (!player.destroyItemByItemId(EItemProcessPurpose.SEVEN_SIGNS, stoneType, stonesNeeded, this, true)) { return; }

                    SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
                    SevenSignsFestival.getInstance().addAccumulatedBonus(_festivalType, stoneType, stonesNeeded);

                    showChatWindow(player, 2, "e", false);
                    break;
                case 3: // Score Registration
                    // Check if the festival period is active, if not then don't register the score.
                    if (SevenSigns.getInstance().isSealValidationPeriod()) {
                        showChatWindow(player, 3, "a", false);
                        return;
                    }

                    // Check if a festival is in progress, if it is don't register the score.
                    if (SevenSignsFestival.getInstance().isFestivalInProgress()) {
                        player.sendMessage("You cannot register a score while a festival is in progress.");
                        return;
                    }

                    // Check if the player is in a party.
                    if (playerParty == null) {
                        showChatWindow(player, 3, "b", false);
                        return;
                    }

                    List<Integer> prevParticipants = SevenSignsFestival.getInstance().getPreviousParticipants(_festivalOracle, _festivalType);

                    // Check if there are any past participants.
                    if ((prevParticipants == null) || prevParticipants.isEmpty() || !prevParticipants.contains(player.getObjectId())) {
                        showChatWindow(player, 3, "b", false);
                        return;
                    }

                    // Check if this player was the party leader in the festival.
                    if (player.getObjectId() != prevParticipants.get(0)) {
                        showChatWindow(player, 3, "b", false);
                        return;
                    }

                    L2ItemInstance bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);

                    // Check if the player collected any blood offerings during the festival.
                    if (bloodOfferings == null) {
                        player.sendMessage("You do not have any blood offerings to contribute.");
                        return;
                    }

                    int offeringScore = bloodOfferings.getCount() * SevenSignsFestival.FESTIVAL_OFFERING_VALUE;
                    if (!player.destroyItem(EItemProcessPurpose.SEVEN_SIGNS, bloodOfferings, this, false)) { return; }

                    boolean isHighestScore = SevenSignsFestival.getInstance().setFinalScore(player, _festivalOracle, _festivalType, offeringScore);

                    // Send message that the contribution score has increased.
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_S1).addNumber(offeringScore));

                    if (isHighestScore) { showChatWindow(player, 3, "c", false); }
                    else { showChatWindow(player, 3, "d", false); }
                    break;
                case 4: // Current High Scores
                    StringBuilder sb = new StringBuilder("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");

                    StatsSet dawnData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, _festivalType);
                    StatsSet duskData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, _festivalType);
                    StatsSet overallData = SevenSignsFestival.getInstance().getOverallHighestScoreData(_festivalType);

                    int dawnScore = dawnData.getInteger("score");
                    int duskScore = duskData.getInteger("score");

                    sb.append(SevenSignsFestival.getFestivalName(_festivalType) + " festival.<br>");

                    if (dawnScore > 0) { sb.append("Dawn: " + calculateDate(dawnData.getString("date")) + ". Score " + dawnScore + "<br>" + dawnData.getString("members") + "<br>"); }
                    else { sb.append("Dawn: No record exists. Score 0<br>"); }

                    if (duskScore > 0) { sb.append("Dusk: " + calculateDate(duskData.getString("date")) + ". Score " + duskScore + "<br>" + duskData.getString("members") + "<br>"); }
                    else { sb.append("Dusk: No record exists. Score 0<br>"); }

                    // If no data is returned, assume there is no record, or all scores are 0.
                    if (overallData != null) {
                        String cabalStr = "Children of Dusk";

                        if (overallData.getString("cabal").equals("dawn")) { cabalStr = "Children of Dawn"; }

                        sb.append("Consecutive top scores: " + calculateDate(overallData.getString("date")) + ". Score " + overallData.getInteger("score") + "<br>Affilated side: " + cabalStr + "<br>" + overallData.getString("members") + "<br>");
                    }
                    else { sb.append("Consecutive top scores: No record exists. Score 0<br>"); }

                    sb.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");

                    NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                    html.setHtml(sb.toString());
                    player.sendPacket(html);
                    break;
                case 8: // Increase the Festival Challenge
                    if (playerParty == null) { return; }

                    if (!SevenSignsFestival.getInstance().isFestivalInProgress()) { return; }

                    if (!playerParty.isLeader(player)) {
                        showChatWindow(player, 8, "a", false);
                        break;
                    }

                    if (SevenSignsFestival.getInstance().increaseChallenge(_festivalOracle, _festivalType)) { showChatWindow(player, 8, "b", false); }
                    else { showChatWindow(player, 8, "c", false); }
                    break;
                case 9: // Leave the Festival
                    if (playerParty == null) { return; }

                    /**
                     * If the player is the party leader, remove all participants from the festival (i.e. set the party to null, when updating the participant list) otherwise just remove this player from the "arena", and also remove them from the party.
                     */
                    boolean isLeader = playerParty.isLeader(player);

                    if (isLeader) {
                        SevenSignsFestival.getInstance().updateParticipants(player, null);
                    }
                    else {
                        SevenSignsFestival.getInstance().updateParticipants(player, playerParty);
                        playerParty.removePartyMember(player, MessageType.Expelled);
                    }
                    break;
                case 0: // Distribute Accumulated Bonus
                    if (!SevenSigns.getInstance().isSealValidationPeriod()) {
                        player.sendMessage("Bonuses cannot be paid during the competition period.");
                        return;
                    }

                    if (SevenSignsFestival.getInstance().distribAccumulatedBonus(player) > 0) { showChatWindow(player, 0, "a", false); }
                    else { showChatWindow(player, 0, "b", false); }
                    break;
                default:
                    showChatWindow(player, val, null, false);
            }
        }
        else {
            // this class dont know any other commands, let forward
            // the command to the parent class
            super.onBypassFeedback(player, command);
        }
    }

    @Override
    public void showChatWindow(L2PcInstance player, int val) {
        String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

        switch (getTemplate().getNpcId()) {
            case 31127: //
            case 31128: //
            case 31129: // Dawn Festival Guides
            case 31130: //
            case 31131: //
                filename += "festival/dawn_guide.htm";
                break;

            case 31137: //
            case 31138: //
            case 31139: // Dusk Festival Guides
            case 31140: //
            case 31141: //
                filename += "festival/dusk_guide.htm";
                break;

            case 31132: //
            case 31133: //
            case 31134: //
            case 31135: //
            case 31136: // Festival Witches
            case 31142: //
            case 31143: //
            case 31144: //
            case 31145: //
            case 31146: //
                filename += "festival/festival_witch.htm";
                break;
        }

        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", getObjectId());
        html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
        player.sendPacket(html);

        // Send ActionFailed to the player in order to avoid he stucks
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private void showChatWindow(L2PcInstance player, int val, String suffix, boolean isDescription) {
        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/" + ((isDescription) ? "desc_" : "festival_") + ((suffix != null) ? val + suffix : val) + ".htm");
        html.replace("%objectId%", getObjectId());
        html.replace("%festivalType%", SevenSignsFestival.getFestivalName(_festivalType));
        html.replace("%cycleMins%", SevenSignsFestival.getInstance().getMinsToNextCycle());
        if (!isDescription && "2b".equals(val + suffix)) { html.replace("%minFestivalPartyMembers%", Config.ALT_FESTIVAL_MIN_PLAYER); }

        // Festival's fee
        if (val == 1) {
            html.replace("%blueStoneNeeded%", _blueStonesNeeded);
            html.replace("%greenStoneNeeded%", _greenStonesNeeded);
            html.replace("%redStoneNeeded%", _redStonesNeeded);
        }
        // If the stats or bonus table is required, construct them.
        else if (val == 5) { html.replace("%statsTable%", getStatsTable()); }
        else if (val == 6) { html.replace("%bonusTable%", getBonusTable()); }

        player.sendPacket(html);

        // Send ActionFailed to the player in order to avoid he stucks
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private static final String getStatsTable() {
        StringBuilder tableHtml = new StringBuilder();

        // Get the scores for each of the festival level ranges (types).
        for (int i = 0; i < 5; i++) {
            int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
            int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
            String festivalName = SevenSignsFestival.getFestivalName(i);
            String winningCabal = "Children of Dusk";

            if (dawnScore > duskScore) { winningCabal = "Children of Dawn"; }
            else if (dawnScore == duskScore) { winningCabal = "None"; }

            tableHtml.append("<tr><td width=\"100\" align=\"center\">" + festivalName + "</td><td align=\"center\" width=\"35\">" + duskScore + "</td><td align=\"center\" width=\"35\">" + dawnScore + "</td><td align=\"center\" width=\"130\">" + winningCabal + "</td></tr>");
        }

        return tableHtml.toString();
    }

    private static final String getBonusTable() {
        StringBuilder tableHtml = new StringBuilder();

        // Get the accumulated scores for each of the festival level ranges (types).
        for (int i = 0; i < 5; i++) {
            int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
            String festivalName = SevenSignsFestival.getFestivalName(i);

            tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
        }

        return tableHtml.toString();
    }

    private static final String calculateDate(String milliFromEpoch) {
        long numMillis = Long.valueOf(milliFromEpoch);
        Calendar calCalc = Calendar.getInstance();

        calCalc.setTimeInMillis(numMillis);

        return calCalc.get(Calendar.YEAR) + "/" + calCalc.get(Calendar.MONTH) + "/" + calCalc.get(Calendar.DAY_OF_MONTH);
    }
}