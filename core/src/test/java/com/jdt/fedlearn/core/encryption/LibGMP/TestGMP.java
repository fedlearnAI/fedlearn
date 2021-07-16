package com.jdt.fedlearn.core.encryption.LibGMP;
//import com.sun.jna.Library;
//import com.sun.jna.Native;

import java.math.BigInteger;
import com.sun.jna.Library;
import com.sun.jna.Native;

import org.testng.annotations.Test;

public class TestGMP {
    public interface CLibrary extends Library {//example on how to load win's kernel32.dll library
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("kernel32", CLibrary.class);
        void Beep(int freq, int duration);//we use the beep function as example
    }

    @Test
    public static void main() {
        //CLibrary.INSTANCE.Beep(400,1000);//beep at 400 Hz for 1000 ms

        int count = 1000;
        long start, end;

                BigInteger a = new BigInteger("1234567890123456789012345678901234567890123456789012345678901234567890");
        BigInteger b = new BigInteger("2345678901234567890123456789012345678901234567890123456789012345678901");
        BigInteger exp = new BigInteger("10");
        BigInteger r;

        r = GMP.add(a, b);
        System.out.println(a + " + " + b + " = " + r);
        r = GMP.subtract(a, b);
        System.out.println(a + " - " + b + " = " + r);
        r = GMP.subtract(b, a);
        System.out.println(b + " - " + a + " = " + r);
        r = GMP.multiply(a, b);
        System.out.println(a + " * " + b + " = " + r);
        r = GMP.divide(b, a);
        System.out.println(b + " / " + a + " = " + r);
        r = GMP.remainder(b, a);
        System.out.println(b + " % " + a + " = " + r);
        r = GMP.divide(a, b);
        System.out.println(a + " / " + b + " = " + r);
        r = GMP.remainder(a, b);
        System.out.println(a + " % " + b + " = " + r);
        r = GMP.gcd(a, b);
        System.out.println("gcd(" + a + ", " + b + ") = " + r);

        // addMod
        BigInteger aa = a.multiply(a);
        start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            r = aa.add(b).mod(b);
        }
        end = System.nanoTime();
        System.out.println("Add mod of BigInteger elapsed time = " + (end - start) / 1e6 + "ms");
        start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            r = GMP.addMod(aa, b, b);
        }
        end = System.nanoTime();
        System.out.println("Add mod of LibGMP elapsed time = " + (end - start) / 1e6 + "ms");


        //BigInteger d = new BigInteger("51235111011101010101111101030313131313131313131313");
        //System.out.println(GMP.isProbablePrime(d, 6) + "  " + d.isProbablePrime(6));
    }

    public void testadd() {

    }

}