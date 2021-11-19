// Generated from /home/wanghaihe/fl/12/fedlearn/tools/src/main/java/com/jdt/fedlearn/tools/Expr.g4 by ANTLR 4.9.1
package com.jdt.fedlearn.tools.antlrGenerate;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExprLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, DOUBLE=5, ID=6, NEWLINE=7, WS=8, MUL=9, 
		DIV=10, Add=11, SUB=12;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "DOUBLE", "ID", "NEWLINE", "WS", "MUL", 
			"DIV", "Add", "SUB"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'='", "'('", "','", "')'", null, null, null, null, "'*'", "'/'", 
			"'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, "DOUBLE", "ID", "NEWLINE", "WS", "MUL", 
			"DIV", "Add", "SUB"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public ExprLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Expr.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16K\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\6\6%\n\6\r\6\16"+
		"\6&\3\6\3\6\6\6+\n\6\r\6\16\6,\5\6/\n\6\3\7\6\7\62\n\7\r\7\16\7\63\3\b"+
		"\5\b\67\n\b\3\b\3\b\5\b;\n\b\3\t\6\t>\n\t\r\t\16\t?\3\t\3\t\3\n\3\n\3"+
		"\13\3\13\3\f\3\f\3\r\3\r\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23"+
		"\13\25\f\27\r\31\16\3\2\5\3\2\62;\7\2\60\60\62;C\\aac|\4\2\13\13\"\"\2"+
		"Q\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\3\33\3\2\2\2\5\35\3\2\2\2\7\37\3\2\2\2\t!\3\2\2\2\13$\3"+
		"\2\2\2\r\61\3\2\2\2\17:\3\2\2\2\21=\3\2\2\2\23C\3\2\2\2\25E\3\2\2\2\27"+
		"G\3\2\2\2\31I\3\2\2\2\33\34\7?\2\2\34\4\3\2\2\2\35\36\7*\2\2\36\6\3\2"+
		"\2\2\37 \7.\2\2 \b\3\2\2\2!\"\7+\2\2\"\n\3\2\2\2#%\t\2\2\2$#\3\2\2\2%"+
		"&\3\2\2\2&$\3\2\2\2&\'\3\2\2\2\'.\3\2\2\2(*\7\60\2\2)+\t\2\2\2*)\3\2\2"+
		"\2+,\3\2\2\2,*\3\2\2\2,-\3\2\2\2-/\3\2\2\2.(\3\2\2\2./\3\2\2\2/\f\3\2"+
		"\2\2\60\62\t\3\2\2\61\60\3\2\2\2\62\63\3\2\2\2\63\61\3\2\2\2\63\64\3\2"+
		"\2\2\64\16\3\2\2\2\65\67\7\17\2\2\66\65\3\2\2\2\66\67\3\2\2\2\678\3\2"+
		"\2\28;\7\f\2\29;\7=\2\2:\66\3\2\2\2:9\3\2\2\2;\20\3\2\2\2<>\t\4\2\2=<"+
		"\3\2\2\2>?\3\2\2\2?=\3\2\2\2?@\3\2\2\2@A\3\2\2\2AB\b\t\2\2B\22\3\2\2\2"+
		"CD\7,\2\2D\24\3\2\2\2EF\7\61\2\2F\26\3\2\2\2GH\7-\2\2H\30\3\2\2\2IJ\7"+
		"/\2\2J\32\3\2\2\2\13\2&,.\61\63\66:?\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}