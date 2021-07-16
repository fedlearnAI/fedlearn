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

import java.util.*;

public class EasyScheme {
    private int n; //Number of participants

    public EasyScheme(int n) {
        checkArgument(n > 0, "N must be > 0");
        this.n = n;
    } //Check whether the input is valid

    // secret_ Num is an integer array containing multiple target numbers to be shared
    public Map<Integer, double[]> split(double[] secretNum) {
        // Generate shared values for all parties
        double[][] values = new double[n][secretNum.length];
        for (int i = 0; i < secretNum.length; i++) {
            // Generate secret sharing values for each number
            double[] p = EasySharing.generate(n, secretNum[i]);
            for (int x = 1; x <= n; x++) {
                // Distribution to parties
                values[x - 1][i] = p[x - 1]; //Value is the secret number assigned to each party
            }
        }

        // Return in the form of hash set, key: 1 ~ n value: secret sharing of all parties
        final Map<Integer, double[]> parts = new HashMap<>(n());
        for (int i = 0; i < values.length; i++) {
            parts.put(i + 1, values[i]);
        }
        return Collections.unmodifiableMap(parts);
    } // According to the input secret number, it is allocated to n party in the form of secret sharing


    public double[] join(Map<Integer, double[]> parts) {
        checkArgument(parts.size() > 0, "No parts provided");  //Check whether the input collection is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        final int[] lengths = parts.values().stream().mapToInt(v -> v.length).distinct().toArray();
        checkArgument(lengths.length == 1, "Varying lengths of part values");

        final double[] secret = new double[lengths[0]];
        for (int i = 0; i < secret.length; i++) {
            final double[][] points = new double[parts.size()][2]; //Create a two-dimensional byte array of the shape n * 2
            int j = 0;

            //The input set map is traversed and stored in points
            for (Map.Entry<Integer, double[]> part : parts.entrySet()) {
                points[j][0] = part.getKey();
                points[j][1] = part.getValue()[i];
                j++;
            }
            secret[i] = EasySharing.interpolate(points); //Collection information to restore data
        }
        return secret;
    }


    public int n() {
        return n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EasyScheme)) {
            return false;
        }
        final EasyScheme scheme = (EasyScheme) o;
        return n == scheme.n;
    }

    @Override
    public int hashCode() {
        return Objects.hash(n);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EasyScheme.class.getSimpleName() + "[", "]")
                .add("n=" + n)
                .toString();
    }

    private static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
