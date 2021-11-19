package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.tools.antlrGenerate.ExprLexer;
import com.jdt.fedlearn.tools.antlrGenerate.ExprParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/***
 * 语法树解析
 * @author Peng Zhengyang
 */
public class ExprAnalysis {

    // 解析后的语法树
    private Map<String, AtomicReference<ParseTree>> parseTreeMap = new ConcurrentHashMap<>();

    /***
     * 初始化新的 表达式-特征名 返回对应token
     * 初始化后可使用token和特征值 计算表达式结果
     * @param expr 表达式
     * @param featuresName 特征名
     * @return token
     */
    public String init(String expr, List<String> featuresName) throws NoSuchElementException{
        // 通过 antlr 解析表达式
        ExprLexer lexer = new ExprLexer(CharStreams.fromString(expr + "\n"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExprParser parser = new ExprParser(tokens);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ExprErrorListener.INSTANCE);
        parser.removeErrorListeners();
        parser.addErrorListener(ExprErrorListener.INSTANCE);
        ParseTree tree = parser.prog();
        AtomicReference<ParseTree> parseTree = new AtomicReference<>();
        parseTree.lazySet(tree);
        AtomicReference<EvalVisitor> visitor = new AtomicReference<>();
        visitor.lazySet(new EvalVisitor(featuresName));
        Random random = new Random();
        String newToken = String.valueOf(new Date().getTime() + random.nextInt());
        parseTreeMap.put(newToken, parseTree);
        return newToken;
    }

    // 释放token对应资源
    public void close(String token) {
        parseTreeMap.get(token).lazySet(null);
        parseTreeMap.remove(token);
    }

    // 清空所有
    public void closeAll() {
        parseTreeMap.clear();
    }

    /***
     *
     * @param token 表达式和特征列名的对应token
     * @param featuresValue 特征对应值
     * @return 表达式结算结果
     */
    public Double expression(String token, double[] featuresValue, List<String> featuresName) {
        if (parseTreeMap.containsKey(token)) {
            EvalVisitor evalVisitorMap = new EvalVisitor(featuresName, featuresValue);
            return evalVisitorMap.visit(parseTreeMap.get(token).get());
        } else {
            throw new NoSuchElementException("no this token");
        }
    }

    /***
     *
     * @param token 表达式和特征列名的对应token
     * @param featuresValue 特征对应值
     * @return 表达式结算结果
     */
    public Double expression(String token, List<Double> featuresValue, List<String> featuresName) {
        if (parseTreeMap.containsKey(token)) {
            EvalVisitor evalVisitorMap = new EvalVisitor(featuresName, featuresValue);
            return evalVisitorMap.visit(parseTreeMap.get(token).get());
        } else {
            throw new NoSuchElementException("no this token");
        }
    }


}
