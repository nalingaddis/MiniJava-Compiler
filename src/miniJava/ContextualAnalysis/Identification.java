package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Identification {
	Package ast;
	IdentificationTable idTable = new IdentificationTable();
	String className;
	boolean staticMember = false;
	String varDeclName = null;
	boolean hasMain = false;
	
	public Identification(Package ast) {
		this.ast = ast;
	}
	
	public boolean identify(){
		try {
			this.traverse();
			return true;
		} catch(IdentificationException e) {
			System.out.println("*** line "+e.posn.line+": Identification Error - "+e.msg);
			return false;
		}
	}
	
	private void reportError(SourcePosition posn, String msg) throws IdentificationException{
		throw new IdentificationException(posn, msg);
	}
	
	private void idType(TypeDenoter type) throws IdentificationException{
		//if we see a class type or array type
		if(type instanceof ClassType ||
				type instanceof ArrayType){
			
			Identifier id = null;
			Declaration decl;
			
			if(type instanceof ClassType) {
				id = ((ClassType)type).className;
			} else if(((ArrayType)type).eltType instanceof ClassType) {
				id = ((ClassType)((ArrayType)type).eltType).className;
			}
			
			if(id!=null) {
				decl = idTable.getClass(id.spelling);
				if(decl == null) {
					reportError(type.posn,"Unknown class type \'"+id.spelling+"\'");
				}
				id.decl = decl;
			}
		}
	}
	
	private void idRef(Reference _ref) throws IdentificationException{
		if(_ref instanceof IdRef) {
			IdRef ref = (IdRef) _ref;
			
			Declaration decl = idTable.get(ref.id.spelling);
			
			if(decl != null) {
				
				if(decl instanceof MemberDecl) {
					MemberDecl memberDecl = (MemberDecl) decl;
					if(memberDecl.isStatic != staticMember) reportError(ref.posn,"Unstatic reference of static member");
				}
				
				ref.id.decl = decl;
				ref.decl = decl;
				
				if(ref.decl.name == varDeclName)
					reportError(ref.posn,"Variable declaration cannot reference variable being declared");
				
			} else {
				reportError(ref.posn,"Unknown identifier \'"+ref.id.spelling+"\'");
			}
		}
		
		else if(_ref instanceof ThisRef) {
			ThisRef ref = (ThisRef) _ref;
			
			if(staticMember) reportError(ref.posn,"Static reference of nonstatic value");
			
			ref.decl = idTable.getClass(className);
		} 
		
		else if(_ref instanceof QualRef) {
			QualRef ref = (QualRef) _ref;
			
			idRef(ref.ref);
			
			String className = "";
			boolean isStatic = false, isPrivate = false;
			
			if(ref.ref.decl instanceof ClassDecl) {
				className = ref.ref.decl.name;
				isStatic = !(ref.ref instanceof ThisRef);
			} else  if (ref.ref.decl.type instanceof ClassType){
				className = ((ClassType)(ref.ref.decl.type)).className.spelling;				
			} else if(ref.ref.decl.type instanceof ArrayType && ref.id.spelling.equals("length")){
				ref.decl = ref.ref.decl;
				return;
			} else {
				reportError(ref.posn, "Invalid qualifing reference \'"+ref.ref.decl.name+"\'");
			}
			
			isPrivate = className.equals(this.className);
			
			MemberDecl decl = (MemberDecl) idTable.getMember(className, ref.id.spelling);
						
			if(
				decl != null && //id exists
				((decl.isPrivate && isPrivate) || !decl.isPrivate) && //id is accessible
				((!decl.isStatic && !isStatic) || decl.isStatic)) //id is static
			{
				ref.id.decl = idTable.getMember(className, ref.id.spelling);
				ref.decl = ref.id.decl;
			} else {
				reportError(ref.posn,"Reference \'"+ref.id.spelling+"\' not accessible");
			}
			
		}
		
		else {
			reportError(_ref.posn,"");
		}
	}
	
	private void idExp(Expression _exp) throws IdentificationException{
		if(_exp instanceof BinaryExpr) {
			BinaryExpr exp = (BinaryExpr) _exp;
			
			idExp(exp.left);
			idExp(exp.right);
		}
		
		else if(_exp instanceof CallExpr) {
			CallExpr exp = (CallExpr) _exp;
			
			idRef(exp.functionRef);
			for(Expression exp_ : exp.argList) {
				idExp(exp_);
			}
		}
		
		else if(_exp instanceof IxExpr) {
			IxExpr exp = (IxExpr) _exp;
			
			idRef(exp.ref);
			idExp(exp.ixExpr);
		}
		
		else if(_exp instanceof LiteralExpr) {
			/*
			 * Do nothing
			 * LiteralExpr exp = (LiteralExpr) _exp;
			 */	
		}
		
		else if(_exp instanceof NewArrayExpr) {
			NewArrayExpr exp = (NewArrayExpr) _exp;
			
			idType(exp.eltType);
			idExp(exp.sizeExpr);
		}
		
		else if(_exp instanceof NewObjectExpr) {
			NewObjectExpr exp = (NewObjectExpr) _exp; 
			
			idType(exp.classtype);
			
			String objCN = exp.classtype.className.spelling;
			if(!objCN.equals(className) 
					&& ((ClassDecl)idTable.getClass(objCN)).constructor!=null
					&& ((ClassDecl)idTable.getClass(objCN)).constructor.isPrivate)
				reportError(exp.posn,"Constructor declaration is private");
			
			for(Expression e:exp.arguments)
				idExp(e);
		}
		
		else if(_exp instanceof RefExpr) {
			RefExpr exp = (RefExpr) _exp;
			
			idRef(exp.ref);
		}
		
		else if(_exp instanceof UnaryExpr) {
			UnaryExpr exp = (UnaryExpr) _exp;
			
			idExp(exp.expr);
		} 
		
		else {
			reportError(_exp.posn,"");
		}
	}
	
	private void idStmt(Statement stmt) throws IdentificationException{
		if(stmt instanceof BlockStmt) {
			idTable.scopeIn();
			for(Statement stmt_ : ((BlockStmt)stmt).sl) {
				idStmt(stmt_);
			}			
			idTable.scopeOut();
		}
		
		else if(stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			
			if(assign.ref instanceof QualRef) {
				QualRef ref = (QualRef) assign.ref;
				if(ref.id.spelling.equals("length"))
					reportError(assign.posn, "Array 'length' field is read-only");
			}
			
			idRef(assign.ref);
			idExp(assign.val);
		}
		
		else if(stmt instanceof CallStmt) {
			CallStmt call = (CallStmt) stmt;
			idRef(call.methodRef);
			for(Expression exp : call.argList) {
				idExp(exp);
			}
		}
		
		else if(stmt instanceof IfStmt) {
			IfStmt ifstmt = (IfStmt) stmt;
			idExp(ifstmt.cond);
			
			if(ifstmt.thenStmt instanceof VarDeclStmt 
				|| (ifstmt.elseStmt!=null && ifstmt.elseStmt instanceof VarDeclStmt)) {
				reportError(ifstmt.posn,"Variable declaration cannot be sole statement of conditional");
			}
			
			idTable.scopeIn();
			idStmt(ifstmt.thenStmt);
			idTable.scopeOut();
			
			if(ifstmt.elseStmt!=null) {
				idTable.scopeIn();
				idStmt(ifstmt.elseStmt);
				idTable.scopeOut();
			}
		}
		
		else if(stmt instanceof IxAssignStmt) {
			IxAssignStmt IxAssign = (IxAssignStmt) stmt;
			
			idRef(IxAssign.ref);
			idExp(IxAssign.ix);
			idExp(IxAssign.exp);
		} 
		
		else if(stmt instanceof ReturnStmt) {
			if(((ReturnStmt) stmt).returnExpr != null)
				idExp(((ReturnStmt)stmt).returnExpr);
		}
		
		else if(stmt instanceof VarDeclStmt) {
			VarDeclStmt _stmt = (VarDeclStmt) stmt;
			
			idType(_stmt.varDecl.type);
			
			if(!idTable.putId(_stmt.varDecl.name,_stmt.varDecl)) {
				reportError(_stmt.varDecl.posn,"Variable "+_stmt.varDecl.name+" already declared");
			}
			
			varDeclName = _stmt.varDecl.name;
			idExp(_stmt.initExp);
			varDeclName = null;
		}
		
		else if(stmt instanceof WhileStmt) {
			WhileStmt _stmt = (WhileStmt) stmt;
			
			idExp(_stmt.cond);
			
			if(_stmt.body instanceof VarDeclStmt) reportError(_stmt.body.posn,"Variable declaration cannot be sole statement of conditional");
			
			idStmt(_stmt.body);
		}
		
		else {
			//should never reach
			reportError(stmt.posn,"");
		}
	}
	
	private void traverse() throws IdentificationException{
		
		/**
		 * AST Structure for:
		 * 
		 * class System { public static _PrintStream out; } 
		 * class _PrintStream { public void println(int n){}; }
		 * class String { }
		 */
		
		FieldDeclList systemFields = new FieldDeclList();
		FieldDecl out = new FieldDecl(
				false,
				true,
				new ClassType(new Identifier(new Token(
						"_PrintStream",
						TokenKind.id,
						0,
						0
						)),null),
				"out",
				null);
		systemFields.add(out);
		
		ClassDecl system = new ClassDecl(
				"System",
				systemFields,
				new MethodDeclList(),
				null);
		
		MethodDeclList psMethods = new MethodDeclList();
		
		ParameterDeclList params = new ParameterDeclList();
		params.add(new ParameterDecl(
				new BaseType(TypeKind.INT,null),
				"n",
				null));
				
		MethodDecl println = new MethodDecl(
				new FieldDecl(
					false,
					false,
					new BaseType(TypeKind.VOID,null),
					"println",
					null 
				),
				params,
				new StatementList(),
				null);
		psMethods.add(println);
		
		ClassDecl printstream = new ClassDecl(
				"_PrintStream",
				new FieldDeclList(),
				psMethods,
				null);
		
		ClassDecl string = new ClassDecl(
				"String",
				new FieldDeclList(),
				new MethodDeclList(),
				null);
		
		/** LEVEL 1 **/
		idTable.putClass("System", system);
		idTable.putClass("_PrintStream", printstream);
		idTable.putClass("String", string);
		
		for(ClassDecl decl : ast.classDeclList) {
			if(!idTable.putClass(decl.name, decl)) {
				reportError(decl.posn,"Class "+decl.name+" already declared");
			}
		}
				
		/** LEVEL 2 **/
		idTable.putMember("System", "out", out);
		idTable.putMember("_PrintStream", "println", println);
		
		for(ClassDecl cDecl : ast.classDeclList) {
			
			//identify fields
			for(FieldDecl fDecl : cDecl.fieldDeclList) {
				idType(fDecl.type);
				if(!idTable.putMember(cDecl.name, fDecl.name, fDecl)) {
					reportError(fDecl.posn,"Member "+fDecl.name+" already declared");
				}
				if(fDecl.exp!=null) {
					staticMember = true;
					idExp(fDecl.exp);
					staticMember = false;
				}
			}
			
			//identify methods
			for(MethodDecl mDecl : cDecl.methodDeclList) {
				idType(mDecl.type);
				if(!idTable.putMember(cDecl.name, mDecl.name, mDecl)) {
					reportError(mDecl.posn,"Member "+mDecl.name+" already declared");
				}
			}
		}
				
		
		for(ClassDecl cDecl : ast.classDeclList) {
			for(MethodDecl mDecl : cDecl.methodDeclList) {
				
				if(
					!mDecl.isPrivate &&
					mDecl.isStatic &&
					mDecl.type.typeKind == TypeKind.VOID &&
					mDecl.name.equals("main") &&
					mDecl.parameterDeclList.size() == 1 &&
					mDecl.parameterDeclList.get(0).type instanceof ArrayType &&
					((ArrayType)mDecl.parameterDeclList.get(0).type).eltType instanceof ClassType &&
					((ClassType)((ArrayType)mDecl.parameterDeclList.get(0).type).eltType).className.spelling.equals("String"))
				{
					if(hasMain)
						reportError(mDecl.posn,"Cannot have two main methods");
					hasMain = true;
					mDecl.isMain = true;
				}
					
				
				className = cDecl.name;
				
				/******* LEVEL 3 *******/
				
				idTable.scopeIn();
				
				for(ParameterDecl pDecl : mDecl.parameterDeclList) {
					idType(pDecl.type);
					if(!idTable.putId(pDecl.name, pDecl)) {
						reportError(pDecl.posn,"Parameter "+pDecl.name+" already declared");
					}
				}
								
				/******* LEVEL 4+ *******/
				
				idTable.scopeIn();
				
				staticMember = mDecl.isStatic;
				
				for(Statement stmt : mDecl.statementList) {
					idStmt(stmt);
				}
				
				idTable.scopeOut();
				idTable.scopeOut();
			}
		}
		
		if(!hasMain)
			reportError(ast.posn,"No main method detected");
	}
}