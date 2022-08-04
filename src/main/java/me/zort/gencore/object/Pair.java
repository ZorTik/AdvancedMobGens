package me.zort.gencore.object;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Pair<F, S> {

    private F first;
    private S second;

}
