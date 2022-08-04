package me.zort.gencore.data;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import org.jetbrains.annotations.Nullable;

public interface JdbcConnector<C extends JdbcPooledConnectionSource, CRE> extends Connector<C, CRE> {

    @Nullable
    <T extends Entity<ID>, ID> Dao<T, ID> getDao(Class<T> entityClass);
    @Nullable
    <T extends Entity<ID>, ID> Dao<T, ID> getOrCreateDao(Class<T> entityClass);

}
