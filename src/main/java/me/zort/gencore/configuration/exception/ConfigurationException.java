package me.zort.gencore.configuration.exception;

import lombok.Getter;
import me.zort.gencore.configuration.Configuration;

public class ConfigurationException extends RuntimeException {

    @Getter
    private final Configuration configuration;

    public ConfigurationException(String message, Configuration configuration) {
        super(message);
        this.configuration = configuration;
    }

}
