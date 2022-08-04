package me.zort.gencore.data.connection.sqlite;

import lombok.Data;

import java.util.Objects;

@Data
public class SQLiteCredentials {

    public static SQLiteCredentials of(String filePath) {
        Objects.requireNonNull(filePath);
        SQLiteCredentials sqLiteCredentials = new SQLiteCredentials();
        sqLiteCredentials.setFilePath(filePath);
        return sqLiteCredentials;
    }

    private String filePath;

}
