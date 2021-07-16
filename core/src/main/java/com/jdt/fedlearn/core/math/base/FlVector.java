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

package com.jdt.fedlearn.core.math.base;

import com.jdt.fedlearn.core.math.Vector;

import java.util.Arrays;

public class FlVector implements Vector {
    public int[] inner;

//    public FlVector(int[] array) {
//        this.inner = array;
//    }

    public FlVector(int... array) {
        this.inner = array;
    }


    public FlVector add(Vector vector){
        if (inner == null || vector==null || inner.length != vector.size()) {
            return null;
        }
        int[] output = new int[inner.length];
        int[] vectorEle = vector.getEle();
        for (int i = 0;i<inner.length;i++){
            output[i] = inner[i] + vectorEle[i];
        }
        return new FlVector(output);
    }



    public int getLength() {
        return 0;
    }

    public int size(){
        return inner.length;
    }

    public int[] getEle() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        FlVector flVector = (FlVector) o;
        return Arrays.equals(inner, flVector.inner);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(inner);
    }
}
