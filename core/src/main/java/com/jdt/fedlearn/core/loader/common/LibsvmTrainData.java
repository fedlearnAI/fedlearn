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
package com.jdt.fedlearn.core.loader.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibsvmTrainData {

    public LibsvmTrainData() {
    }

    public String[][] lib2csv(String[] data) {
        int[] dim = featureMaxDim(data);
        int maxDim = dim[0];
        int cnt = 0;
        List<String[]> res = new ArrayList<>();
        String[] header = generateHeader(maxDim, "uid", "y");
        res.add(header);
        for (String line : data) {
            String[] strs = line.split(" ");
            String[] r = new String[maxDim + 2];
            Arrays.fill(r, "0");
            r[r.length - 1] = strs[0];
            for (int i = 1; i < strs.length; i++) {
                r[0] = String.valueOf(cnt);
                int ind = Integer.parseInt(strs[i].split(":")[0]);
                r[ind] = strs[i].split(":")[1];
            }
            res.add(r);
            cnt += 1;
        }
        return res.toArray(new String[res.size()][]);
    }

    public int[] featureMaxDim(String[] data) {
        int[] dim = {0, 100000000};
        for (String line : data) {
            String[] strs = line.split(" ");
            int maxInd = Integer.parseInt(strs[strs.length - 1].split(":")[0]);
            if (dim[0] < maxInd) {
                dim[0] = maxInd;
            }
            int minInd = Integer.parseInt(strs[1].split(":")[0]);
            if (dim[1] > minInd) {
                dim[1] = minInd;
            }
        }
        return dim;
    }

    public String[] generateHeader(int maxDim, String uid, String label) {
        String[] header = new String[maxDim + 2];
        header[0] = uid;
        for (int i = 1; i < maxDim + 1; i++) {
            header[i] = String.valueOf(i);
        }
        header[maxDim + 1] = label;
        return header;
    }


    public String[] convertLib(String[] data, int maxDim) {
        String[] res = new String[maxDim + 1];
        Arrays.fill(res, "");
        for (int i = 0; i < data.length; i++) {
            String[] line = data[i].split(" ");
            for (int j = 1; j < line.length; j++) {
                int ind = Integer.parseInt(line[j].split(":")[0]);
//                int cnt = i+1;
                String subLine = i + ":" + line[j].split(":")[1] + " ";
                res[ind - 1] += subLine;
            }
            String label = line[0] + " ";
            res[maxDim] += label;
        }
        return res;
    }

}
