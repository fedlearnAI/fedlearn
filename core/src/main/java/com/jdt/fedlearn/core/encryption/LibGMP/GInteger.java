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

package com.jdt.fedlearn.core.encryption.LibGMP;

import java.beans.Transient;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

/**
 * Prototype taken from jna-gmp github project (https://github.com/square/jna-gmp)
 */
public class GInteger extends BigInteger implements Serializable {
    private static final long serialVersionUID = -8474409790218658764L;
    private transient final MPZMemory memory = new MPZMemory();

    {
        GMP.INSTANCE.get().mpzImport(memory.peer, super.signum(), super.abs().toByteArray());
    }

    mpz_t getPeer() {
        return memory.peer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GInteger gInteger = (GInteger) o;
        return Objects.equals(memory, gInteger.memory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), memory);
    }

    /**
     * Various constructors
     */
    public GInteger (BigInteger other) {
        super(other.toByteArray());
    }

    public GInteger(byte[] val) {
        super(val);
    }
    public GInteger(int signum, byte[] magnitude) {
        super(signum, magnitude);
    }
    public GInteger(String val, int radix) {
        super(val, radix);
    }
    public GInteger(String val) {
        super(val);
    }
    public GInteger(int numbits, Random r) {
        super(numbits, r);
    }
    public GInteger (int bitlength, int certainty, Random r) {
        super(bitlength, certainty, r);
    }
}