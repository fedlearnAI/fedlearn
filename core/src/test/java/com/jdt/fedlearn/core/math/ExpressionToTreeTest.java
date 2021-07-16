package com.jdt.fedlearn.core.math;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExpressionToTreeTest {

    @Test
    public void testExpTree() {
        ExpressionToTree expressionToTree = new ExpressionToTree();
        ExpressionToTree.Node root = expressionToTree.expTree("45+23*56/2-5");
        assertEquals("45 + 23 * 56 / 2 - 5 ", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("45 + 23*56/2-5");
        assertEquals("45 + 23 * 56 / 2 - 5 ", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("45+23.4*56/(2-5)");
        assertEquals("45 + 23.4 * 56 / 2 - 5 ", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("-45+(-23.4)*56/(2-5)");
        assertEquals("-45 + -23.4 * 56 / 2 - 5 ", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("-45+(-23.4)*/56/(2-5)");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("*45+(-23.4)*56/(2-5)");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("45+(-23.4)*56/(2-5)-");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("45+(-23.4))*/56/(2-5)");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("(45+(-23.4)*56/(2-5)");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("45+(-23.4)@#*56/(2-5)");
        assertEquals("", expressionToTree.midOrderString(root));

        root = expressionToTree.expTree("12");
        assertEquals("12 ", expressionToTree.midOrderString(root));

    }
}