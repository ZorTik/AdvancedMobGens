package me.zort.gencore.data.exception;

import lombok.Getter;
import me.zort.gencore.data.Connector;

@Getter
public class CoreDataException extends RuntimeException {

    private final Connector<?, ?> connector;

    public CoreDataException(Connector<?, ?> connector, String message) {
        super(message);
        this.connector = connector;
    }

}
