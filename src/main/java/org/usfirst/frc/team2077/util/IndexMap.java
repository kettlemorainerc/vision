package org.usfirst.frc.team2077.util;

import java.util.*;
import java.util.function.BiConsumer;

public class IndexMap {
    private LinkedList<Mapping> mapping = new LinkedList<>();

    public void addMapping(int fromIndex, int toIndex) {
        if(toIndex == -1) return;
        mapping.add(new Mapping(fromIndex, toIndex));
    }

    public void forEach(BiConsumer<Integer, Integer> sourceTargetIdxPair) {
        for(Mapping m : mapping) {
            sourceTargetIdxPair.accept(m.from, m.to);
        }
    }

    public int getMapped(int from) {
        for(Mapping m : mapping) if(m.from == from) return m.to;
        return -1;
    }

    public boolean isEmpty() {return mapping.isEmpty();}

    public static final class Mapping {
        private final int from, to;

        Mapping(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }
}
