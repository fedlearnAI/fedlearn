package com.jdt.fedlearn.tools.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class GetMethodUtil {


    /***
     * 根据类名,方法名和参数，查找方法，返回执行方法后的结果
     * @param className 完整类名
     * @param methodName 方法名
     * @param params 参数
     * @return 执行方法后的结果
     * @throws ClassNotFoundException 异常
     */
    public static Double getMethod(String className, String methodName, Object[] params) throws ClassNotFoundException {
        try {
            double result;
            Class<?> c = Class.forName(className);
            Method method = Arrays.stream(c.getMethods()).filter(i -> i.getName().equals(methodName) && i.getParameterTypes()[0].getName().equals("double")).findFirst().orElse(null);
            if (method != null) {
                // 判断该方法是否为static方法，如果不是需要newInstance
                if (Modifier.isStatic(method.getModifiers())) {
                    result = (double) method.invoke(method.getClass(), params);
                } else {
                    result = (double) method.invoke(c.newInstance(), params);
                }
            } else {
                throw new ClassNotFoundException("getMethod error");
            }
            return result;
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new ClassNotFoundException("getMethod error", e);
        }
    }

    /***
     * 根据类名,方法名和参数，查找方法，返回执行方法后的结果
     * @param className 完整类名
     * @param methodName 方法名
     * @param parameterType 参数类型
     * @param params 参数
     * @return 执行方法后的结果
     * @throws ClassNotFoundException 异常
     */
    public static Double getMethod(String className, String methodName, String parameterType, Object[] params) throws ClassNotFoundException {
        try {
            double result;
            Class<?> c = Class.forName(className);
            Method method = Arrays.stream(c.getMethods()).filter(i -> i.getName().equals(methodName) && i.getParameterTypes()[0].getName().equals(parameterType)).findFirst().orElse(null);
            if (method != null) {
                // 判断该方法是否为static方法，如果不是需要newInstance
                if (Modifier.isStatic(method.getModifiers())) {
                    result = (double) method.invoke(method.getClass(), params);
                } else {
                    result = (double) method.invoke(c.newInstance(), params);
                }
            } else {
                throw new ClassNotFoundException("getMethod error");
            }
            return result;
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new ClassNotFoundException("getMethod error", e);
        }
    }
}
