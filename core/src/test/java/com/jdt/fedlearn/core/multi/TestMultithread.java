package com.jdt.fedlearn.core.multi;

import org.testng.annotations.Test;

import java.util.concurrent.ForkJoinPool;

public class TestMultithread {
    @Test
    public void testProcessor(){
        System.out.println(Runtime.getRuntime().availableProcessors());
        System.out.println(ForkJoinPool.getCommonPoolParallelism());
    }
}
