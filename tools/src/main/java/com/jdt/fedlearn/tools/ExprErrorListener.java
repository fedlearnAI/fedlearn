package com.jdt.fedlearn.tools;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.NoSuchElementException;

/***
 * 语法树错误监听
 * @author Peng Zhengyang
 */
public class ExprErrorListener extends BaseErrorListener {
    public static ExprErrorListener INSTANCE = new ExprErrorListener();
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object object, int line, int position, String msg,
                            RecognitionException e) {
        throw new NoSuchElementException("line:" + line + " column:" + position + " " + msg);
    }
}