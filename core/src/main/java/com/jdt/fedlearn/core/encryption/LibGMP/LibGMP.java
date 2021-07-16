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

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains JNA mappings to libgmp functions
 *
 * The implementation borrows code from jna-gmp (https://github.com/square/jna-gmp)
 */
public class LibGMP {
    private static final Class SIZE_T_CLASS;
    private static final Logger logger = LoggerFactory.getLogger(LibGMP.class);
    /**
     * These classes are very similar but when doing readSizeT(ptr) implementation is different.
     */
    static class SizeT4 {
        static native void __gmpz_import(mpz_t rop, int count, int order, int size, int endian,
                                         int nails, Pointer buffer);
        static native Pointer __gmpz_export(Pointer rop, Pointer countp, int order, int size,
                                            int endian, int nails, mpz_t op);
    }
    static class SizeT8 {
        static native void __gmpz_import(mpz_t rop, int count, int order, int size, int endian,
                                         int nails, Pointer buffer);
        static native Pointer __gmpz_export(Pointer rop, Pointer countp, int order, int size,
                                            int endian, int nails, mpz_t op);
    }

    /**
     * Pick one of SizeT4 / SizeT8 based on the size of a native size_t type (in bytes)
     */
    static {
        if (Native.SIZE_T_SIZE == 4) {
            SIZE_T_CLASS = SizeT4.class;
        } else if (Native.SIZE_T_SIZE == 8) {
            SIZE_T_CLASS = SizeT8.class;
        } else {
            throw new AssertionError("Unexpected Native.SIZE_T_SIZE: "  + Native.SIZE_T_SIZE);
        }
    }


    public static void __gmpz_import(mpz_t rop, int count, int order, int size, int endian,
                                     int nails, Pointer buffer) {
        if (SIZE_T_CLASS == SizeT4.class) {
            SizeT4.__gmpz_import(rop, count, order, size, endian, nails, buffer);
        } else {
            SizeT8.__gmpz_import(rop, count, order, size, endian, nails, buffer);
        }
    }

    public static void __gmpz_export(Pointer rop, Pointer countp, int order, int size, int endian,
                                     int nails, mpz_t op) {
        if (SIZE_T_CLASS == SizeT4.class) {
            SizeT4.__gmpz_export(rop, countp, order, size, endian, nails, op);
        } else {
            SizeT8.__gmpz_export(rop, countp, order, size, endian, nails, op);
        }
    }

    /**
     * Load the GMP library.
     *
     * loadlib("libgmp") works for linux systems that have libgmp installed
     * loadlib("libgmp-10") loads for windows systems where the gmp library was built using
     *       ./configure --disable-static --enable-shared --host=x86_64-w64-mingw32
     */
    static {//load any required libraries (for now libgmp only) by name
        if (SystemUtils.IS_OS_MAC) {
            loadlib("gmp");//mac lib libgmp
        } else if (SystemUtils.IS_OS_LINUX) {
            loadlib("libgmp");//linux use after building/installing libgmp
        } else if (SystemUtils.IS_OS_WINDOWS) {
            loadlib("libgmp-10");//windows build with mingw32
        }
    }
    private static void loadlib(String name) {
        try {
            NativeLibrary lib = NativeLibrary.getInstance(name, LibGMP.class.getClassLoader());
            Native.register(LibGMP.class, lib);
            Native.register(SIZE_T_CLASS, lib);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void init() {} //method to force initialization

    public static int readSizeT(Pointer ptr) {
        if (SIZE_T_CLASS == SizeT4.class) {
            int result = ptr.getInt(0);
            assert result >= 0;
            return result;
        } else {
            long result = ptr.getLong(0);
            assert result >= 0;
            assert result < Integer.MAX_VALUE;
            return (int)result;
        }
    }




    /**
     * Initializing/assigning integers
     */
    public static native void __gmpz_init(mpz_t integer);//limp space, initial value 0
    public static native void __gmpz_init2(mpz_t x, NativeLong n);//space for n-bit numbers, initial value 0
    public static native void __gmpz_clear(mpz_t x);//Free the space occupied by x

    public static native void __gmpz_abs(mpz_t rop, mpz_t op);
    public static native void __gmpz_neg(mpz_t rop, mpz_t op);
    public static native void __gmpz_add(mpz_t rop, mpz_t op1, mpz_t op2);
    public static native void __gmpz_sub(mpz_t rop, mpz_t op1, mpz_t op2);
    public static native void __gmpz_mul(mpz_t rop, mpz_t op1, mpz_t op2);
    public static native void __gmpz_tdiv_q(mpz_t q, mpz_t n, mpz_t d);
    public static native void __gmpz_tdiv_r(mpz_t r, mpz_t n, mpz_t d);

    public static native void __gmpz_divexact(mpz_t q, mpz_t n, mpz_t d);
    public static native void __gmpz_gcd(mpz_t rop, mpz_t op1, mpz_t op2);

    /**
     * Compare op1 and op2. Returns positive if op1 大于 op2, zero if equal, and negative if op1 小于 op2
     */
    public static native int __gmpz_cmp_si(mpz_t op1, NativeLong op2);

    /**
     * Modular Operations
     *
     * For powm, negative exponent is supported (if base is invertible)
     * For powm_sec, exp 大于 0 and base is odd
     */
    public static native void __gmpz_mod(mpz_t r, mpz_t n, mpz_t d);
    public static native void __gmpz_powm(mpz_t rop, mpz_t base, mpz_t exp, mpz_t mod);
    public static native void __gmpz_powm_sec(mpz_t rop, mpz_t base, mpz_t exp, mpz_t mod);
    public static native int __gmpz_invert(mpz_t rop, mpz_t op1, mpz_t op2);//modpowinv


    /**
     * Compute the Legendre and Jacobi symbols (a/p)
     * @param a
     * @param p
     * @return
     */
    public static native int __gmpz_legendre(mpz_t a, mpz_t p);
    public static native int __gmpz_jacobi(mpz_t a, mpz_t p);


    /**
     * Primality functions
     */
    public static native int __gmpz_probab_prime_p(mpz_t a, int reps);
    public static native void __gmpz_nextprime(mpz_t rop, mpz_t op);

}