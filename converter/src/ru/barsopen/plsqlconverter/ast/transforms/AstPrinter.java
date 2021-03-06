package ru.barsopen.plsqlconverter.ast.transforms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.barsopen.plsqlconverter.Main;
import ru.barsopen.plsqlconverter.ast.DerivedSqlPrinter;
import ru.barsopen.plsqlconverter.util.ReflectionUtil;

public class AstPrinter {

	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static String prettyPrint(org.antlr.runtime.tree.Tree tree) {
		StringBuilder sb = new StringBuilder();
		prettyPrint(tree, sb, 0);
		return sb.toString();
	}

	static void printIndent(StringBuilder sb, int indent) {
		for (int i = 0; i < indent; ++i) {
			sb.append("  ");
		}
	}
	
	private static void prettyPrint(org.antlr.runtime.tree.Tree tree, StringBuilder sb, int indent) {
		sb.append('(');
		String nodeText = prettyPrintNodeTag(tree);
		sb.append(nodeText);
		if (tree.getChildCount() == 1 && tree.getChild(0).getChildCount() == 0) {
			sb.append(" ");
			org.antlr.runtime.tree.Tree childNode = tree.getChild(0);
			String childNodeText = prettyPrintNodeTag(childNode);
			sb.append(childNodeText);
		} else {
			for (int i = 0; i < tree.getChildCount(); ++i) {
				org.antlr.runtime.tree.Tree childNode = tree.getChild(i);
				sb.append("\n");
				printIndent(sb, indent + 1);
				if (childNode.getChildCount() == 0) {
					String childNodeText = prettyPrintNodeTag(childNode);
					sb.append(childNodeText);
				} else {
					prettyPrint(childNode, sb, indent + 1);
				}
			}
		}
		sb.append(")");
	}
	
	private static String prettyPrintNodeTag(Tree tree) {
		int ttype = tree.getType();
		String tokenName = ttype >= 0 && ttype < AstUtil.tokenNames.length ? AstUtil.tokenNames[ttype] : "";
		String text = tree.getText();
		if (tokenName.equals(text)) {
			return tokenName;
		} else {
			//int ntype = Arrays.asList(PLSQLParser.tokenNames).indexOf(tree.getText());
			return String.format("%s[%s]", tokenName, text);
		}
	}

	public static PrintResult printTreeToOracleString(org.antlr.runtime.tree.Tree theTree, String treeType)
			throws IOException, RecognitionException {
	    logger.debug("Enter");
	
		DerivedSqlPrinter printer = new DerivedSqlPrinter(new CommonTreeNodeStream(theTree));
	
		printer.setTemplateLib(oracleStg);
		printer.gPLSQLPrinter_DDL.setTemplateLib(oracleStg);
		StringTemplate printedTemplate = (StringTemplate)ReflectionUtil.getField(ReflectionUtil.callMethod(printer, treeType), "st");
		String printed = printedTemplate.toString();
		PrintResult result = new PrintResult();
		result.printErrors = printer.errors;
		result.text = printed;
		return result;
	}

	public static PrintResult printTreeToPostgresqlString(org.antlr.runtime.tree.Tree theTree, String treeType)
			throws IOException, RecognitionException {
		logger.debug("Enter:treeType=" + treeType);
		logger.debug("theTree:text=" + theTree.getText() + ", Line=" + theTree.getLine() + ", CharPositionInLine=" + theTree.getCharPositionInLine());
		//CommonTreeNodeStream stream = new CommonTreeNodeStream(theTree);
		DerivedSqlPrinter printer = new DerivedSqlPrinter(new CommonTreeNodeStream(theTree));
		
		printer.setTemplateLib(postgresqlStg);
		printer.gPLSQLPrinter_DDL.setTemplateLib(postgresqlStg);
		StringTemplate printedTemplate = (StringTemplate)ReflectionUtil.getField(ReflectionUtil.callMethod(printer, treeType), "st");
		String printed = printedTemplate.toString();
		PrintResult result = new PrintResult();
		result.printErrors = printer.errors;
		result.text = printed;
		
		logger.debug("Return:" + result.text);
		return result;
	}
	
	public static StringTemplateGroup oracleStg = getOracleSTG();
	public static StringTemplateGroup postgresqlStg = getPostgresqlSTG();
	
	public static StringTemplateGroup getOracleSTG() {
		logger.debug("Enter: " + (new File("PLSQLPrinterTemplates.stg")).exists());
		//ClassLoader.getResourceAsStream()鏃犺瑕佹煡鎵剧殑璧勬簮鍓嶉潰鏄惁甯�'/'閮戒細浠巆lasspath鐨勬牴璺緞涓嬫煡鎵俱��
		//浠ヤ笅鍒欎細鍦ˋstPrinter鎵�鍦ㄥ寘涓嬫煡鎵剧浉搴旂殑璧勬簮;濡傛灉杩欎釜name鏄互'/'寮�澶寸殑锛岄偅涔堝氨浼氫粠classpath鐨勬牴璺緞涓嬪紑濮嬫煡鎵俱��
		try (InputStream templateInputStream = new FileInputStream(new File("PLSQLPrinterTemplates.stg"))){
		//(InputStream templateInputStream = AstPrinter.class.getClassLoader().getResourceAsStream("PLSQLPrinterTemplates.stg")) {
			logger.debug("templateInputStream=" + templateInputStream);
			StringTemplateGroup templateGroup = new StringTemplateGroup(new InputStreamReader(templateInputStream, Charset.forName("UTF-8")), AngleBracketTemplateLexer.class);
			logger.debug("rootDir=" + templateGroup.getRootDir() + ", name=" + templateGroup.getName()
			+ templateGroup.getClass());
			return templateGroup;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static StringTemplateGroup getPostgresqlSTG() {
		try (InputStream templateInputStream = new FileInputStream(new File("PLPGSQLPrinterTemplates.stg"))){
		//(InputStream templateInputStream = AstPrinter.class.getClassLoader().getResourceAsStream("PLPGSQLPrinterTemplates.stg")) {
			StringTemplateGroup templateGroup = new StringTemplateGroup(new InputStreamReader(templateInputStream, Charset.forName("UTF-8")), AngleBracketTemplateLexer.class);
			templateGroup.setSuperGroup(getOracleSTG());
			return templateGroup;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
