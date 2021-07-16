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

package com.jdt.fedlearn.core.research.mpc;

import java.security.SecureRandom;
import java.util.*;

/**
 * An implementation of Shamir's Secret Sharing over {@code GF(256)} to securely split secrets into
 * {@code N} parts, of which any {@code K} can be joined to recover the original secret.
 */
public class GF256Scheme {
    private final SecureRandom random;
    private final int n;
    private final int k;

    /**
     * Creates a new {@link GF256Scheme} instance.
     *
     * @param random a {@link SecureRandom} instance
     * @param n      the number of parts to produce (must be {@code >1})
     * @param k      the threshold of joinable parts (must be {@code <= n})
     */
    public GF256Scheme(SecureRandom random, int n, int k) {
        this.random = random;
        checkArgument(k > 1, "K must be > 1");
        checkArgument(n >= k, "N must be >= K");
        checkArgument(n <= 255, "N must be <= 255");
        this.n = n;
        this.k = k;
    } //Check whether the input is valid

    /**
     * Splits the given secret into n parts, of which any k or more can be combined to
     * recover the original secret.
     */
    // Secret is a byte array containing multiple target numbers to be shared
    public Map<Integer, byte[]> split(byte[] secret) {
        // generate part values
        final byte[][] values = new byte[n][secret.length];
        for (int i = 0; i < secret.length; i++) {
            // for each byte, generate a random polynomial, p
            final byte[] p = GF256.generate(random, k - 1, secret[i]); //Where p is the coefficient of polynomial, P [0] = secret [i]
            for (int x = 1; x <= n; x++) {
                // each part's byte is p(partId)
                values[x - 1][i] = GF256.eval(p, (byte) x); //Value is the secret number assigned to each party
            }
        }

        // Return in the form of hash set, key: 1 ~ n value: secret sharing of all parties
        final Map<Integer, byte[]> parts = new HashMap<>(n());
        for (int i = 0; i < values.length; i++) {
            parts.put(i + 1, values[i]);
        }
        return Collections.unmodifiableMap(parts);
    } // According to the input secret number, it is allocated to n party in the form of secret sharing

    /**
     * Joins the given parts to recover the original secret.
     */
    public byte[] join(Map<Integer, byte[]> parts) {
        checkArgument(parts.size() > 0, "No parts provided"); //Check whether the input collection is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        final int[] lengths = parts.values().stream().mapToInt(v -> v.length).distinct().toArray();
        checkArgument(lengths.length == 1, "Varying lengths of part values");

        final byte[] secret = new byte[lengths[0]];
        for (int i = 0; i < secret.length; i++) {
            final byte[][] points = new byte[parts.size()][2]; //Create a two-dimensional byte array of the shape n * 2
            int j = 0;

            //The input set map is traversed and stored in points
            for (Map.Entry<Integer, byte[]> part : parts.entrySet()) {
                points[j][0] = part.getKey().byteValue();
                points[j][1] = part.getValue()[i];
                j++;
            }
            secret[i] = GF256.interpolate(points); //Collection information to restore data
        }
        return secret;
    }

    // Addition of a party
    public byte[] secret_add(byte[] a1, byte[] a2) {
        checkArgument((a1.length > 0) && (a2.length > 0), "No parts provided"); //Check whether the input array is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        checkArgument(a1.length == a2.length, "Varying lengths of part values");

        byte[] result = new byte[a1.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = GF256.add(a1[i], a2[i]); //Collection information to restore data
        }
        return result;
    }


    /**
     * The number of parts the scheme will generate when splitting a secret.
     */
    public int n() {
        return n;
    }

    /**
     * The number of parts the scheme will require to re-create a secret.
     */
    public int k() {
        return k;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GF256Scheme)) {
            return false;
        }
        final GF256Scheme scheme = (GF256Scheme) o;
        return n == scheme.n && k == scheme.k && Objects.equals(random, scheme.random);
    }

    @Override
    public int hashCode() {
        return Objects.hash(random, n, k);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GF256Scheme.class.getSimpleName() + "[", "]")
                .add("random=" + random)
                .add("n=" + n)
                .add("k=" + k)
                .toString();
    }

    private static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}


