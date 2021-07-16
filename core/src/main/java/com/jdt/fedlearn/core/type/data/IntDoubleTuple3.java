/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.type.data;

import java.io.Serializable;
import java.util.Objects;

public class IntDoubleTuple3 implements Serializable {

    private int first;
    private double second;
    private double third;


    public IntDoubleTuple3() {
    }

    /**
     * Creates a new IntDoubleTuple3
     *
     * @param first  The first value for this pair
     * @param second The second value for this pair
     * @param third  The third value for this pair
     */
    public IntDoubleTuple3(@NamedArg("first") int first, @NamedArg("second") double second, @NamedArg("third") double third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public void setValues(int first, double second, double third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public void setDoubles(double second, double third) {
        this.second = second;
        this.third = third;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public double getSecond() {
        return second;
    }

    public void setSecond(double second) {
        this.second = second;
    }

    public double getThird() {
        return third;
    }

    public void setThird(double third) {
        this.third = third;
    }

    @Override
    public String toString() {
        return first + "," + second + "," + third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IntDoubleTuple3) {
            IntDoubleTuple3 triple = (IntDoubleTuple3) o;
            if (!Objects.equals(first, triple.first)) {
                return false;
            }
            if (!Objects.equals(second, triple.second)) {
                return false;
            }
            return Objects.equals(third, triple.third);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }
}
