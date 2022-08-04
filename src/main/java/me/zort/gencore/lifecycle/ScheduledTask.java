package me.zort.gencore.lifecycle;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Logger;

public abstract class ScheduledTask implements Runnable {

    @Setter
    @Getter
    private TaskMeta meta;
    private final long interval;

    @Setter(AccessLevel.PROTECTED)
    @Getter
    private ScheduledTaskChain assignedChain;

    public ScheduledTask(long interval) {
        this(TaskMeta.def(), interval);
    }

    public ScheduledTask(TaskMeta meta, long interval) {
        this.meta = meta;
        this.interval = interval;
        this.assignedChain = null;
    }

    @Override
    public void run() {
        if(assignedChain == null) {
            return;
        }
        long uptime = assignedChain.getUptime();
        if(uptime % interval == 0L) {
            run(uptime);
        }
    }

    public abstract void run(long uptime);

    public void runCatching(Logger logger) {
        try {
            run();
        } catch(Exception ex) {
            logger.info("Task Error (" + meta.getUniqueId().toString() + ") :: " + ex.getMessage());
        }
    }

}
