package com.jdt.fedlearn.core.math.base;

import com.jdt.fedlearn.core.math.Scalar;

import java.util.Objects;

public class FlScalar implements Scalar {
    private final int a;

    public FlScalar(int a) {
        this.a = a;
    }

    public Scalar mul(Scalar scalar){
        return new FlScalar(scalar.getEle() * this.getEle()) ;
    }

    public Scalar add(Scalar scalar){
        return new FlScalar(scalar.getEle() + this.a);
    }

    public int getEle(){
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        FlScalar flScalar = (FlScalar) o;
        return a == flScalar.a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a);
    }
}
