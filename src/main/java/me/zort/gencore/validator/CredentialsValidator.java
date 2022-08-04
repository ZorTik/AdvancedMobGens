package me.zort.gencore.validator;

import me.zort.gencore.data.connection.sqlite.SQLiteConnector;
import me.zort.gencore.data.connection.sqlite.SQLiteCredentials;
import me.zort.gencore.data.exception.CoreDataException;

public final class CredentialsValidator {

    public static void validate(SQLiteConnector connector, SQLiteCredentials credentials) {
        if(credentials.getFilePath() == null) {
            throw new CoreDataException(connector, "JDBC database file path can't be null!");
        }
    }

}
