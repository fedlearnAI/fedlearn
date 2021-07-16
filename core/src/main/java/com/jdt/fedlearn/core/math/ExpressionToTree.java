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

package com.jdt.fedlearn.core.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 表达式转二叉树
 */
public class ExpressionToTree {
    private static final Logger logger = LoggerFactory.getLogger(ExpressionToTree.class);
    /**
     * 字符串表达式转化为二叉树
     *
     * @param str 表达式字符串
     * @return 转化后的二叉树根节点
     */
    public Node expTree(String str) {
        Map<String, Integer> prio = new HashMap<>();
        prio.put("(", 1);
        prio.put("+", 2);
        prio.put("-", 2);
        prio.put("*", 3);
        prio.put("/", 3);
        Deque<String> ops = new ArrayDeque<>();
        Deque<Node> stack = new ArrayDeque<>();
        Deque<String> stackError = new ArrayDeque<>();
        String digit = "";
        // flagSub为0时"-"为减法运算，flagSub为1时为负数前缀
        int flagSub = 1;
        int preIsNotDigit = 0;
        List<String> stringListStart = Arrays.asList("+", "*", "/", ")", ".");
        List<String> stringListEnd = Arrays.asList("+", "-", "*", "/", "(", ".");
        List<String> stringListAll = Arrays.asList("+", "-", "*", "/", ")", "(", ".");
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);

            // 合法性校验
            if (ch == ' ') {
                continue;
            }
            if ((i == 0) && (stringListStart.contains(ch + ""))) {
                logger.info("首位非法");
                return null;
            }
            if ((i == str.length() - 1) && (stringListEnd.contains(ch + ""))) {
                logger.info("末位非法");
                return null;
            }
            if (!Character.isDigit(ch) && !stringListAll.contains(ch + "")) {
                logger.info("存在特殊字符");
                return null;
            }
            if (ch == '(') {
                stackError.push(ch + "");
            }
            if (ch == ')') {
                if (stackError.size() == 0) {
                    logger.info("括号不匹配");
                    return null;
                }
                stackError.pop();
            }

            // 转化
            if (ch == '(') {
                ops.push(ch + "");
                flagSub = 1;
                preIsNotDigit = 1;
            } else if (Character.isDigit(ch)) {
                digit += ch + "";
                flagSub = 0;
                preIsNotDigit = 0;
            } else if (ch == ')') {
                if (!"".equals(digit)) {
                    stack.push(new Node(digit));
                }
                if (preIsNotDigit == 1) {
                    logger.info("连续符号错误");
                    return null;
                }
                digit = "";
                flagSub = 0;
                while (ops.peek() != null && !"(".equals(ops.peek())) {
                    combine(ops, stack);
                    if (ops.size() == 0) {
                        logger.info("括号不匹配");
                        return null;
                    }
                }
                // pop掉左括号
                ops.pop();
                preIsNotDigit = 0;
            } else if (ch == '.') {
                digit += ".";
                preIsNotDigit = 1;
            } else if (flagSub == 1 && ch == '-') {
                digit += "-";
                flagSub = 0;
                preIsNotDigit = 1;
            } else {
                if (!"".equals(digit)) {
                    stack.push(new Node(digit));
                }
                if (preIsNotDigit == 1) {
                    logger.info("存在连续计算符号");
                    return null;
                }
                preIsNotDigit = 1;
                digit = "";
                flagSub = 1;
                while (!ops.isEmpty() && prio.get(ops.peek()) > prio.get(ch + "")) {
                    combine(ops, stack);
                }
                ops.push(ch + "");
            }
        }
        if (stackError.size() > 0) {
            logger.info("括号不匹配");
            return null;
        }
        if (!"".equals(digit)) {
            stack.push(new Node(digit));
        }
        while (stack.size() > 1) {
            combine(ops, stack);
        }
        return stack.peek();
    }

    /**
     * 节点合并为树
     */
    private void combine(Deque<String> ops, Deque<Node> stack) {
        Node root = new Node(ops.pop());
        // 先pop的是右孩子，然后是左孩子
        root.right = stack.pop();
        root.left = stack.pop();
        stack.push(root);
    }

    /**
     * 中序遍历
     *
     * @param node 根结点
     * @return 中序遍历对应的字符串
     */
    public static String midOrderString(Node node) {
        StringBuilder resBuilder = new StringBuilder();
        Deque<Node> stack = new ArrayDeque<>();
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            if (!stack.isEmpty()) {
                node = stack.pop();
                resBuilder.append(node.val).append(" ");
                node = node.right;
            }
        }
        String res = resBuilder.toString();
        return res;
    }

    /**
     * 节点类
     */
    public static class Node {
        public String val;
        public Node left, right;

        public Node(String val) {
            this.val = val;
        }
    }
}