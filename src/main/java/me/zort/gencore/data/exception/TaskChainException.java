package me.zort.gencore.data.exception;

import lombok.Getter;
import me.zort.gencore.lifecycle.TaskChainImpl;

public class TaskChainException extends RuntimeException {

    @Getter
    private final TaskChainImpl taskChain;

    public TaskChainException(String message, TaskChainImpl taskChain) {
        super(message);
        this.taskChain = taskChain;
    }

}
