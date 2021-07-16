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

import com.jdt.fedlearn.core.parameter.FgbParameter;

import java.io.Serializable;
import java.util.*;

public class Tree implements Serializable {
    private TreeNode root;
    /** 目前所有需要分裂的点的队列 */
    private Queue<TreeNode> aliveNodes = new LinkedList<>();
    /** number of tree node of this tree */
    public int nodesCnt = 0;
    /** number of nan tree node of this tree */
    public int nanNodesCnt = 0;

    public Tree() {
    }

    public Tree(TreeNode root) {
        this.root = root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public Queue<TreeNode> getAliveNodes() {
        return aliveNodes;
    }

    public static double calculateLeafScore(double G, double H, double lambda) {
        //According to xgboost, the leaf score is : - G / (H+lambda)
        return -G / (H + lambda);
    }

    /**
     *
     * @param GLeft
     * @param HLeft
     * @param GTotal
     * @param HTotal
     * @param p
     * @return
     */
    public static double calculateSplitGain(double GLeft, double HLeft, double GTotal, double HTotal, FgbParameter p) {
        //According to xgboost, the scoring function is:
        //     gain = 0.5 * (GL^2/(HL+lambda) + GR^2/(HR+lambda) - (GL+GR)^2/(HL+HR+lambda)) - gamma
        //this gain is the loss reduction, We want it to be as large as possible.
        double gamma = p.getGamma();
        double lambda = p.getLambda();
        double GRight = GTotal - GLeft;
        double HRight = HTotal - HLeft;

        //if we let those with missing value go to a nan child
        // TODO 这里默认放置在NULL中，XGB处理缺失值，一般默认放置到右分支
        double gain_1 = 0.5 * (
                Math.pow(GLeft, 2) / (HLeft + lambda)
                        + Math.pow(GRight, 2) / (HRight + lambda)
                        - Math.pow(GTotal, 2) / (HTotal + lambda)) - gamma;


        return gain_1;
    }

    public TreeNode getRoot() {
        return root;
    }
}

