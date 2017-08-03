package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.taskmanager.tasks.*;
import net.sf.l2j.gameserver.taskmanager.tasks.ATask.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public final class TaskManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);

    private static final String LOAD_TASKS = "SELECT id,task,type,last_activation,param1,param2,param3 FROM global_tasks";
    private static final String UPDATE_TASK = "UPDATE global_tasks SET last_activation=? WHERE id=?";
    private static final String LOAD_TASK = "SELECT id FROM global_tasks WHERE task=?";
    private static final String SAVE_TASK = "INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)";

    private final Map<Integer, ATask> _tasks = new HashMap<>();
    protected final List<ExecutedTask> _currentTasks = new ArrayList<>();

    public class ExecutedTask implements Runnable {
        private final int _id;
        private final ATask _task;
        private final TaskType _type;
        private final String[] _params;

        private long _lastActivation;
        ScheduledFuture<?> _scheduled;

        public ExecutedTask(int id, ATask task, TaskType type, String[] params, long lastActivation) {
            _id = id;
            _task = task;
            _type = type;
            _params = params;

            _lastActivation = lastActivation;
        }

        @Override
        public void run() {
            _task.onTimeElapsed(this);
            _lastActivation = System.currentTimeMillis();

            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement(UPDATE_TASK);
                statement.setLong(1, _lastActivation);
                statement.setInt(2, _id);
                statement.executeUpdate();
                statement.close();
            }
            catch (SQLException e) {
                LOGGER.error("Cannot updated the Global Task {}: {}", _id, e.getMessage(), e);
            }

            if (_type == TaskType.TYPE_SHEDULED || _type == TaskType.TYPE_TIME) { stopTask(); }
        }

        @Override
        public boolean equals(Object object) {
            return _id == ((ExecutedTask) object)._id;
        }

        public int getId() {
            return _id;
        }

        public ATask getTask() {
            return _task;
        }

        public TaskType getType() {
            return _type;
        }

        public String[] getParams() {
            return _params;
        }

        public long getLastActivation() {
            return _lastActivation;
        }

        public void stopTask() {
            if (_scheduled != null) { _scheduled.cancel(true); }

            _currentTasks.remove(this);
        }

    }

    public static final TaskManager getInstance() {
        return SingletonHolder._instance;
    }

    protected TaskManager() {
        // initialize all tasks
        registerTask(new TaskClansLadder());
        registerTask(new TaskCleanUp());
        registerTask(new TaskOlympiadSave());
        registerTask(new TaskRaidPointsReset());
        registerTask(new TaskRecom());
        registerTask(new TaskRestart());
        registerTask(new TaskSevenSignsUpdate());
        registerTask(new TaskShutdown());

        // load data and start all tasks
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(LOAD_TASKS);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                ATask task = _tasks.get(rset.getString("task").trim().toLowerCase().hashCode());
                if (task == null) { continue; }

                TaskType type = TaskType.valueOf(rset.getString("type"));
                if (type != TaskType.TYPE_NONE) {
                    String[] params = new String[]
                            {
                                    rset.getString("param1"),
                                    rset.getString("param2"),
                                    rset.getString("param3")
                            };

                    ExecutedTask current = new ExecutedTask(rset.getInt("id"), task, type, params, rset.getLong("last_activation"));
                    if (launchTask(current)) { _currentTasks.add(current); }
                }
            }
            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error while loading Global Task table: {}", e.getMessage(), e);
        }
    }

    private final void registerTask(ATask task) {
        _tasks.put(task.getName().hashCode(), task);
        task.initializate();
    }

    private static final boolean launchTask(ExecutedTask task) {
        final ThreadPoolManager scheduler = ThreadPoolManager.getInstance();
        final TaskType type = task.getType();
        long delay, interval;

        switch (type) {
            case TYPE_STARTUP:
                task.run();
                return false;

            case TYPE_SHEDULED:
                delay = Long.valueOf(task.getParams()[0]);
                task._scheduled = scheduler.scheduleGeneral(task, delay);
                return true;

            case TYPE_FIXED_SHEDULED:
                delay = Long.valueOf(task.getParams()[0]);
                interval = Long.valueOf(task.getParams()[1]);
                task._scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
                return true;

            case TYPE_TIME:
                try {
                    Date desired = DateFormat.getInstance().parse(task.getParams()[0]);
                    long diff = desired.getTime() - System.currentTimeMillis();
                    if (diff >= 0) {
                        task._scheduled = scheduler.scheduleGeneral(task, diff);
                        return true;
                    }
                    LOGGER.info("Task {} is obsoleted.", task.getId());
                    return false;
                }
                catch (Exception e) {
                    return false;
                }

            case TYPE_SPECIAL:
                ScheduledFuture<?> result = task.getTask().launchSpecial(task);
                if (result != null) {
                    task._scheduled = result;
                    return true;
                }
                return false;

            case TYPE_GLOBAL_TASK:
                interval = Long.valueOf(task.getParams()[0]) * 86400000L;
                String[] hour = task.getParams()[1].split(":");

                if (hour.length != 3) {
                    LOGGER.warn("Task {} has incorrect parameters", task.getId());
                    return false;
                }

                Calendar check = Calendar.getInstance();
                check.setTimeInMillis(task.getLastActivation() + interval);

                Calendar min = Calendar.getInstance();
                try {
                    min.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour[0]));
                    min.set(Calendar.MINUTE, Integer.parseInt(hour[1]));
                    min.set(Calendar.SECOND, Integer.parseInt(hour[2]));
                }
                catch (Exception e) {
                    LOGGER.error("Bad parameter on task {}: {}", task.getId(), e.getMessage(), e);
                    return false;
                }

                delay = min.getTimeInMillis() - System.currentTimeMillis();

                if (check.after(min) || delay < 0) { delay += interval; }

                task._scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
                return false;

            default:
                return false;
        }
    }

    public static boolean addUniqueTask(String task, TaskType type, String param1, String param2, String param3, long lastActivation) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(LOAD_TASK);
            statement.setString(1, task);
            ResultSet rset = statement.executeQuery();

            if (!rset.next()) {
                PreparedStatement statement2 = con.prepareStatement(SAVE_TASK);
                statement2.setString(1, task);
                statement2.setString(2, type.toString());
                statement2.setLong(3, lastActivation);
                statement2.setString(4, param1);
                statement2.setString(5, param2);
                statement2.setString(6, param3);
                statement2.execute();
                statement2.close();
            }

            rset.close();
            statement.close();

            return true;
        }
        catch (SQLException e) {
            LOGGER.error("Cannot add the unique task: {}", e.getMessage(), e);
        }

        return false;
    }

    public static boolean addTask(String task, TaskType type, String param1, String param2, String param3, long lastActivation) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(SAVE_TASK);
            statement.setString(1, task);
            statement.setString(2, type.toString());
            statement.setLong(3, lastActivation);
            statement.setString(4, param1);
            statement.setString(5, param2);
            statement.setString(6, param3);
            statement.execute();

            statement.close();
            return true;
        }
        catch (SQLException e) {
            LOGGER.error("Cannot add the task:  {}", e.getMessage(), e);
        }

        return false;
    }

    private static class SingletonHolder {
        protected static final TaskManager _instance = new TaskManager();
    }
}