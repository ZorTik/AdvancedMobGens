package me.zort.gencore.validator;

import me.zort.gencore.configuration.Configuration;
import me.zort.gencore.configuration.exception.ConfigurationException;

import java.io.File;

public final class ConfigurationValidator {

    public static void validate(Configuration configuration, File configFile) {
        String message = null;
        if(!configFile.exists()) {
            message = "Configuration file " + configFile.getName() + " does not exist!";
        } else if(configFile.isDirectory()) {
            message = "Configuration file " + configFile.getName() + " is directory!";
        }
        if(message != null) {
            throw new ConfigurationException(message, configuration);
        }
    }

    public static void checkLoaded(Configuration configuration) {
        if(!configuration.isLoaded()) {
            throw new ConfigurationException("Configuration " + configuration.getFile().getName() + " is not loaded yet!", configuration);
        }
    }

}
