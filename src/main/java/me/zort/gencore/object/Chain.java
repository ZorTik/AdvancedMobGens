package me.zort.gencore.object;

import java.util.Iterator;
import java.util.LinkedList;

public interface Chain<T> extends Iterator<T> {

    void appendAfter(T part);
    LinkedList<T> getParts();

}
