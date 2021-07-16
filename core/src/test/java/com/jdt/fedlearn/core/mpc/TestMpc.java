package com.jdt.fedlearn.core.mpc;
import com.jdt.fedlearn.core.entity.mpc.PartyA;
import com.jdt.fedlearn.core.entity.mpc.PartyB;
import com.jdt.fedlearn.core.research.mpc.MpcModel;


import java.io.UnsupportedEncodingException;
import java.util.*;

public class TestMpc {

    /**
     * Examples of secret sharing encryption, addition and multiplication without threshold
     */
    public void bill(int a,int b){
        int n = 3;
        MpcModel m = new MpcModel(n);
        m.millionaire(a,b);

    }

    public void easyTest() {
        //// 1�� Examples of addition
        double[] num1 = {115, 4, 55.7, 80.5};
        double[] num2 = {1, 5.5, 99, 0.4};
        int n = 3;
        MpcModel scheme = new MpcModel(n);
        double[] res = scheme.easysharing(num1,num2,3,"multiply");
        System.out.println("res:"+Arrays.toString(res));

    }


    /**
     * An example of secret sharing encryption with threshold
     */
    public void GF256Test(String b) throws UnsupportedEncodingException {
        int n = 3;
        MpcModel m = new MpcModel(n);
        m.gfsharing(b,3,3);
    }


    /**
     * 用于百万富翁比大小的算法流程实现
     * 具体算法流程可见文档 https://cf.jd.com/pages/viewpage.action?pageId=308536647
     */
    public void compare() {
        int MIN = 1;
        int MAX = 10000; //约定的秘密数字的大小范围
        PartyA a = new PartyA(9921, MIN, MAX);
        PartyB b = new PartyB(3098, MIN, MAX);
        //step1
        a.setPubilcKeyB(b.sendPublicKey(), b.sendN());
        long step1ASendB = a.columStep1();

        //step2
        long[] step2_list = b.step2List(step1ASendB);

        //step3
        b.setRandomNumLen(a.randomNumLen);
        long p = b.step3ChoseP();
        long[] step3_list = b.step3ModP(step2_list, p);

        //step4
        int result = a.step4Result(step3_list, p);
        if (result == 0) {
            System.out.println("a<=b");
        } else {
            System.out.println("a>b");
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        int a = 1;
        int b = 10;
        String s = "100";
        TestMpc m = new TestMpc();
        m.compare();
        m.bill(a,b);
        m.easyTest();
        m.GF256Test(s);
//        m.GF256Test();
    }
}
