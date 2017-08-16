package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PetitionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetitionManager.class);

    private final Map<Integer, Petition> _pendingPetitions;
    private final Map<Integer, Petition> _completedPetitions;

    protected PetitionManager() {
        _pendingPetitions = new HashMap<>();
        _completedPetitions = new HashMap<>();
    }

    public static PetitionManager getInstance() {
        return SingletonHolder._instance;
    }

    public static boolean isPetitioningAllowed() {
        return Config.PETITIONING_ALLOWED;
    }

    public void clearCompletedPetitions() {
        int numPetitions = getPendingPetitionCount();

        getCompletedPetitions().clear();
        LOGGER.info("PetitionManager: Completed petition data cleared. {} petition(s) removed.", numPetitions);
    }

    public void clearPendingPetitions() {
        int numPetitions = getPendingPetitionCount();

        getPendingPetitions().clear();
        LOGGER.info("PetitionManager: Pending petition queue cleared. {} petition(s) removed.", numPetitions);
    }

    public boolean acceptPetition(L2PcInstance respondingAdmin, int petitionId) {
        if (!isValidPetition(petitionId)) { return false; }

        Petition currPetition = getPendingPetitions().get(petitionId);

        if (currPetition.getResponder() != null) { return false; }

        currPetition.setResponder(respondingAdmin);
        currPetition.setState(PetitionState.In_Process);

        // Petition application accepted. (Send to Petitioner)
        currPetition.sendPetitionerPacket(SystemMessage.getSystemMessage(SystemMessageId.PETITION_APP_ACCEPTED));

        // Petition application accepted. Reciept No. is <ID>
        currPetition.sendResponderPacket(SystemMessage.getSystemMessage(SystemMessageId.PETITION_ACCEPTED_RECENT_NO_S1).addNumber(currPetition.getId()));

        // Petition consultation with <Player> underway.
        currPetition.sendResponderPacket(SystemMessage.getSystemMessage(SystemMessageId.PETITION_WITH_S1_UNDER_WAY).addPcName(currPetition.getPetitioner()));
        return true;
    }

    public boolean cancelActivePetition(L2PcInstance player) {
        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId()) {
                return currPetition.endPetitionConsultation(PetitionState.Petitioner_Cancel);
            }

            if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId()) {
                return currPetition.endPetitionConsultation(PetitionState.Responder_Cancel);
            }
        }

        return false;
    }

    public void checkPetitionMessages(L2PcInstance petitioner) {
        if (petitioner != null) {
            for (Petition currPetition : getPendingPetitions().values()) {
                if (currPetition == null) { continue; }

                if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId()) {
                    for (CreatureSay logMessage : currPetition.getLogMessages()) { petitioner.sendPacket(logMessage); }

                    return;
                }
            }
        }
    }

    public boolean endActivePetition(L2PcInstance player) {
        if (!player.isGM()) { return false; }

        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition == null) { continue; }

            if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId()) {
                return currPetition.endPetitionConsultation(PetitionState.Completed);
            }
        }

        return false;
    }

    protected Map<Integer, Petition> getCompletedPetitions() {
        return _completedPetitions;
    }

    protected Map<Integer, Petition> getPendingPetitions() {
        return _pendingPetitions;
    }

    public int getPendingPetitionCount() {
        return getPendingPetitions().size();
    }

    public int getPlayerTotalPetitionCount(L2PcInstance player) {
        if (player == null) { return 0; }

        int petitionCount = 0;

        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition == null) { continue; }

            if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId()) {
                petitionCount++;
            }
        }

        for (Petition currPetition : getCompletedPetitions().values()) {
            if (currPetition == null) { continue; }

            if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId()) {
                petitionCount++;
            }
        }

        return petitionCount;
    }

    public boolean isPetitionInProcess() {
        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition == null) { continue; }

            if (currPetition.getState() == PetitionState.In_Process) { return true; }
        }

        return false;
    }

    public boolean isPetitionInProcess(int petitionId) {
        if (!isValidPetition(petitionId)) { return false; }

        Petition currPetition = getPendingPetitions().get(petitionId);
        return currPetition.getState() == PetitionState.In_Process;
    }

    public boolean isPlayerInConsultation(L2PcInstance player) {
        if (player != null) {
            for (Petition currPetition : getPendingPetitions().values()) {
                if (currPetition == null) { continue; }

                if (currPetition.getState() != PetitionState.In_Process) { continue; }

                if ((currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId()) || (currPetition.getResponder() != null && currPetition.getResponder()
                                                                                                                                                                                        .getObjectId() == player.getObjectId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isPlayerPetitionPending(L2PcInstance petitioner) {
        if (petitioner != null) {
            for (Petition currPetition : getPendingPetitions().values()) {
                if (currPetition == null) { continue; }

                if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValidPetition(int petitionId) {
        return getPendingPetitions().containsKey(petitionId);
    }

    public boolean rejectPetition(L2PcInstance respondingAdmin, int petitionId) {
        if (!isValidPetition(petitionId)) { return false; }

        Petition currPetition = getPendingPetitions().get(petitionId);

        if (currPetition.getResponder() != null) { return false; }

        currPetition.setResponder(respondingAdmin);
        return currPetition.endPetitionConsultation(PetitionState.Responder_Reject);
    }

    public boolean sendActivePetitionMessage(L2PcInstance player, String messageText) {
        CreatureSay cs;

        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition == null) { continue; }

            if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId()) {
                cs = new CreatureSay(player.getObjectId(), EChatType.PETITION_PLAYER, player.getName(), messageText);
                currPetition.addLogMessage(cs);

                currPetition.sendResponderPacket(cs);
                currPetition.sendPetitionerPacket(cs);
                return true;
            }

            if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId()) {
                cs = new CreatureSay(player.getObjectId(), EChatType.PETITION_GM, player.getName(), messageText);
                currPetition.addLogMessage(cs);

                currPetition.sendResponderPacket(cs);
                currPetition.sendPetitionerPacket(cs);
                return true;
            }
        }

        return false;
    }

    public void sendPendingPetitionList(L2PcInstance activeChar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder("<html><body><center><font color=\"LEVEL\">Current Petitions</font><br><table width=\"300\">");

        if (getPendingPetitionCount() == 0) {
            sb.append("<tr><td colspan=\"4\">There are no currently pending petitions.</td></tr>");
        }
        else {
            sb.append("<tr><td></td><td><font color=\"999999\">Petitioner</font></td><td><font color=\"999999\">Petition Type</font></td><td><font color=\"999999\">Submitted</font></td></tr>");
        }

        for (Petition currPetition : getPendingPetitions().values()) {
            if (currPetition == null) { continue; }

            sb.append("<tr><td>");

            if (currPetition.getState() != PetitionState.In_Process) {
                StringUtil.append(sb, "<button value=\"View\" action=\"bypass -h admin_view_petition ", currPetition.getId(), "\" width=\"40\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\">");
            }
            else { sb.append("<font color=\"999999\">In Process</font>"); }

            StringUtil.append(sb, "</td><td>", currPetition.getPetitioner().getName(), "</td><td>", currPetition.getTypeAsString(), "</td><td>", sdf.format(currPetition.getSubmitTime()), "</td></tr>");
        }

        sb.append("</table><br><button value=\"Refresh\" action=\"bypass -h admin_view_petitions\" width=\"50\" " + "height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br><button value=\"Back\" action=\"bypass -h admin_admin\" " + "width=\"40\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        activeChar.sendPacket(html);
    }

    public int submitPetition(L2PcInstance petitioner, String petitionText, int petitionType) {
        // Create a new petition instance and add it to the list of pending petitions.
        Petition newPetition = new Petition(petitioner, petitionText, petitionType);
        int newPetitionId = newPetition.getId();
        getPendingPetitions().put(newPetitionId, newPetition);

        // Notify all GMs that a new petition has been submitted.
        String msgContent = petitioner.getName() + " has submitted a new petition."; // (ID: " + newPetitionId + ").";
        GmListTable.broadcastToGMs(new CreatureSay(petitioner.getObjectId(), EChatType.HERO_VOICE, "Petition System", msgContent));

        return newPetitionId;
    }

    public void viewPetition(L2PcInstance activeChar, int petitionId) {
        if (!activeChar.isGM()) { return; }

        if (!isValidPetition(petitionId)) { return; }

        Petition currPetition = getPendingPetitions().get(petitionId);
        StringBuilder sb = new StringBuilder("<html><body>");

        sb.append("<center><br><font color=\"LEVEL\">Petition #" + currPetition.getId() + "</font><br1>");
        sb.append("<img src=\"L2UI.SquareGray\" width=\"200\" height=\"1\"></center><br>");
        sb.append("Submit Time: " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(currPetition.getSubmitTime()) + "<br1>");
        sb.append("Petitioner: " + currPetition.getPetitioner().getName() + "<br1>");
        sb.append("Petition Type: " + currPetition.getTypeAsString() + "<br>" + currPetition.getContent() + "<br>");
        sb.append("<center><button value=\"Accept\" action=\"bypass -h admin_accept_petition " + currPetition.getId() + "\"" + "width=\"50\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br1>");
        sb.append("<button value=\"Reject\" action=\"bypass -h admin_reject_petition " + currPetition.getId() + "\" " + "width=\"50\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        sb.append("<button value=\"Back\" action=\"bypass -h admin_view_petitions\" width=\"40\" height=\"15\" back=\"sek.cbui94\" " + "fore=\"sek.cbui92\"></center>");
        sb.append("</body></html>");

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        activeChar.sendPacket(html);
    }

    private enum PetitionState {
        Pending,
        Responder_Cancel,
        Responder_Missing,
        Responder_Reject,
        Responder_Complete,
        Petitioner_Cancel,
        Petitioner_Missing,
        In_Process,
        Completed
    }

    private enum PetitionType {
        Immobility,
        Recovery_Related,
        Bug_Report,
        Quest_Related,
        Bad_User,
        Suggestions,
        Game_Tip,
        Operation_Related,
        Other
    }

    private static class SingletonHolder {
        protected static final PetitionManager _instance = new PetitionManager();
    }

    private class Petition {
        private final long _submitTime = System.currentTimeMillis();

        private final int _id;
        private final PetitionType _type;
        private final String _content;
        private final List<CreatureSay> _messageLog = new ArrayList<>();
        private final L2PcInstance _petitioner;
        private PetitionState _state = PetitionState.Pending;
        private L2PcInstance _responder;

        public Petition(L2PcInstance petitioner, String petitionText, int petitionType) {
            petitionType--;
            _id = IdFactory.getInstance().getNextId();
            if (petitionType >= PetitionType.values().length) {
                LOGGER.warn("PetitionManager: invalid petition type (received type was +1) : {}", petitionType);
            }

            _type = PetitionType.values()[petitionType];
            _content = petitionText;
            _petitioner = petitioner;
        }

        protected boolean addLogMessage(CreatureSay cs) {
            return _messageLog.add(cs);
        }

        protected List<CreatureSay> getLogMessages() {
            return _messageLog;
        }

        public boolean endPetitionConsultation(PetitionState endState) {
            setState(endState);

            if (getResponder() != null && getResponder().isOnline()) {
                if (endState == PetitionState.Responder_Reject) {
                    getPetitioner().sendMessage("Your petition was rejected. Please try again later.");
                }
                else {
                    // Ending petition consultation with <Player>.
                    getResponder().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PETITION_ENDED_WITH_S1).addPcName(getPetitioner()));

                    // Receipt No. <ID> petition cancelled.
                    if (endState == PetitionState.Petitioner_Cancel) {
                        getResponder().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RECENT_NO_S1_CANCELED).addNumber(getId()));
                    }
                }
            }

            // End petition consultation and inform them, if they are still online.
            if (getPetitioner() != null && getPetitioner().isOnline()) {
                getPetitioner().sendPacket(SystemMessageId.THIS_END_THE_PETITION_PLEASE_PROVIDE_FEEDBACK);
            }

            getCompletedPetitions().put(getId(), this);
            return getPendingPetitions().remove(getId()) != null;
        }

        public String getContent() {
            return _content;
        }

        public int getId() {
            return _id;
        }

        public L2PcInstance getPetitioner() {
            return _petitioner;
        }

        public L2PcInstance getResponder() {
            return _responder;
        }

        public void setResponder(L2PcInstance respondingAdmin) {
            if (getResponder() != null) { return; }

            _responder = respondingAdmin;
        }

        public long getSubmitTime() {
            return _submitTime;
        }

        public PetitionState getState() {
            return _state;
        }

        public void setState(PetitionState state) {
            _state = state;
        }

        public String getTypeAsString() {
            return _type.toString().replace("_", " ");
        }

        public void sendPetitionerPacket(L2GameServerPacket responsePacket) {
            if (getPetitioner() == null || !getPetitioner().isOnline()) { return; }

            getPetitioner().sendPacket(responsePacket);
        }

        public void sendResponderPacket(L2GameServerPacket responsePacket) {
            if (getResponder() == null || !getResponder().isOnline()) {
                endPetitionConsultation(PetitionState.Responder_Missing);
                return;
            }

            getResponder().sendPacket(responsePacket);
        }
    }
}