package me.zort.gencore.object;

public interface ChainSort<T> {

    boolean isBefore(T other);
    boolean isAfter(T other);

}
