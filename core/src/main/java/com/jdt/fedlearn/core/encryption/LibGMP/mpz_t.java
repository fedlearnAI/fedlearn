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

import com.sun.jna.Pointer;

/**
 * The data type to pass to libgmp functions (gmp integers of type mpz_t)
 *
 * Prototype taken from jna-gmp github project (https://github.com/square/jna-gmp)
 */
public class mpz_t extends Pointer {
    public static final int SIZE = 16;//size in bytes of the native structures

    /**
     * Construct a long from a native address.
     * @param peer the address of a block of native memory at least SIZE bytes large
     */
    public mpz_t(long peer) {
        super(peer);
    }

    /**
     * Constructs mpz_t from a Pointer
     * @param from
     */
    public mpz_t(Pointer from) {
        this(Pointer.nativeValue(from));
    }
}