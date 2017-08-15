package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.StringTokenizer;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>gm = turns gm mode off for a short period of time (by default 1 minute).</li>
 * </ul>
 */
public class AdminGm implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_gm"
            };

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.startsWith("admin_gm")) {
            if (activeChar.isGM()) {
                StringTokenizer st = new StringTokenizer(command, " ");
                st.nextToken();

                int numberOfMinutes = 1;
                if (st.hasMoreTokens()) {
                    try {
                        numberOfMinutes = Integer.parseInt(st.nextToken());
                    }
                    catch (Exception e) {
                        activeChar.sendMessage("Invalid timer setted for //gm ; default time is used.");
                    }
                }

                // We keep the previous level to rehabilitate it later.
                final int previousAccessLevel = activeChar.getAccessLevel().getLevel();

                GmListTable.getInstance().deleteGm(activeChar);
                activeChar.setAccessLevel(AccessLevels.USER_ACCESS_LEVEL_NUMBER);
                activeChar.sendMessage("You no longer have GM status, but will be rehabilitated after " + numberOfMinutes + " minutes.");

                ThreadPoolManager.getInstance().schedule(new GiveBackAccess(activeChar, previousAccessLevel), numberOfMinutes * 60000);
            }
        }
        return true;
    }

    private class GiveBackAccess implements Runnable {
        private final L2PcInstance _activeChar;
        private final int _previousAccessLevel;

        public GiveBackAccess(L2PcInstance activeChar, int previousAccessLevel) {
            _activeChar = activeChar;
            _previousAccessLevel = previousAccessLevel;
        }

        @Override
        public void run() {
            if (!_activeChar.isOnline()) { return; }

            GmListTable.getInstance().addGm(_activeChar, false);
            _activeChar.setAccessLevel(_previousAccessLevel);
            _activeChar.sendMessage("Your previous access level has been rehabilitated.");
        }
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}