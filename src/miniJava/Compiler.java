package miniJava;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecker;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {	
	
	static boolean 
			DEBUG_TOOLS = true,
			DISPLAY_AST = false,
			DEBUGGING = false;
	
	public static void main(String[] args){
		InputStream input = null;
		try {
			input = new FileInputStream(new File(args[0]));
		} catch(Exception e) {
			System.out.println("Input file not found");
			System.exit(3);
		}
		
		Scanner scanner = new Scanner(input);
		Parser parser = new Parser(scanner);
		AST tree = parser.parse();
		ASTDisplay display = new ASTDisplay();
		
		if(tree == null) {
			System.out.println("Invalid miniJava Program");
			System.exit(4);
		} else {
			if(DISPLAY_AST) display.showTree(tree);
		}
		
		Identification identifier = new Identification((Package) tree);		
		if(!identifier.identify()) System.exit(4);
		
		TypeChecker typer = new TypeChecker((Package) tree);
		if(!typer.typify()) System.exit(4);
		
		CodeGenerator gen = new CodeGenerator((Package) tree);
		gen.generateCode();
		
		String filename = args[0].split("\\.")[0];
		ObjectFile of = new ObjectFile(filename+".mJAM");
		if(of.write()) {
			System.out.println("Object file creation failed.");
			System.exit(4);
		}
		
/********************************************* DEBUGGING *********************************************/
		if(!DEBUG_TOOLS) return;
		
		Disassembler d = new Disassembler(filename+".mJAM");
		if(d.disassemble()) {
			System.out.println("Diassembly failed.");
		}
		
		if(DEBUGGING)
			Interpreter.debug(filename+".mJAM", filename+".asm");
		else
			Interpreter.interpret(filename+".mJAM");
	}
}

/**
 * AST Changes
 * 
 * - Added NullLiteral node to AST
 * - Added Declaration field to Identifier
 * - Added Declaration field to Reference
 * 
 * - Added TypeKind null
 * 
 * - Added private length field to ArrayType
 * - - Readable through public method
 * 
 * - Added RuntimeEntity field to Declaration
 * 
 * - Added boolean "isMain" to MethodDecl
 * 
 * Static Field Initialization
 * - Added Expression field to FieldDecl
 * - Added a new constructor to FieldDecl that takes in Expression
 * - If the Expression is not null, it is displayed by ASTDisplay
 * 
 * Parameterize Constructor
 * - Added MethodDecl field to ClassDecl class
 * -- The MethodDecl represents the constructors declaration
 * - Added ExprList to NewObjectExpr class
 * -- The ExprList is a list of arguments passed to the constructor
 * - Updated ASTDisplay to play arguments passed to constructor
 */
