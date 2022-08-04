package me.zort.gencore.data;

@FunctionalInterface
public interface Accessor<T> {

    void access(T object);

}
