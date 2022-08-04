package me.zort.gencore.data.connection.sqlite;

import com.google.common.collect.Maps;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import me.zort.gencore.data.Entity;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.exception.CoreDataException;
import me.zort.gencore.validator.CredentialsValidator;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SQLiteConnector implements JdbcConnector<SQLiteConnectionSource, SQLiteCredentials> {

    private final Collection<Class<? extends Entity<?>>> initEntities;
    private final Map<Class<? extends Entity<?>>, Dao<? extends Entity<?>, ?>> daoCache;
    @Getter
    private final Logger logger;
    private SQLiteConnectionSource connectionSource;

    public SQLiteConnector(Collection<Class<? extends Entity<?>>> initEntities) {
        this(initEntities, Logger.getAnonymousLogger());
    }

    public SQLiteConnector(Collection<Class<? extends Entity<?>>> initEntities, Logger logger) {
        if(initEntities.isEmpty()) {
            throw new CoreDataException(this, "Initialization entities collection size must be > 0!");
        }
        this.initEntities = initEntities;
        this.daoCache = Maps.newConcurrentMap();
        this.logger = logger;
    }

    @Override
    public SQLiteConnectionSource connect(SQLiteCredentials credentials) {
        // Validation
        CredentialsValidator.validate(this, credentials);
        try {
            SQLiteConnectionSource connectionSource = new SQLiteConnectionSource(credentials);
            this.connectionSource = connectionSource;
            // Test connection
            List<Dao> tempDaoList = initEntities.stream()
                    .map(this::gocdInternal)
                    .collect(Collectors.toList());
            if(!tempDaoList.stream()
                    .allMatch(Objects::nonNull)) {
                this.connectionSource = null;
                return null;
            }
            return connectionSource;
        } catch (SQLException e) {
            e.printStackTrace();
            this.connectionSource = null;
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Entity<ID>, ID> Dao<T, ID> getDao(Class<T> entityClass) {
        if(!isConnected() || !daoCache.containsKey(entityClass)) {
            return null;
        }
        return (Dao<T, ID>) daoCache.get(entityClass);
    }

    @Nullable
    private Dao gocdInternal(Class entityClass) {
        return getOrCreateDao(entityClass);
    }

    @Nullable
    public <T extends Entity<ID>, ID> Dao<T, ID> getOrCreateDao(Class<T> entityClass) {
        if(!isConnected()) {
            return null;
        }
        Dao<T, ID> dao = getDao(entityClass);
        if(dao != null) {
            return dao;
        }
        try {
            SQLiteConnectionSource connection = getConnection();
            assert connection != null;
            TableUtils.createTableIfNotExists(connection, entityClass);
            dao = DaoManager.createDao(connection, entityClass);
            daoCache.put(entityClass, dao);
            return dao;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {
        if(!isConnected()) {
            return;
        }
        boolean success = false;
        try {
            connectionSource.close();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!success) {
            logger.info("There was error while closing, but we will still unregister the connection.");
        }
        connectionSource = null;
    }

    @Override
    public @Nullable SQLiteConnectionSource getConnection() {
        return connectionSource;
    }

    public enum Event {

        CREATION,
        MODIFICATION

    }

}
