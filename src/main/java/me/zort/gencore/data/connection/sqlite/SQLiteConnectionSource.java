package me.zort.gencore.data.connection.sqlite;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import lombok.Getter;
import me.zort.gencore.GenCore;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;

@Getter
public class SQLiteConnectionSource extends JdbcPooledConnectionSource {

    @NotNull
    private final String filePath;

    public SQLiteConnectionSource(SQLiteCredentials credentials) throws SQLException {
        super("jdbc:sqlite:" + GenCore.getSingleton().getDataFolder().getAbsolutePath() + credentials.getFilePath());
        this.filePath = credentials.getFilePath();
    }

    public File getDatabaseFile() {
        return new File(filePath);
    }

}
