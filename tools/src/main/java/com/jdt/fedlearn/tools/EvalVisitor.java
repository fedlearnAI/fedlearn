package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.tools.antlrGenerate.ExprBaseVisitor;
import com.jdt.fedlearn.tools.antlrGenerate.ExprParser;
import com.jdt.fedlearn.tools.utils.GetMethodUtil;

import java.lang.invoke.WrongMethodTypeException;
import java.util.*;

/***
 * 语法树Visitor类
 * @author Peng Zhengyang
 */

public class EvalVisitor extends ExprBaseVisitor<Double> {

    // 保存赋值后的ID对应值
    private Map<String, Double> memory = new HashMap<>();
    private List<String> featuresName;
    private List<Double> dataList;
    private double[] dataArray;

    public EvalVisitor(List<String> featuresName) {
        this.featuresName = featuresName;
    }

    public EvalVisitor(List<String> featuresName, double[] dataArray) {
        this.featuresName = featuresName;
        this.dataArray = dataArray;
    }

    public EvalVisitor(List<String> featuresName, List<Double> dataList) {
        this.featuresName = featuresName;
        this.dataList = dataList;
    }

    public EvalVisitor() {
    }

    @Override
    public Double visitAssign(ExprParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        double value = visit(ctx.expr());
        memory.put(id, value);
        return value;
    }

    @Override
    public Double visitPrintExpr(ExprParser.PrintExprContext ctx) {
        Double value = visit(ctx.expr());

        return value;
    }

    @Override
    public Double visitDouble(ExprParser.DoubleContext ctx) {
        return Double.valueOf(ctx.DOUBLE().getText());
    }

    @Override
    public Double visitId(ExprParser.IdContext ctx) {
        String id = ctx.ID().getText();
        if (memory.containsKey(id)) {
            return memory.get(id);
        } else {
            Set<String> featuresSet = new HashSet<>(featuresName);
            if (featuresSet.contains(id)) {
                if (dataList != null) {
                    return dataList.get(featuresName.indexOf(id));
                } else if (dataArray != null) {
                    return dataArray[featuresName.indexOf(id)];
                } else {
                    throw new WrongMethodTypeException("no data");
                }
            } else
                throw new WrongMethodTypeException("feature ID error");
        }
    }

    @Override
    public Double visitMulDiv(ExprParser.MulDivContext ctx) {
        double left = visit(ctx.expr(0));
        double right = visit(ctx.expr(1));
        if (ctx.op.getType() == ExprParser.MUL) {
            return left * right;
        } else {
            return left / right;
        }
    }

    @Override
    public Double visitAddSub(ExprParser.AddSubContext ctx) {
        double left = visit(ctx.expr(0));
        double right = visit(ctx.expr(1));
        if (ctx.op.getType() == ExprParser.Add) {
            return left + right;
        } else {
            return left - right;
        }
    }

    @Override
    public Double visitNegative(ExprParser.NegativeContext ctx) {
        return -Double.parseDouble(ctx.DOUBLE().getText());
    }

    @Override
    public Double visitMethod(ExprParser.MethodContext ctx) {
        try {
            String id = ctx.ID().getText();
//            String[] list = id.split("\\.");
//            Method method = GetMethodUtil.getMethod("java.lang." + list[0], list[1]);
            String className = id.substring(0, id.lastIndexOf("."));
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            Double[] params = ctx.expr().stream().map(this::visit).toArray(Double[]::new);
            Double result = GetMethodUtil.getMethod(className, methodName, params);
            if (result == null) {
                throw new WrongMethodTypeException();
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new WrongMethodTypeException();
        }
    }

    @Override
    public Double visitParen(ExprParser.ParenContext ctx) {
        return visit(ctx.expr());
    }

    public void setDataList(List<Double> dataList) {
        this.dataList = dataList;
    }

    public void setDataArray(double[] dataArray) {
        this.dataArray = dataArray;
    }
}
