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

import java.io.Serializable;

public class InferTreeNode implements Serializable {
    private int index;
    private boolean isLeaf;
    // function = 1: x < split, goes left; function = 0: x < split, goes right
    // function = -1: dummy nodes exist before this leaf
    private int function;
    private double[] cast;
    private int nanGoTo;

    private String splitFeature;
    private double splitValue;

    private InferTreeNode leftChild;
    private InferTreeNode rightChild;
    //leaf node
    private double leafScore;
    private InferTreeNode parent;
    private int depth;

    public InferTreeNode() {
    }

    public InferTreeNode(int depth, int index, double leafScore) {
        //leaf node construct
        this.depth = depth;
        this.isLeaf = true;
        this.function = 1;
        this.index = index;
        this.leafScore = leafScore;
    }

    public InferTreeNode(int depth, int index) {
        this.depth = depth;
        this.index = index;
        this.function = 1;
    }

    public InferTreeNode(int depth, int index, String splitFeature, double splitValue) {
        //internal node construct,numeric split feature
        this.depth = depth;
        this.isLeaf = false;
        this.index = index;
        this.function = 1;
        this.splitFeature = splitFeature;
        this.splitValue = splitValue;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public double getLeafScore() {
        return leafScore;
    }

    public String getSplitFeature() {
        return splitFeature;
    }

    public double getSplitValue() {
        return splitValue;
    }

    public int goesToLeft() {
        return function;
    }

    public int getIndex() {
        return index;
    }

    public InferTreeNode getLeftChild() {
        return leftChild;
    }

    public InferTreeNode getRightChild() {
        return rightChild;
    }

    public InferTreeNode getParent() {
        return parent;
    }

    public double[] getCast() {
        return cast;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public void setAsLeaf(double leafScore) {
        isLeaf = true;
        this.leafScore = leafScore;
    }

    public void setLeftChild(InferTreeNode leftChild) {
        this.leftChild = leftChild;
    }

    public void setRightChild(InferTreeNode rightChild) {
        this.rightChild = rightChild;
    }

    public void setParent(InferTreeNode parent) {
        this.parent = parent;
    }

    public void setSplit(String splitFeature, double splitValue) {
        this.splitFeature = splitFeature;
        this.splitValue = splitValue;
    }

    public void setCast(double[] cast) {
        this.cast = cast;
    }
}
