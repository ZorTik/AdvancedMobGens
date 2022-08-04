package me.zort.gencore.data.persister;

import com.google.common.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.zort.gencore.GenCore.GSON;

public class JsonListPersister<T> extends StringType {

    private final Type type;
    private final boolean sync;

    protected JsonListPersister(boolean sync) {
        super(SqlType.STRING, new Class[0]);
        this.type = new TypeToken<ArrayList<T>>(){}.getType();
        this.sync = sync;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        List<T> mapObject = (List<T>) javaObject;
        return GSON.toJson(mapObject);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        String str = (String) sqlArg;
        List<T> list = GSON.<List<T>>fromJson(str, type);
        return sync
                ? Collections.synchronizedList(list)
                : list;
    }

}
