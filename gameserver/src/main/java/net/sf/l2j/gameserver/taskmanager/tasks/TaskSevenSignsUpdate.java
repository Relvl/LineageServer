package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates all data for the Seven Signs and Festival of Darkness engines, when time is elapsed.
 *
 * @author Tempy
 */
public final class TaskSevenSignsUpdate extends ATask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSevenSignsUpdate.class);

    private static final String NAME = "SevenSignsUpdate";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initializate() {
        TaskManager.addUniqueTask(NAME, TaskType.TYPE_FIXED_SHEDULED, "1800000", "1800000", "", 0);
    }

    @Override
    public void onTimeElapsed(ExecutedTask task) {
        try {
            SevenSigns.getInstance().saveSevenSignsStatus();

            if (!SevenSigns.getInstance().isSealValidationPeriod()) { SevenSignsFestival.getInstance().saveFestivalData(false); }

            LOGGER.info("SevenSigns: Data updated successfully.");
        }
        catch (Exception e) {
            LOGGER.error("SevenSigns: Failed to save Seven Signs configuration: ", e);
        }
    }
}