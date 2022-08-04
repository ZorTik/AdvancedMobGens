package me.zort.gencore.configuration;

import java.io.File;

public interface Configuration {

    boolean reload();
    boolean isLoaded();
    File getFile();

}
