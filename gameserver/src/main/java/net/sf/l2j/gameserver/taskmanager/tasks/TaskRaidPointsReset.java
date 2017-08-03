package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;

public final class TaskRaidPointsReset extends ATask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRaidPointsReset.class);

    private static final String NAME = "raid_points_reset";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initializate() {
        TaskManager.addUniqueTask(NAME, TaskType.TYPE_GLOBAL_TASK, "1", "00:10:00", "", 0);
    }

    @Override
    public void onTimeElapsed(ExecutedTask task) {
        Calendar cal = Calendar.getInstance();

        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            // reward clan reputation points
            Map<Integer, Integer> rankList = RaidBossPointsManager.getInstance().getRankList();
            for (L2Clan c : ClanTable.getInstance().getClans()) {
                for (Map.Entry<Integer, Integer> entry : rankList.entrySet()) {
                    if (entry.getValue() <= 100 && c.isMember(entry.getKey())) {
                        int reputation = 0;
                        switch (entry.getValue()) {
                            case 1:
                                reputation = 1250;
                                break;
                            case 2:
                                reputation = 900;
                                break;
                            case 3:
                                reputation = 700;
                                break;
                            case 4:
                                reputation = 600;
                                break;
                            case 5:
                                reputation = 450;
                                break;
                            case 6:
                                reputation = 350;
                                break;
                            case 7:
                                reputation = 300;
                                break;
                            case 8:
                                reputation = 200;
                                break;
                            case 9:
                                reputation = 150;
                                break;
                            case 10:
                                reputation = 100;
                                break;
                            default:
                                if (entry.getValue() <= 50) { reputation = 25; }
                                else { reputation = 12; }
                                break;
                        }
                        c.addReputationScore(reputation);
                    }
                }
            }

            RaidBossPointsManager.getInstance().cleanUp();
            LOGGER.info("Raid Points Reset Global Task: launched.");
        }
    }
}