package me.zort.gencore.data;

import org.jetbrains.annotations.Nullable;

public interface Connector<C, CRE> extends AutoCloseable {

    C connect(CRE credentials);
    @Nullable
    C getConnection();
    @Override
    void close() throws Exception;

    default boolean isConnected() {
        return getConnection() != null;
    }

}
