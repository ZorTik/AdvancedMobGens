package me.zort.gencore.data.persister;

import com.google.common.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import me.zort.gencore.data.companion.Generator;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static me.zort.gencore.GenCore.GSON;

public class PlayerDataGeneratorsPersister extends StringType {

    @Nullable
    private static PlayerDataGeneratorsPersister SINGLETON = null;

    public static PlayerDataGeneratorsPersister getSingleton() {
        if(SINGLETON == null) {
            SINGLETON = new PlayerDataGeneratorsPersister();
        }
        return SINGLETON;
    }

    protected PlayerDataGeneratorsPersister() {
        super(SqlType.STRING, new Class[0]);
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        List<Generator> mapObject = (List<Generator>) javaObject;
        return GSON.toJson(mapObject);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        String str = (String) sqlArg;
        List<Generator> list = GSON.fromJson(str, new TypeToken<List<Generator>>(){}.getType());
        return Collections.synchronizedList(list);
    }

}
