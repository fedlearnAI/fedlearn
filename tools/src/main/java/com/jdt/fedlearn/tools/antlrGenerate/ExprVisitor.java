// Generated from /home/wanghaihe/fl/12/fedlearn/tools/src/main/java/com/jdt/fedlearn/tools/Expr.g4 by ANTLR 4.9.1
package com.jdt.fedlearn.tools.antlrGenerate;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExprParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExprVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExprParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(ExprParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code printExpr}
	 * labeled alternative in {@link ExprParser#stat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrintExpr(ExprParser.PrintExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assign}
	 * labeled alternative in {@link ExprParser#stat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssign(ExprParser.AssignContext ctx);
	/**
	 * Visit a parse tree produced by the {@code blank}
	 * labeled alternative in {@link ExprParser#stat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlank(ExprParser.BlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code paren}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParen(ExprParser.ParenContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Negative}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegative(ExprParser.NegativeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MulDiv}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulDiv(ExprParser.MulDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AddSub}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSub(ExprParser.AddSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code double}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble(ExprParser.DoubleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Method}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod(ExprParser.MethodContext ctx);
	/**
	 * Visit a parse tree produced by the {@code id}
	 * labeled alternative in {@link ExprParser#}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(ExprParser.IdContext ctx);
}