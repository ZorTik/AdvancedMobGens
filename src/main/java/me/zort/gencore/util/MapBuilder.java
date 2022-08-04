package me.zort.gencore.util;

import java.util.Map;

public class MapBuilder<K, V> {

    public static <K, V> MapBuilder<K, V> of(Map<K, V> initial) {
        return new MapBuilder<>(initial);
    }

    private final Map<K, V> base;

    public MapBuilder(Map<K, V> initial) {
        this.base = initial;
    }

    public MapBuilder<K, V> record(K key, V value) {
        base.put(key, value);
        return this;
    }

    public Map<K, V> build() {
        return base;
    }

}
