package me.zort.gencore.validator;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Validator {

    public static void requireNonNulls(Object... objects) {
        requireNonNulls(Arrays.stream(objects).collect(Collectors.toList()));
    }

    public static void requireNonNulls(Collection<Object> objects) {
        requireNonNulls(objects, null);
    }

    public static void requireNonNulls(Collection<Object> objects, @Nullable String message) {
        for(Object o : objects) {
            if(message != null) {
                Objects.requireNonNull(objects, message);
            } else {
                Objects.requireNonNull(objects);
            }
        }
    }

}
