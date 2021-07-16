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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EasySharing {
    private EasySharing() {
    }
    // Generate a secret sharing form of a number
    public static double[] generate(int n, double secretNum) {
        final double[] p = new double[n];
        // Generating random numbers
        //TODO randomseed
        Random rand = new Random(7);
        double temp = secretNum;
        for (int i = 0; i < n - 1; i++) {
            p[i] = rand.nextFloat() * Math.pow(10, 8) - Math.pow(10, 4);
            temp = temp - p[i];
        }
        p[n - 1] = temp;
        return p;
    }

    public static double interpolate(double[][] points) {
        double secretNum = 0;
        for (int i = 0; i < points.length; i++) {
            secretNum = secretNum + points[i][1];
        }
        return secretNum;
    }

    // secret_ Num is an integer array containing multiple target numbers to be shared
    public static Map<Integer, double[]> split(double[] secretNum, int n) {
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
        final Map<Integer, double[]> parts = new HashMap<>(n);
        for (int i = 0; i < values.length; i++) {
            parts.put(i + 1, values[i]);
        }
        return Collections.unmodifiableMap(parts);
    } // According to the input secret number, it is allocated to n party in the form of secret sharing


    public static double[] join(Map<Integer, double[]> parts) {
//        checkArgument(parts.size() > 0, "No parts provided");  //Check whether the input collection is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        final int[] lengths = parts.values().stream().mapToInt(v -> v.length).distinct().toArray();
//        checkArgument(lengths.length == 1, "Varying lengths of part values");

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


}
