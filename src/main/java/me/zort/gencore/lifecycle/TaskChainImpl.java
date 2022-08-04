package me.zort.gencore.lifecycle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.gencore.data.exception.TaskChainException;
import me.zort.gencore.object.Chain;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TaskChainImpl implements Chain<ScheduledTask> {

    private final List<ScheduledTask> tasksCache = Collections.synchronizedList(Lists.newLinkedList());
    private final Map<String, ScheduledTaskRef> futureCacheInternal = Maps.newConcurrentMap();
    @Getter
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    @Getter
    private final Plugin plugin;
    private final Logger logger;

    public TaskChainImpl(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void cleanup() {
        List<String> toRem = Lists.newArrayList();
        for(String id : futureCacheInternal.keySet()) {
            try {
                ScheduledTaskRef ref = futureCacheInternal.get(id);
                if(ref == null) {
                    toRem.add(id);
                    continue;
                }
                if(ref.getRunnable().isCancelled() || (System.currentTimeMillis() - ref.getMillis()) > TimeUnit.MINUTES.toMillis(2)) {
                    toRem.add(id);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        toRem.forEach(futureCacheInternal::remove);
    }

    public boolean invokeCandidates(boolean renew) {
        cleanup();
        LinkedList<ScheduledTask> applicableTasks = getApplicableTasks();
        applicableTasks.forEach(task -> {
            TaskMeta meta = task.getMeta();
            String chainId = RandomStringUtils.randomAlphanumeric(12);
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    task.runCatching(logger);
                    try {
                        getTaskId();
                        cancel();
                    } catch(Exception ignored) {}
                    futureCacheInternal.remove(chainId);
                }
            };
            if(meta.isAsync()) {
                ScheduledTaskRef ref = new ScheduledTaskRef(runnable);
                futureCacheInternal.put(chainId, ref);
                runnable.runTaskAsynchronously(plugin);
            } else {
                runnable.run();
            }
        });
        if(renew) {
            List<ScheduledTask> pausedTasks = getPausedTasks();
            renew(pausedTasks);
        }
        return true;
    }

    @Override
    public void appendAfter(ScheduledTask part) {
        if(tasksCache.stream()
                .anyMatch(task -> {
                    TaskMeta meta = task.getMeta();
                    return meta.getUniqueId()
                            .equals(part.getMeta().getUniqueId());
                })) {
            throw new TaskChainException("Provided part is already in task chain!", this);
        }
        tasksCache.add(part);
    }

    @Override
    public ScheduledTask next() {
        if(!hasNext()) {
            return null;
        }
        LinkedList<ScheduledTask> applicableParts = getApplicableTasks();
        return applicableParts.getFirst();
    }

    @Override
    public boolean hasNext() {
        return !getApplicableTasks().isEmpty();
    }

    public int getPausedTasksCount() {
        return getPausedTasks().size();
    }

    public List<ScheduledTask> getPausedTasks() {
        return tasksCache.stream()
                .filter(task -> task.getMeta().isPaused())
                .collect(Collectors.toList());
    }

    public LinkedList<ScheduledTask> getApplicableTasks() {
        return getParts().stream()
                .filter(part -> !part.getMeta().isPaused())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public LinkedList<ScheduledTask> getParts() {
        List<ScheduledTask> temp = tasksCache.stream()
                .sorted(Comparator.comparingInt(task -> task.getMeta().getPriority()))
                .collect(Collectors.toList());
        Collections.reverse(temp);
        return new LinkedList<>(temp);
    }

    private void renew(List<ScheduledTask> newList) {
        tasksCache.clear();
        tasksCache.addAll(newList);
    }

    @RequiredArgsConstructor
    @Getter
    public static class ScheduledTaskRef {

        private final long millis = System.currentTimeMillis();
        private final BukkitRunnable runnable;

    }

}
