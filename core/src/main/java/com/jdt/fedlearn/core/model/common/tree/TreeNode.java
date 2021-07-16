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

package com.jdt.fedlearn.core.model.common.tree;

import com.jdt.fedlearn.core.entity.ClientInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

//        about TreeNode.index, an example:
//                    1
//           2        3       4
//        5  6  7   8 9 10  11 12 13
//   14 15 16                        40
//
//        index of the root node is 1,
//        its left child's index is 3*root.index-1,
//        its right child's index is 3*root.index+1,
//        the middle child is nanChild, its index is 3*root.index

public class TreeNode implements Serializable {
    //实例空间，即属于该节点的样本id的集合
    public int[] instanceSpace;
    //
    public double gain;
    public int index;
    public int depth; // 当前节点处于整棵树的第几层
    public int featureDim;
    public boolean isLeaf;
    public int numSample;
    //the gradient/hessian sum of the samples fall into this tree node
    public double Grad;
    public double Hess;
    //for split finding, record the gradient/hessian sum of the left
    //when split finding, record the best threshold, gain, missing value's branch for each feature
    private double[] bestThresholds;
    private double[] bestGains;
    private double[] bestNanGoTo;
    public double nanGoTo;
    //some data fall into this tree node
    //gradient sum, hessian sum of those with missing value for each feature
    public double[] GradMissing;
    public double[] HessMissing;
    //internal node
    public int splitFeature;

    public int recordId;
    public ClientInfo client;

    public List<Double> splitLeftChildCatvalue;
    public TreeNode nanChild;
    public TreeNode leftChild;
    public TreeNode rightChild;
    //leaf node
    public double leafScore;
    //for categorical feature,store (col,(value,(gradSum,hessSum)))
//    public HashMap<Integer, HashMap<Integer, double[]>> catFeatureColValueGH = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>> catFeatureColLeftcatvalue = new HashMap<>();

    public TreeNode() {
    }

    public TreeNode(int index, double leafScore) {
        //leaf node construct
        this.isLeaf = true;
        this.index = index;
        this.leafScore = leafScore;
    }

    public TreeNode(int index, int splitFeature, ClientInfo client, int recordId, double nanGoTo) {
        //internal node construct,numeric split feature
        this.isLeaf = false;
        this.index = index;
        this.splitFeature = splitFeature;
        this.client = client;
        this.recordId = recordId;
        this.nanGoTo = nanGoTo;
    }

    public TreeNode(int index, int splitFeature, ArrayList<Double> splitLeftChildCatvalue, double nanGoTo) {
        //internal node construct,categorical split feature
        this.isLeaf = false;
        this.index = index;
        this.splitFeature = splitFeature;
        this.splitLeftChildCatvalue = splitLeftChildCatvalue;
        this.nanGoTo = nanGoTo;
    }


    public TreeNode(int index, int depth, int featureDim, boolean isLeaf) {
        this.index = index;
        this.depth = depth;
        this.featureDim = featureDim;
        this.isLeaf = isLeaf;
        this.bestThresholds = new double[featureDim];
        this.bestGains = new double[featureDim];
        this.bestNanGoTo = new double[featureDim];
        this.GradMissing = new double[featureDim];
        this.HessMissing = new double[featureDim];

        Arrays.fill(this.bestGains, -Double.MAX_VALUE);
    }

    public void GradAdd(double value) {
        Grad += value;
    }

    public void HessAdd(double value) {
        Hess += value;
    }

    public void numSampleAdd(double value) {
        numSample += value;
    }

    public void GradSetter(double value) {
        Grad = value;
    }

    public void HessSetter(double value) {
        Hess = value;
    }

    public void updateBestSplit(int col, double threshold, double gain, double nanGoTo) {
        if (gain > bestGains[col]) {
            bestGains[col] = gain;
            bestThresholds[col] = threshold;
            bestNanGoTo[col] = nanGoTo;
        }
    }

    public void setCategoricalFeatureBestSplit(int col, ArrayList<Integer> leftChildCatvalue, double gain, double nanGoTo) {
        bestGains[col] = gain;
        bestNanGoTo[col] = nanGoTo;
        catFeatureColLeftcatvalue.put(col, leftChildCatvalue);
    }

    public ArrayList<Double> fetchBestFeatureThresholdGain() {
        int bestFeature = 0;
        double maxGain = -Double.MAX_VALUE;
        for (int i = 0; i < featureDim; i++) {
            if (bestGains[i] > maxGain) {
                maxGain = bestGains[i];
                bestFeature = i;
            }
        }
        //consider categorical feature
        ArrayList<Double> ret = new ArrayList<>();
        ret.add((double) bestFeature);
        ret.add(maxGain);
        ret.add(bestNanGoTo[bestFeature]);
        if (catFeatureColLeftcatvalue.containsKey(bestFeature)) {
            for (double catvalue : catFeatureColLeftcatvalue.get(bestFeature)) {
                ret.add(catvalue);
            }
        } else {
            ret.add(bestThresholds[bestFeature]);
        }
        return ret;
    }

    public void internalNodeSetterSecure(double nanGoTo, TreeNode nanChild,
                                         TreeNode leftChild, TreeNode rightChild, boolean isLeaf) {
//        this.splitFeature = (int) feature;
//        this.splitThreshold = threshold;
        this.nanGoTo = nanGoTo;
        this.nanChild = nanChild;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.isLeaf = isLeaf;
        cleanUp();
    }

    public void internalNodeSetter(double feature, List<Double> leftChildCatvalue, double nanGoTo, TreeNode nanChild,
                                   TreeNode leftChild, TreeNode rightChild, boolean isLeaf) {
        this.splitFeature = (int) feature;
        this.splitLeftChildCatvalue = leftChildCatvalue;
        this.nanGoTo = nanGoTo;
        this.nanChild = nanChild;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.isLeaf = isLeaf;
        cleanUp();
    }

    public void leafNodeSetter(double leafScore, boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.leafScore = leafScore;
        cleanUp();
    }

    private void cleanUp() {
        //release memory
        bestThresholds = null;
        bestGains = null;
        bestNanGoTo = null;
//        catFeatureColValueGH = null;
        catFeatureColLeftcatvalue = null;
    }

    public String infoTreeNode() {
        String info = "";
        if (this.isLeaf) {
            info = String.format("Leaf Node %d depth %d with %d samples. Father node: %d ", index, depth, instanceSpace.length, (index + 1) % 3 == 0 ? ((index + 1) / 3) : ((index - 1) / 3));
        } else {
            info = String.format("Interval Node %d depth %d: Father node: %d gain:%f on feature %d port %d ", index, depth, (index + 1) % 3 == 0 ? ((index + 1) / 3) : ((index - 1) / 3), gain, splitFeature, client.getPort());
        }
        return info;
    }

    public String[] picTreeNode(int maxDepth) {
        String[] node = new String[3];
        int spacecnt2 = ((int) Math.pow(2, maxDepth - depth + 3d) - 6) >> 1;

        String cnt2 = String.format("%d s", spacecnt2);

        node[1] = String.format("%s N.%04d %s", cnt2, index, cnt2);
        if (this.isLeaf) {
            node[0] = cnt2 + " Leaf " + cnt2;
            node[2] = String.format("%s %4d %s", cnt2, instanceSpace.length, cnt2);
        } else {
            node[0] = String.format("%s Dept%2d %s", cnt2, depth, cnt2);
            node[2] = String.format("%s f%02d %s", cnt2, splitFeature, cnt2);
//            node[3] = String.format(cnt2 + "%6.4f" + cnt2, splitThreshold);
        }
        return node;
    }
}