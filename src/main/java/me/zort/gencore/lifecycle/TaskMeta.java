package me.zort.gencore.lifecycle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.gencore.object.ChainSort;
import me.zort.gencore.object.Predicate;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class TaskMeta implements ChainSort<TaskMeta> {

    public static TaskMeta def() {
        return def(false);
    }

    public static TaskMeta def(boolean async) {
        Predicate defPredicate = Predicate.of(() -> true);
        return new TaskMeta(0, defPredicate, async);
    }

    private final int priority;
    private final Predicate condition;
    private final boolean async;
    private UUID uniqueId = UUID.randomUUID();
    private UUID pausedPredId = null;

    public boolean isBefore(TaskMeta other) {
        return !isAfter(other);
    }

    public boolean isAfter(TaskMeta other) {
        return other.getPriority() >= priority;
    }

    public boolean canInvokeNow() {
        return condition.test();
    }

    public void setPaused(boolean paused) {
        if(paused && !isPaused()) {
            Predicate pausedPred = Predicate.of(() -> false);
            pausedPredId = pausedPred.getUniqueId();
            condition.and(pausedPred);
        } else {
            pausedPredId = null;
            isPaused();
        }
    }

    public boolean isPaused() {
        java.util.function.Predicate<Predicate> uuidPred = pred -> pred.getUniqueId().equals(pausedPredId);
        if(pausedPredId == null && condition.anyPart(uuidPred)) {
            condition.deletePart(uuidPred);
        }
        return pausedPredId != null;
    }

}
