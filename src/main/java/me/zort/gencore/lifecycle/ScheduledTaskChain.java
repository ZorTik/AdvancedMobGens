package me.zort.gencore.lifecycle;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class ScheduledTaskChain extends TaskChainImpl implements Runnable {

    private BukkitTask generalTask;
    private final long interval;

    private long uptime = 0L;

    public ScheduledTaskChain(Plugin plugin, long interval) {
        super(plugin);
        this.generalTask = null;
        this.interval = interval;
    }

    public void start() {
        if(isRunning()) stop();
        uptime = 0L;
        this.generalTask = getScheduler()
                .runTaskTimer(getPlugin(), this, 0L, interval);
    }

    public void stop() {
        getScheduler().cancelTask(generalTask.getTaskId());
        generalTask = null;
    }

    @Override
    public void appendAfter(ScheduledTask part) {
        part.setAssignedChain(this);
        super.appendAfter(part);
    }

    @Override
    public void run() {
        invokeCandidates(false);
        uptime += interval;
    }

    public boolean isRunning() {
        return generalTask != null && !generalTask.isCancelled();
    }

}
