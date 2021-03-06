package com.jdt.fedlearn.core.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>输入(x<sub>i</sub>, y<sub>i</sub>)的n对多项式的根，通过拉格朗日插值法可以求出多项式的系数，
 * 此处的拉格朗日插值法仅用于Freedman ID对齐算法中的{@code SolvePolynomial}阶段，仅需要输入Active方的数字型id
 * 即可获得系数，id无需排序</p>
 *
 * <p>因为应用在Freedman id对齐算法，经过简化，具体求解方式为计算多项式（x+a[0]）* (x+a[1]) * ... * (x+a[n-1])化简后的系数</p>
 *
 * <p>例如：当Active方id是{2, 3, 1}时，可以以下式方式创建{@code LagrangeInterpolation}对象
 * {@code LagrangeInterpolation li = new LagrangeInterpolation(new double[]{-2.0, -3.0, -1.0});}
 * </p>
 * <p>调用{@code li.generateCoefficients()}即可获得系数</p>
 */
public class LagrangeInterpolation {
    private static double[] BigIds;

    public LagrangeInterpolation(double[] BigIds) {
        this.BigIds = BigIds;
    }

    //多项式函数的系数递归求值。
    private static double getCoefficient(int n, int k) {
	/**
     * getCoefficient(n, k)表示n次多项式的k次幂前的系数和，返回一个int
     * 其中a此处为ids
     * getCoefficient(n, n-1) = a[0] + ... + a[n-1] = getCoefficient(n-1, n-2) + a[n-1]
     * getCoefficient(n, k) = getCoefficient(n-1, k-1) + getCoefficient(n-1, k) * a[n-1]
     * getCoefficient(n, 0) = a[0] * a[1] * ... * a[n] = getCoefficient(n-1, 0) * a[n-1]
     * 其中n为迭代次数，k为k次幂
     */
        if ((n > 0 && n <= BigIds.length) && (k >= 0 && k < BigIds.length)) {
            if (n == 1) {
                return BigIds[0];
            }
            if (k == 0) {
                return BigIds[n - 1] * (getCoefficient(n - 1, 0));
            }
            else if (k == n - 1) {
                return BigIds[n - 1] + (getCoefficient(n - 1, n - 2));
            }
            else {
                return getCoefficient(n - 1, k) * (BigIds[n - 1]) + getCoefficient(n - 1, k - 1);
            }
        } else {
            throw new UnsupportedOperationException("Wrong n and k, should satisfy（0<n<=arr.length&&0<=k<arr.length）");
        }
    }

    //输出多项式展开后的系数表现形式
    public double[] generateBigCoefficients() {
        List<Double> solution = new ArrayList<>();
        // 对于x^n次方(最高次方)来说系数确定为1
        solution.add(1.0);
        for (int i = BigIds.length - 1; i >= 0; i--) {
            solution.add(getCoefficient(BigIds.length, i));
        }
        return solution.stream().mapToDouble(i->i).toArray();
    }

}


