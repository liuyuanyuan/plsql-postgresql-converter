package ru.barsopen.plsqlconverter.ast.transforms;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.barsopen.plsqlconverter.Main;
import ru.barsopen.plsqlconverter.ast.DerivedSqlLexer;
import ru.barsopen.plsqlconverter.ast.DerivedSqlParser;
import ru.barsopen.plsqlconverter.util.ReflectionUtil;
/*
 * 
 */
public class AstParser {
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	/*
	 * inputContent: an single statement
	 * printTokens : 
	 * treeType: one of sql_script|statement|expression 
	 */
	public static ParseResult parseTreeFromString(String inputContent, boolean printTokens, String treeType)
			throws RecognitionException 
	{
		logger.debug("Enter: printTokens=" + printTokens + ", treeType=" + treeType);
		ANTLRStringStream input = new ANTLRStringStream(inputContent);
		DerivedSqlLexer l = new DerivedSqlLexer(input);
		CommonTokenStream cts = new CommonTokenStream(l);
		
		if (printTokens)
		{
			cts.fill();
			List<? extends Token> tokens = cts.getTokens();
			DerivedSqlParser p = new DerivedSqlParser(cts);
			String[] tokenNames = p.getTokenNames();
			for (Token t: tokens) {
				int type = t.getType();
				if (type != Token.EOF && t.getChannel() != Token.HIDDEN_CHANNEL) {
					String s = tokenNames[type];
					String tokenText = t.getText();
					System.out.printf("%s '%s' %d\n", s, tokenText, t.getChannel());
				}
			}
			System.exit(0);
		}
		
		DerivedSqlParser p = new DerivedSqlParser(cts);
		Object tree = ReflectionUtil.callMethod(ReflectionUtil.callMethod(p, treeType), "getTree");
	
		org.antlr.runtime.tree.Tree theTree = (org.antlr.runtime.tree.Tree)tree;
		ParseResult result = new ParseResult();
		result.tokens = new ArrayList<Token>();
		result.tokens.addAll(cts.getTokens());
		result.tree = theTree;
		result.lexerErrors = l.errors;
		result.parserErrors = p.errors;
		
		logger.debug("tree:line=" + theTree.getLine() + ", CharPositionInLine=" + theTree.getCharPositionInLine()
		+ ",text=" + theTree.getText() + ", childCount=" + theTree.getChildCount() + ",childIndex=" + theTree.getChildIndex());
		for (Token t : cts.getTokens())
		{
			logger.debug("token:line=" + t.getLine() + ", CharPositionInLine=" + t.getCharPositionInLine() 
			+ ", text="	+ t.getText() + ", type=" + t.getType());
		}	
		
		return result;
	}
}
