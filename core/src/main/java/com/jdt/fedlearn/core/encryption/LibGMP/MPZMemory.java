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

import com.sun.jna.Memory;

import java.util.Objects;

/**
 * Prototype taken from jna-gmp github project (https://github.com/square/jna-gmp)
 *
 * You do not need to edit this class
 */
public class MPZMemory extends Memory {
    public final mpz_t peer;

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
        MPZMemory mpzMemory = (MPZMemory) o;
        return Objects.equals(peer, mpzMemory.peer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), peer);
    }

    MPZMemory() {
        super(mpz_t.SIZE);
        peer = new mpz_t(this);
        LibGMP.__gmpz_init(peer);
    }

    @Override protected void finalize() {
        LibGMP.__gmpz_clear(peer);
        super.finalize();
    }
}