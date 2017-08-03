package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Layane
 */
public final class TaskRecom extends ATask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRecom.class);

    private static final String NAME = "sp_recommendations";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initializate() {
        TaskManager.addUniqueTask(NAME, TaskType.TYPE_GLOBAL_TASK, "1", "06:30:00", "", 0);
    }

    @Override
    public void onTimeElapsed(ExecutedTask task) {
        for (L2PcInstance player : L2World.getInstance().getPlayers()) {
            player.restartRecom();
            player.sendPacket(new UserInfo(player));
        }
        LOGGER.info("Recommendation Global Task: launched.");
    }
}