package com.jdt.fedlearn.core.type.data;


public class IntTuple3 {
    private final int a;
    private final int b;
    private final int c;

    public IntTuple3(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public int _1() {
        return a;
    }

    public int _2() {
        return b;
    }

    public int _3() {
        return c;
    }

    @Override
    public String toString() {
        return "IntTuple3{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                '}';
    }
}
