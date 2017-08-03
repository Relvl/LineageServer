package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskOlympiadSave extends ATask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOlympiadSave.class);

    private static final String NAME = "OlympiadSave";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initializate() {
        TaskManager.addUniqueTask(NAME, TaskType.TYPE_FIXED_SHEDULED, "900000", "1800000", "", 0);
    }

    @Override
    public void onTimeElapsed(ExecutedTask task) {
        if (Olympiad.getInstance().inCompPeriod()) {
            Olympiad.getInstance().saveOlympiadStatus();
            LOGGER.info("Olympiad: Data updated successfully.");
        }
    }
}