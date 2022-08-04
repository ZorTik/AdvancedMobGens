package me.zort.gencore.data.persister;

import com.google.common.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static me.zort.gencore.GenCore.GSON;

public class PlayerDataVirtualStoragePersister extends StringType {

    @Nullable
    private static PlayerDataVirtualStoragePersister SINGLETON = null;

    public static PlayerDataVirtualStoragePersister getSingleton() {
        if(SINGLETON == null) {
            SINGLETON = new PlayerDataVirtualStoragePersister();
        }
        return SINGLETON;
    }

    protected PlayerDataVirtualStoragePersister() {
        super(SqlType.STRING, new Class[0]);
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        List<String> mapObject = (List<String>) javaObject;
        return GSON.toJson(mapObject);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        String str = (String) sqlArg;
        List<String> list = GSON.fromJson(str, new TypeToken<List<String>>(){}.getType());
        return Collections.synchronizedList(list);
    }

}
