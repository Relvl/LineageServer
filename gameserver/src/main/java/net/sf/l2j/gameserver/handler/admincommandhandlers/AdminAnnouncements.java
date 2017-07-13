package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.datatables.AnnouncementTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AdminAnnouncements implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS = {
            "admin_announce",
            "admin_ann",
            "admin_say"
    };

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.startsWith("admin_announce")) {
            final String[] tokens = command.split(" ", 3);
            switch (tokens[1]) {
                case "list":
                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                case "all":
                    for (L2PcInstance player : L2World.getInstance().getPlayers()) {
                        AnnouncementTable.getInstance().showAnnouncements(player, false);
                    }

                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                case "all_auto":
                    for (L2PcInstance player : L2World.getInstance().getPlayers()) {
                        AnnouncementTable.getInstance().showAnnouncements(player, true);
                    }

                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                case "add":
                    try {
                        final String[] split = tokens[2].split(" ", 2); // boolean string
                        final boolean crit = Boolean.parseBoolean(split[0]);

                        if (!AnnouncementTable.getInstance().addAnnouncement(split[1], crit, false, -1, -1, -1)) {
                            activeChar.sendMessage("Invalid //announce message content ; can't be null or empty.");
                        }
                    } catch (Exception e) {
                    }
                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                case "add_auto":
                    try {
                        final String[] split = tokens[2].split(" ", 6); // boolean boolean int int int string
                        final boolean crit = Boolean.parseBoolean(split[0]);
                        final boolean auto = Boolean.parseBoolean(split[1]);
                        final int idelay = Integer.parseInt(split[2]);
                        final int delay = Integer.parseInt(split[3]);
                        final int limit = Integer.parseInt(split[4]);
                        final String msg = split[5];

                        if (!AnnouncementTable.getInstance().addAnnouncement(msg, crit, auto, idelay, delay, limit)) {
                            activeChar.sendMessage("Invalid //announce message content ; can't be null or empty.");
                        }
                    } catch (Exception e) {
                    }
                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                case "del":
                    try {
                        AnnouncementTable.getInstance().delAnnouncement(Integer.parseInt(tokens[2]));
                    } catch (Exception e) {
                    }
                    AnnouncementTable.getInstance().listAnnouncements(activeChar);
                    break;

                default:
                    activeChar.sendMessage("Possible //announce parameters : <list|all|add|add_auto|del>");
                    return false;
            }
        }
        else if (command.startsWith("admin_ann") || command.startsWith("admin_say")) {
            AnnouncementTable.handleAnnounce(command, 10, command.startsWith("admin_say"));
        }

        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}